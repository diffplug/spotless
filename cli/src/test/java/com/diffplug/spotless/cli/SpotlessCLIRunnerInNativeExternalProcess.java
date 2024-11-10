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
package com.diffplug.spotless.cli;

import java.util.ArrayList;
import java.util.List;

import com.diffplug.spotless.ProcessRunner;
import com.diffplug.spotless.ThrowingEx;

public class SpotlessCLIRunnerInNativeExternalProcess extends SpotlessCLIRunner {

	private static final String SPOTLESS_CLI_NATIVE_IMAGE_SYSPROP = "spotless.cli.nativeImage";

	public SpotlessCLIRunnerInNativeExternalProcess() {
		super();
		if (System.getProperty(SPOTLESS_CLI_NATIVE_IMAGE_SYSPROP) == null) {
			throw new IllegalStateException(SPOTLESS_CLI_NATIVE_IMAGE_SYSPROP + " system property must be set to the path of the native binary");
		}
		System.out.println("SpotlessCLIRunnerInNativeExternalProcess: " + System.getProperty(SPOTLESS_CLI_NATIVE_IMAGE_SYSPROP));
	}

	protected Result executeCommand(List<String> args) {
		try (ProcessRunner runner = new ProcessRunner()) {

			ProcessRunner.Result pResult = ThrowingEx.get(() -> runner.exec(
					workingDir(),
					System.getenv(),
					null,
					processArgs(args)));

			return new Result(pResult.exitCode(), null, pResult.stdOutUtf8(), pResult.stdErrUtf8());
		}
	}

	private List<String> processArgs(List<String> args) {
		List<String> processArgs = new ArrayList<>();
		processArgs.add(System.getProperty(SPOTLESS_CLI_NATIVE_IMAGE_SYSPROP));
		//		processArgs.add(SpotlessCLI.class.getProtectionDomain().getCodeSource().getLocation().getPath());

		processArgs.addAll(args);
		return processArgs;
	}

	private String currentJavaExecutable() {
		return ProcessHandle.current().info().command().orElse("java");
	}

}
