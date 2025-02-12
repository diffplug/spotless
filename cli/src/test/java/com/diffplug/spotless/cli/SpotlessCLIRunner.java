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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.diffplug.spotless.cli.steps.SpotlessCLIFormatterStep;

import picocli.CommandLine;

public abstract class SpotlessCLIRunner {

	private File workingDir = new File(".");

	private final List<String> args = new ArrayList<>();

	public static SpotlessCLIRunner create() {
		return new SpotlessCLIRunnerInSameThread();
	}

	public static SpotlessCLIRunner createExternalProcess() {
		return new SpotlessCLIRunnerInExternalJavaProcess();
	}

	public static SpotlessCLIRunner createNative() {
		return new SpotlessCLIRunnerInNativeExternalProcess();
	}

	public SpotlessCLIRunner withWorkingDir(@NotNull File workingDir) {
		this.workingDir = Objects.requireNonNull(workingDir);
		return this;
	}

	protected File workingDir() {
		return workingDir;
	}

	public SpotlessCLIRunner withOption(@NotNull String option) {
		args.add(Objects.requireNonNull(option));
		return this;
	}

	public SpotlessCLIRunner withOption(@NotNull String option, @NotNull String value) {
		args.add(String.format("%s=%s", Objects.requireNonNull(option), Objects.requireNonNull(value)));
		return this;
	}

	public SpotlessCLIRunner withTargets(String... targets) {
		for (String target : targets) {
			withOption("--target", target);
		}
		return this;
	}

	public SpotlessCLIRunner withStep(@NotNull String stepName) {
		args.add(Objects.requireNonNull(stepName));
		return this;
	}

	public SpotlessCLIRunner withStep(@NotNull Class<? extends SpotlessCLIFormatterStep> stepClass) {
		String stepName = determineStepName(stepClass);
		return withStep(stepName);
	}

	private String determineStepName(Class<? extends SpotlessCLIFormatterStep> stepClass) {
		CommandLine.Command annotation = stepClass.getAnnotation(CommandLine.Command.class);
		if (annotation == null) {
			throw new IllegalArgumentException("Step class must be annotated with @CommandLine.Command");
		}
		return annotation.name();
	}

	public Result run() {
		Result result = executeCommand(args);
		if (result.executionException() != null) {
			throwRuntimeException("Error while executing Spotless CLI command", result);
		}
		if (result.exitCode == null || result.exitCode != 0) {
			throwRuntimeException("Spotless CLI command failed with exit code " + result.exitCode, result);
		}
		return result;
	}

	public Result runAndFail() {
		Result result = executeCommand(args);
		if (result.executionException() != null) {
			throwRuntimeException("Error while executing Spotless CLI command", result);
		}
		if (result.exitCode == null || result.exitCode == 0) {
			throwRuntimeException("Spotless CLI command should have failed but exited with code " + result.exitCode, result);
		}
		return result;
	}

	private void throwRuntimeException(String message, Result result) {
		StringBuilder sb = new StringBuilder(message)
				.append("\nExit code: ").append(result.exitCode()).append("\n")
				.append("\n--- Standard output: ---\n").append(result.stdOut()).append("\n------------------------\n")
				.append("\n--- Standard error: ---\n").append(result.stdErr()).append("\n------------------------\n");

		if (result.executionException() != null) {
			throw new RuntimeException(sb.toString(), result.executionException());
		}
		throw new RuntimeException(sb.toString());
	}

	protected abstract Result executeCommand(List<String> args);

	public static class Result {

		private final Integer exitCode;
		private final String stdOut;
		private final String stdErr;
		private final Exception executionException;

		protected Result(@Nullable Integer exitCode, @Nullable Exception executionException, @NotNull String stdOut, @NotNull String stdErr) {
			this.exitCode = exitCode;
			this.executionException = executionException;
			this.stdOut = Objects.requireNonNull(stdOut);
			this.stdErr = Objects.requireNonNull(stdErr);
		}

		public Integer exitCode() {
			return exitCode;
		}

		public String stdOut() {
			return stdOut;
		}

		public String stdErr() {
			return stdErr;
		}

		public Exception executionException() {
			return executionException;
		}
	}
}
