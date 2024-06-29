/*
 * Copyright 2024 DiffPlug
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

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner.LongRunningProcess;
import com.diffplug.spotless.ThrowingEx;

abstract class NpmServerBasedFormatterStep implements FormatterStep {

	private final String name;
	protected final NpmFormatterStepLocations locations;

	protected final NodeServerLayout nodeServerLayout;
	private final NodeServeApp nodeServeApp;

	private NpmServerProcessInfo serverProcessInfo;

	public NpmServerBasedFormatterStep(@Nonnull String name,
			@Nonnull NpmConfig npmConfig, @Nonnull NpmFormatterStepLocations locations) {
		this.name = Objects.requireNonNull(name);
		this.locations = Objects.requireNonNull(locations);
		this.nodeServerLayout = new NodeServerLayout(Objects.requireNonNull(locations).buildDir(), Objects.requireNonNull(npmConfig).getPackageJsonContent());
		this.nodeServeApp = new NodeServeApp(nodeServerLayout, npmConfig, locations);
	}

	// FormatterStep

	@Override
	public String getName() {
		return name;
	}

	@Nullable
	@Override
	public String format(String rawUnix, File file) throws Exception {
		if (this.serverProcessInfo == null) {
			assertNodeServerDirReady();
			this.serverProcessInfo = npmRunServer();
		}
		return formatWithServer(serverProcessInfo, rawUnix, file);
	}

	@Override
	public void close() throws Exception {
		if (this.serverProcessInfo != null) {
			this.serverProcessInfo.close();
		}
		this.serverProcessInfo = null;
	}

	// Equals and HashCode

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		NpmServerBasedFormatterStep that = (NpmServerBasedFormatterStep) o;
		return Objects.equals(name, that.name) && Objects.equals(nodeServerLayout, that.nodeServerLayout) && Objects.equals(nodeServeApp, that.nodeServeApp) && Objects.equals(serverProcessInfo, that.serverProcessInfo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, nodeServerLayout, nodeServeApp, serverProcessInfo);
	}

	// Serializable contract

	// override serialize output
	private void writeObject(ObjectOutputStream out) throws IOException {
		// TODO (simschla, 27.06.2024): Implement serialization
		System.out.println("TODO: Implement serialization - writeObject " + this);
		//		out.writeObject(state());
	}

	// override serialize input
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		// TODO (simschla, 27.06.2024): Implement serialization
		System.out.println("TODO: Implement serialization - readObject " + this);
		//		state = (T) Objects.requireNonNull(in.readObject());
	}

	// override serialize input
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException {
		throw new UnsupportedOperationException();
	}

	// NpmServerBasedFormatterStep

	protected void assertNodeServerDirReady() throws IOException {
		if (needsPrepareNodeServerLayout()) {
			// reinstall if missing
			prepareNodeServerLayout();
		}
		if (needsPrepareNodeServer()) {
			// run npm install if node_modules is missing
			prepareNodeServer();
		}
	}

	protected boolean needsPrepareNodeServerLayout() {
		return nodeServeApp.needsPrepareNodeAppLayout();
	}

	protected void prepareNodeServerLayout() throws IOException {
		nodeServeApp.prepareNodeAppLayout();
		doPrepareNodeServerLayout(nodeServerLayout);
	}

	abstract protected void doPrepareNodeServerLayout(NodeServerLayout layout) throws IOException;

	protected boolean needsPrepareNodeServer() {
		return nodeServeApp.needsNpmInstall();
	}

	protected void prepareNodeServer() throws IOException {
		nodeServeApp.npmInstall();
	}

	protected NpmServerProcessInfo npmRunServer() throws ServerStartException, IOException {
		assertNodeServerDirReady();
		LongRunningProcess server = null;
		try {
			// The npm process will output the randomly selected port of the http server process to 'server.port' file
			// so in order to be safe, remove such a file if it exists before starting.
			final File serverPortFile = new File(this.nodeServerLayout.nodeModulesDir(), "server.port");
			NpmResourceHelper.deleteFileIfExists(serverPortFile);
			// start the http server in node
			server = nodeServeApp.startNpmServeProcess();

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
			return new NpmServerProcessInfo(server, serverPort, serverPortFile);
		} catch (IOException | TimeoutException e) {
			throw new ServerStartException("Starting server failed." + (server != null ? "\n\nProcess result:\n" + ThrowingEx.get(server::result) : ""), e);
		}
	}

	protected abstract String formatWithServer(NpmServerProcessInfo serverProcessInfo, String rawUnix, File file);
}
