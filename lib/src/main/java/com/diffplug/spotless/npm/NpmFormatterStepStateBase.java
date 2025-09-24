/*
 * Copyright 2016-2025 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.npm;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.ProcessRunner;
import com.diffplug.spotless.ProcessRunner.LongRunningProcess;
import com.diffplug.spotless.ThrowingEx;

abstract class NpmFormatterStepStateBase implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(NpmFormatterStepStateBase.class);

	private static final TimedLogger timedLogger = TimedLogger.forLogger(logger);

	@Serial
	private static final long serialVersionUID = 1460749955865959948L;

	private final String stepName;
	private final NpmConfig npmConfig;

	public final NpmFormatterStepLocations locations;

	protected NpmFormatterStepStateBase(String stepName, NpmConfig npmConfig, NpmFormatterStepLocations locations) throws IOException {
		this.stepName = requireNonNull(stepName);
		this.npmConfig = requireNonNull(npmConfig);
		this.locations = locations;
	}

	public Runtime toRuntime() {
		return new Runtime(this);
	}

	protected void prepareNodeServerLayout(NodeServerLayout layout) throws IOException {

	}

	public static class Runtime {
		private final NpmFormatterStepStateBase parent;
		private final NodeServerLayout nodeServerLayout;
		private final NodeServeApp nodeServeApp;

		Runtime(NpmFormatterStepStateBase parent) {
			this.parent = parent;
			this.nodeServerLayout = new NodeServerLayout(parent.locations.buildDir(), parent.npmConfig.getPackageJsonContent(), parent.npmConfig.getServeScriptContent());
			this.nodeServeApp = new NodeServeApp(nodeServerLayout, parent.npmConfig, parent.locations);
		}

		public NodeServerLayout nodeServerLayout() {
			return nodeServerLayout;
		}

		protected void prepareNodeServerLayout() throws IOException {
			nodeServeApp.prepareNodeAppLayout();
			parent.prepareNodeServerLayout(nodeServerLayout);
		}

		protected void prepareNodeServer() throws IOException {
			nodeServeApp.npmInstall();
		}

		protected void assertNodeServerDirReady() throws IOException {
			ExclusiveFolderAccess.forFolder(nodeServerLayout.nodeModulesDir())
					.runExclusively(() -> {
						if (needsPrepareNodeServerLayout()) {
							// reinstall if missing
							prepareNodeServerLayout();
						}
						if (needsPrepareNodeServer()) {
							// run npm install if node_modules is missing
							prepareNodeServer();
						}
					});
		}

		protected boolean needsPrepareNodeServer() {
			return nodeServeApp.needsNpmInstall();
		}

		protected boolean needsPrepareNodeServerLayout() {
			return nodeServeApp.needsPrepareNodeAppLayout();
		}

		protected ServerProcessInfo npmRunServer() throws ServerStartException, IOException {
			assertNodeServerDirReady();
			LongRunningProcess server = null;
			try {
				final UUID nodeServerInstanceId = UUID.randomUUID();
				// The npm process will output the randomly selected port of the http server process to 'server-<id>.port' file
				// so in order to be safe, remove such a file if it exists before starting.
				final File serverPortFile = new File(this.nodeServerLayout.nodeModulesDir(), "server-%s.port".formatted(nodeServerInstanceId));
				NpmResourceHelper.deleteFileIfExists(serverPortFile);
				// start the http server in node
				server = nodeServeApp.startNpmServeProcess(nodeServerInstanceId);

				// await the readiness of the http server - wait for at most 60 seconds
				try {
					NpmResourceHelper.awaitReadableFile(serverPortFile, Duration.ofSeconds(60));
				} catch (TimeoutException timeoutException) {
					// forcibly end the server process
					try {
						if (server.isAlive()) {
							server.destroyForcibly();
							ProcessRunner.Result result = server.result();
							logger.info("Launching npm server process failed. Process result:\n{}", result);
						}
					} catch (Throwable t) {
						ProcessRunner.Result result = ThrowingEx.get(server::result);
						logger.debug("Unable to forcibly end the server process. Process result:\n{}", result, t);
					}
					throw timeoutException;
				}
				// read the server.port file for resulting port and remember the port for later formatting calls
				String serverPort = NpmResourceHelper.readUtf8StringFromFile(serverPortFile).trim();
				return new ServerProcessInfo(server, serverPort, serverPortFile);
			} catch (IOException | TimeoutException e) {
				throw new ServerStartException("Starting server failed." + (server != null ? "\n\nProcess result:\n" + ThrowingEx.get(server::result) : ""), e);
			}
		}
	}

	protected static String replaceDevDependencies(String template, Map<String, String> devDependencies) {
		StringBuilder builder = new StringBuilder();
		Iterator<Map.Entry<String, String>> entryIter = devDependencies.entrySet().iterator();
		while (entryIter.hasNext()) {
			Map.Entry<String, String> entry = entryIter.next();
			builder.append("\t\t\"");
			builder.append(entry.getKey());
			builder.append("\": \"");
			builder.append(entry.getValue());
			builder.append("\"");
			if (entryIter.hasNext()) {
				builder.append(",\n");
			}
		}
		return replacePlaceholders(template, Collections.singletonMap("devDependencies", builder.toString()));
	}

	private static String replacePlaceholders(String template, Map<String, String> replacements) {
		String result = template;
		for (Entry<String, String> entry : replacements.entrySet()) {
			result = result.replaceAll("\\Q${" + entry.getKey() + "}\\E", entry.getValue());
		}
		return result;
	}

	public abstract FormatterFunc createFormatterFunc();

	protected static class ServerProcessInfo implements AutoCloseable {
		private final Process server;
		private final String serverPort;
		private final File serverPortFile;

		public ServerProcessInfo(Process server, String serverPort, File serverPortFile) {
			this.server = server;
			this.serverPort = serverPort;
			this.serverPortFile = serverPortFile;
		}

		public String getBaseUrl() {
			return "http://127.0.0.1:" + this.serverPort;
		}

		@Override
		public void close() throws Exception {
			try {
				logger.trace("Closing npm server in directory <{}> and port <{}>",
						serverPortFile.getParent(), serverPort);

				if (server.isAlive()) {
					boolean ended = server.waitFor(5, TimeUnit.SECONDS);
					if (!ended) {
						logger.info("Force-Closing npm server in directory <{}> and port <{}>", serverPortFile.getParent(), serverPort);
						server.destroyForcibly().waitFor();
						logger.trace("Force-Closing npm server in directory <{}> and port <{}> -- Finished", serverPortFile.getParent(), serverPort);
					}
				}
			} finally {
				NpmResourceHelper.deleteFileIfExists(serverPortFile);
			}
		}
	}

	protected static class ServerStartException extends RuntimeException {
		@Serial
		private static final long serialVersionUID = -8803977379866483002L;

		public ServerStartException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
