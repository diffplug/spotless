/*
 * Copyright 2016-2023 DiffPlug
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
import java.io.Serializable;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

abstract class NpmFormatterStepStateBase implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(NpmFormatterStepStateBase.class);

	private static final long serialVersionUID = 1460749955865959948L;

	@SuppressWarnings("unused")
	private final FileSignature packageJsonSignature;

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	public final transient File nodeModulesDir;

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	private final transient File npmExecutable;

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	public final transient File projectDir;

	private final NpmConfig npmConfig;

	private final String stepName;

	protected NpmFormatterStepStateBase(String stepName, NpmConfig npmConfig, File projectDir, File buildDir, File npm) throws IOException {
		this.stepName = requireNonNull(stepName);
		this.npmConfig = requireNonNull(npmConfig);
		this.projectDir = requireNonNull(projectDir);
		this.npmExecutable = npm;

		NodeServerLayout layout = prepareNodeServer(buildDir);
		this.nodeModulesDir = layout.nodeModulesDir();
		this.packageJsonSignature = FileSignature.signAsList(layout.packageJsonFile());
	}

	private NodeServerLayout prepareNodeServer(File buildDir) throws IOException {
		NodeServerLayout layout = new NodeServerLayout(buildDir, stepName);
		NpmResourceHelper.assertDirectoryExists(layout.nodeModulesDir());
		NpmResourceHelper.writeUtf8StringToFile(layout.packageJsonFile(),
				this.npmConfig.getPackageJsonContent());
		NpmResourceHelper
				.writeUtf8StringToFile(layout.serveJsFile(), this.npmConfig.getServeScriptContent());
		if (this.npmConfig.getNpmrcContent() != null) {
			NpmResourceHelper.writeUtf8StringToFile(layout.npmrcFile(), this.npmConfig.getNpmrcContent());
		} else {
			NpmResourceHelper.deleteFileIfExists(layout.npmrcFile());
		}
		FormattedPrinter.SYSOUT.print("running npm install");
		runNpmInstall(layout.nodeModulesDir());
		FormattedPrinter.SYSOUT.print("npm install finished");
		return layout;
	}

	private void runNpmInstall(File npmProjectDir) throws IOException {
		new NpmProcess(npmProjectDir, this.npmExecutable).install();
	}

	protected ServerProcessInfo npmRunServer() throws ServerStartException, IOException {
		if (!this.nodeModulesDir.exists()) {
			prepareNodeServer(NodeServerLayout.getBuildDirFromNodeModulesDir(this.nodeModulesDir));
		}

		try {
			// The npm process will output the randomly selected port of the http server process to 'server.port' file
			// so in order to be safe, remove such a file if it exists before starting.
			final File serverPortFile = new File(this.nodeModulesDir, "server.port");
			NpmResourceHelper.deleteFileIfExists(serverPortFile);
			// start the http server in node
			Process server = new NpmProcess(this.nodeModulesDir, this.npmExecutable).start();

			// await the readiness of the http server - wait for at most 60 seconds
			try {
				NpmResourceHelper.awaitReadableFile(serverPortFile, Duration.ofSeconds(60));
			} catch (TimeoutException timeoutException) {
				// forcibly end the server process
				try {
					if (server.isAlive()) {
						server.destroyForcibly();
						server.waitFor();
					}
				} catch (Throwable t) {
					// ignore
				}
				throw timeoutException;
			}
			// read the server.port file for resulting port and remember the port for later formatting calls
			String serverPort = NpmResourceHelper.readUtf8StringFromFile(serverPortFile).trim();
			return new ServerProcessInfo(server, serverPort, serverPortFile);
		} catch (IOException | TimeoutException e) {
			throw new ServerStartException(e);
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
		private static final long serialVersionUID = -8803977379866483002L;

		public ServerStartException(Throwable cause) {
			super(cause);
		}
	}
}
