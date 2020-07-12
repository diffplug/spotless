/*
 * Copyright 2016-2020 DiffPlug
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class NpmProcess {

	private final File workingDir;

	private final File npmExecutable;

	NpmProcess(File workingDir, File npmExecutable) {
		this.workingDir = workingDir;
		this.npmExecutable = npmExecutable;
	}

	void install() {
		npmAwait("install", "--no-audit", "--no-package-lock");
	}

	Process start() {
		// adding --scripts-prepend-node-path=true due to https://github.com/diffplug/spotless/issues/619#issuecomment-648018679
		return npm("start", "--scripts-prepend-node-path=true");
	}

	private void npmAwait(String... args) {
		final Process npmProcess = npm(args);

		try {
			if (npmProcess.waitFor() != 0) {
				throw new NpmProcessException("Running npm command '" + commandLine(args) + "' failed with exit code: " + npmProcess.exitValue());
			}
		} catch (InterruptedException e) {
			throw new NpmProcessException("Running npm command '" + commandLine(args) + "' was interrupted.", e);
		}
	}

	private Process npm(String... args) {
		List<String> processCommand = processCommand(args);
		try {
			return new ProcessBuilder()
					.inheritIO()
					.directory(this.workingDir)
					.command(processCommand)
					.start();
		} catch (IOException e) {
			throw new NpmProcessException("Failed to launch npm command '" + commandLine(args) + "'.", e);
		}
	}

	private List<String> processCommand(String... args) {
		List<String> command = new ArrayList<>(args.length + 1);
		command.add(this.npmExecutable.getAbsolutePath());
		command.addAll(Arrays.asList(args));
		return command;
	}

	private String commandLine(String... args) {
		return "npm " + Arrays.stream(args).collect(Collectors.joining(" "));
	}

	static class NpmProcessException extends RuntimeException {
		private static final long serialVersionUID = 6424331316676759525L;

		public NpmProcessException(String message) {
			super(message);
		}

		public NpmProcessException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
