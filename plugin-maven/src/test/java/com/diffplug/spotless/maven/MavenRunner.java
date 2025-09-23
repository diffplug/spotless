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
package com.diffplug.spotless.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.ProcessRunner;

/**
 * Harness for running a Maven build, same idea as the
 * <a href="https://docs.gradle.org/current/javadoc/org/gradle/testkit/runner/GradleRunner.html">GradleRunner from the Gradle testkit</a>.
 */
public class MavenRunner {
	public static MavenRunner create() {
		return new MavenRunner();
	}

	private MavenRunner() {}

	private File projectDir;
	private String[] args;
	private Map<String, String> environment = new HashMap<>();
	private Map<String, String> systemProperties = new HashMap<>();
	private ProcessRunner runner;

	public MavenRunner withProjectDir(File projectDir) {
		this.projectDir = Objects.requireNonNull(projectDir);
		return this;
	}

	public MavenRunner withArguments(String... args) {
		this.args = Objects.requireNonNull(args);
		return this;
	}

	public MavenRunner withEnvironment(String key, String value) {
		environment.put(key, value);
		return this;
	}

	public MavenRunner withRunner(ProcessRunner runner) {
		this.runner = runner;
		return this;
	}

	public MavenRunner withRemoteDebug(int port) {
		String address = (Jvm.version() < 9 ? "" : "*:") + port;
		environment.put("MAVEN_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + address);
		return this;
	}

	public MavenRunner withSystemProperty(String key, String value) {
		systemProperties.put(key, value);
		return this;
	}

	private Map<String, String> calculateEnvironment() {
		Map<String, String> env = new HashMap<>(environment);
		if (!systemProperties.isEmpty()) {
			// add system properties as environment variables as MAVEN_OPTS or append if already there
			String sysProps = systemProperties.entrySet().stream()
					.map(entry -> String.format("-D%s=%s", entry.getKey(), entry.getValue()))
					.collect(Collectors.joining(" "));
			String mavenOpts = Stream.of(env.getOrDefault("MAVEN_OPTS", ""), sysProps)
					.collect(Collectors.joining(" "));
			env.put("MAVEN_OPTS", mavenOpts.trim());
		}
		return env;
	}

	private ProcessRunner.Result run() throws IOException, InterruptedException {
		Objects.requireNonNull(projectDir, "Need to call withProjectDir() first");
		Objects.requireNonNull(args, "Need to call withArguments() first");
		// run Maven with the given args in the given directory
		String argsString = "-e " + String.join(" ", Arrays.asList(args));
		return runner.shellWinUnix(projectDir, calculateEnvironment(), "mvnw " + argsString, "./mvnw " + argsString);
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
