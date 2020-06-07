/*
 * Copyright 2016 DiffPlug
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

import java.io.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

import com.diffplug.spotless.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

abstract class NpmFormatterStepStateBase implements Serializable {

	private static final long serialVersionUID = 1460749955865959948L;

	@SuppressWarnings("unused")
	private final FileSignature nodeModulesSignature;

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	public final transient File nodeModulesDir;

	private final transient File npmExecutable;

	private final NpmConfig npmConfig;

	private final String stepName;

	protected NpmFormatterStepStateBase(String stepName, NpmConfig npmConfig, File buildDir, @Nullable File npm) throws IOException {
		this.stepName = requireNonNull(stepName);
		this.npmConfig = requireNonNull(npmConfig);
		this.npmExecutable = resolveNpm(npm);

		this.nodeModulesDir = prepareNodeServer(buildDir);
		this.nodeModulesSignature = FileSignature.signAsList(this.nodeModulesDir);
	}

	private File prepareNodeServer(File buildDir) throws IOException {
		File targetDir = new File(buildDir, "spotless-node-modules-" + stepName);
		SimpleResourceHelper.assertDirectoryExists(targetDir);
		SimpleResourceHelper.writeUtf8StringToFile(targetDir, "package.json", this.npmConfig.getPackageJsonContent());
		SimpleResourceHelper.writeUtf8StringToFile(targetDir, "serve.js", this.npmConfig.getServeScriptContent());
		runNpmInstall(targetDir);
		return targetDir;
	}

	private void runNpmInstall(File npmProjectDir) throws IOException {
		Process npmInstall = new ProcessBuilder()
				.inheritIO()
				.directory(npmProjectDir)
				.command(this.npmExecutable.getAbsolutePath(), "install", "--no-audit", "--no-package-lock")
				.start();
		try {
			if (npmInstall.waitFor() != 0) {
				throw new IOException("Creating npm modules failed with exit code: " + npmInstall.exitValue());
			}
		} catch (InterruptedException e) {
			throw new IOException("Running npm install was interrupted.", e);
		}
	}

	protected ServerProcessInfo npmRunServer() throws ServerStartException {
		try {
			// The npm process will output the randomly selected port of the http server process to 'server.port' file
			// so in order to be safe, remove such a file if it exists before starting.
			final File serverPortFile = new File(this.nodeModulesDir, "server.port");
			SimpleResourceHelper.deleteFileIfExists(serverPortFile);
			// start the http server in node
			Process server = new ProcessBuilder()
					.inheritIO()
					.directory(this.nodeModulesDir)
					.command(this.npmExecutable.getAbsolutePath(), "start")
					.start();

			// await the readiness of the http server
			final long startedAt = System.currentTimeMillis();
			while (!serverPortFile.exists() || !serverPortFile.canRead()) {
				// wait for at most 60 seconds - if it is not ready by then, something is terribly wrong
				if ((System.currentTimeMillis() - startedAt) > (60 * 1000L)) {
					// forcibly end the server process
					try {
						server.destroyForcibly();
					} catch (Throwable t) {
						// ignore
					}
					throw new TimeoutException("The server did not startup in the requested time frame of 60 seconds.");
				}
			}
			// read the server.port file for resulting port and remember the port for later formatting calls
			String serverPort = SimpleResourceHelper.readUtf8StringFromFile(serverPortFile).trim();
			return new ServerProcessInfo(server, serverPort, serverPortFile);
		} catch (IOException | TimeoutException e) {
			throw new ServerStartException(e);
		}
	}

	private static File resolveNpm(@Nullable File npm) {
		return Optional.ofNullable(npm)
				.orElseGet(() -> NpmExecutableResolver.tryFind()
						.orElseThrow(() -> new IllegalStateException("Can't automatically determine npm executable and none was specifically supplied!\n\n" + NpmExecutableResolver.explainMessage())));
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
			SimpleResourceHelper.deleteFileIfExists(serverPortFile);
			if (this.server.isAlive()) {
				this.server.destroy();
			}
		}
	}

	protected static class ServerStartException extends RuntimeException {
		public ServerStartException(Throwable cause) {
			super(cause);
		}
	}
}
