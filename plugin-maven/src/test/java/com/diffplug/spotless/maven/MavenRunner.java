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
package com.diffplug.spotless.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import com.diffplug.common.base.Throwables;
import com.diffplug.common.io.ByteStreams;

/**
 * Harness for running a maven build, same idea as the
 * [GradleRunner from the gradle testkit](https://docs.gradle.org/current/javadoc/org/gradle/testkit/runner/GradleRunner.html).
 */
public class MavenRunner {
	public static MavenRunner create() {
		return new MavenRunner();
	}

	private MavenRunner() {}

	private File projectDir;
	private String[] args;

	public MavenRunner withProjectDir(File projectDir) {
		this.projectDir = Objects.requireNonNull(projectDir);
		return this;
	}

	public MavenRunner withArguments(String... args) {
		this.args = Objects.requireNonNull(args);
		return this;
	}

	private Result run() throws IOException, InterruptedException {
		Objects.requireNonNull(projectDir, "Need to call withProjectDir() first");
		Objects.requireNonNull(args, "Need to call withArguments() first");
		// run maven with the given args in the given directory
		//   -e to display execution errors at the console
		//   -U to force update of snapshots
		List<String> cmds = getPlatformCmds("-X -U  " + Arrays.stream(args).collect(Collectors.joining(" ")));
		ProcessBuilder builder = new ProcessBuilder(cmds);
		builder.directory(projectDir);
		Process process = builder.start();
		// slurp and return the stdout, stderr, and exitValue
		Slurper output = new Slurper(process.getInputStream());
		Slurper error = new Slurper(process.getErrorStream());
		int exitValue = process.waitFor();
		output.join();
		error.join();
		return new Result(exitValue, output.result(), error.result());
	}

	/** Runs the command and asserts that exit code is 0. */
	public Result runNoError() throws IOException, InterruptedException {
		Result result = run();
		assertThat(result.exitValue()).as("Run without error %s", result).isEqualTo(0);
		return result;
	}

	/** Runs the command and asserts that exit code is not 0. */
	public Result runHasError() throws IOException, InterruptedException {
		Result result = run();
		assertThat(result.exitValue()).as("Run with error %s", result).isNotEqualTo(0);
		return result;
	}

	public static class Result {
		private final int exitValue;
		private final String output;
		private final String error;

		public Result(int exitValue, String output, String error) {
			super();
			this.exitValue = exitValue;
			this.output = Objects.requireNonNull(output);
			this.error = Objects.requireNonNull(error);
		}

		public int exitValue() {
			return exitValue;
		}

		public String output() {
			return output;
		}

		public String error() {
			return error;
		}

		@Override
		public String toString() {
			return "Result{" +
					"exitValue=" + exitValue +
					", output='" + output + '\'' +
					", error='" + error + '\'' +
					'}';
		}
	}

	/** Prepends any arguments necessary to run a console command. */
	private static List<String> getPlatformCmds(String cmd) {
		if (isWin()) {
			return Arrays.asList("cmd", "/c", "mvnw " + cmd);
		} else {
			return Arrays.asList("/bin/sh", "-c", "./mvnw " + cmd);
		}
	}

	private static boolean isWin() {
		String os_name = System.getProperty("os.name").toLowerCase(Locale.getDefault());
		return os_name.contains("win");
	}

	private static class Slurper extends Thread {
		private final InputStream input;
		private volatile String result;

		Slurper(InputStream input) {
			this.input = Objects.requireNonNull(input);
			start();
		}

		@Override
		public void run() {
			try {
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				ByteStreams.copy(input, output);
				result = output.toString(Charset.defaultCharset().name());
			} catch (Exception e) {
				result = Throwables.getStackTraceAsString(e);
			}
		}

		public String result() {
			return result;
		}
	}
}
