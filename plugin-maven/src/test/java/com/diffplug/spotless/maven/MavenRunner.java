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
package com.diffplug.spotless.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.ProcessRunner;

/**
 * Harness for running a maven build, same idea as the
 * <a href="https://docs.gradle.org/current/javadoc/org/gradle/testkit/runner/GradleRunner.html">GradleRunner from the gradle testkit</a>.
 */
public class MavenRunner {
	public static MavenRunner create() {
		return new MavenRunner();
	}

	private MavenRunner() {}

	private File projectDir;
	private String[] args;
	private Map<String, String> environment = new HashMap<>();
	private ProcessRunner runner;

	public MavenRunner withProjectDir(File projectDir) {
		this.projectDir = Objects.requireNonNull(projectDir);
		return this;
	}

	public MavenRunner withArguments(String... args) {
		this.args = Objects.requireNonNull(args);
		return this;
	}

	public MavenRunner withRunner(ProcessRunner runner) {
		this.runner = runner;
		return this;
	}

	public MavenRunner withRemoteDebug(int port) {
		var address = (Jvm.version() < 9 ? "" : "*:") + port;
		environment.put("MAVEN_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + address);
		return this;
	}

	private ProcessRunner.Result run() throws IOException, InterruptedException {
		Objects.requireNonNull(projectDir, "Need to call withProjectDir() first");
		Objects.requireNonNull(args, "Need to call withArguments() first");
		// run maven with the given args in the given directory
		var argsString = "-e " + String.join(" ", Arrays.asList(args));
		return runner.shellWinUnix(projectDir, environment, "mvnw " + argsString, "./mvnw " + argsString);
	}

	/** Runs the command and asserts that exit code is 0. */
	public ProcessRunner.Result runNoError() throws IOException, InterruptedException {
		ProcessRunner.Result result = run();
		assertThat(result.exitCode()).as("Run without error %s", result).isEqualTo(0);
		return result;
	}

	/** Runs the command and asserts that exit code is not 0. */
	public ProcessRunner.Result runHasError() throws IOException, InterruptedException {
		ProcessRunner.Result result = run();
		assertThat(result.exitCode()).as("Run with error %s", result).isNotEqualTo(0);
		return result;
	}
}
