/*
 * Copyright 2025 DiffPlug
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ProcessRunner;

class SpotlessInstallPrePushHookMojoTest extends MavenIntegrationHarness {

	@Test
	public void should_create_pre_hook_file_when_hook_file_does_not_exists() throws Exception {
		// given
		setFile(".git/config").toContent("");
		setFile("license.txt").toResource("git_pre_hook/TestLicense.txt");
		writePomWithJavaLicenseHeaderStep();

		// when
		final var output = runMaven("spotless:install-git-pre-push-hook");

		// then
		assertThat(output).contains("Installing git pre-push hook");
		assertThat(output).contains("Git pre-push hook not found, creating it");
		assertThat(output).contains("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push"));

		final var hookContent = getHookContent("git_pre_hook/pre-push.created-tpl");
		assertFile(".git/hooks/pre-push").hasContent(hookContent);
	}

	@Test
	public void should_append_to_existing_pre_hook_file_when_hook_file_exists() throws Exception {
		// given
		setFile(".git/config").toContent("");
		setFile("license.txt").toResource("git_pre_hook/TestLicense.txt");
		setFile(".git/hooks/pre-push").toResource("git_pre_hook/pre-push.existing");

		writePomWithJavaLicenseHeaderStep();

		// when
		final var output = runMaven("spotless:install-git-pre-push-hook");

		// then
		assertThat(output).contains("Installing git pre-push hook");
		assertThat(output).contains("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push"));

		final var hookContent = getHookContent("git_pre_hook/pre-push.existing-installed-end-tpl");
		assertFile(".git/hooks/pre-push").hasContent(hookContent);
	}

	@Test
	public void should_execute_pre_push_script() throws Exception {
		// given
		setFile("license.txt").toResource("git_pre_hook/TestLicense.txt");
		setFile(".git/config").toContent("");
		setFile("src/main/java/com.github.youribonnaffe.gradle.format/Java8Test.java").toResource("git_pre_hook/MissingLicense.test");
		writePomWithJavaLicenseHeaderStep();

		// when
		// install pre-hook
		final var output = runMaven("spotless:install-git-pre-push-hook");

		final var result = executeHookScript(".git/hooks/pre-push");

		// then
		assertThat(output).contains("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push"));

		assertThat(result.stdErrUtf8()).startsWith("spotless found problems, running spotless:apply; commit the result and re-push");
		assertThat(result.exitCode()).isEqualTo(1);

		final var fileContent = read("src/main/java/com.github.youribonnaffe.gradle.format/Java8Test.java");
		assertThat(fileContent).startsWith("this is a test license!\n");
	}

	private void writePomWithJavaLicenseHeaderStep() throws IOException {
		writePomWithJavaSteps(
				"<licenseHeader>",
				"  <file>${basedir}/license.txt</file>",
				"</licenseHeader>");
	}

	private String getHookContent(String resourceFile) {
		final var executorFile = executorWrapperFile();
		final var executorPath = executorFile.exists() ? executorFile.getAbsolutePath().replace("\\", "/") : "mvn";
		return getTestResource(resourceFile)
				.replace("${executor}", executorPath)
				.replace("${checkCommand}", "spotless:check")
				.replace("${applyCommand}", "spotless:apply");
	}

	private boolean isWindows() {
		return System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win");
	}

	private File executorWrapperFile() {
		if (isWindows()) {
			final var bat = newFile("mvnw.bat");
			if (bat.exists()) {
				return bat;
			}

			return newFile("mvnw.cmd");
		}

		return newFile("mvnw");
	}

	private String runMaven(String command) throws Exception {
		return mavenRunner()
				.withArguments(command)
				.withEnvironment("JAVA_HOME", System.getProperty("java.home"))
				.runNoError()
				.stdOutUtf8();
	}

	private ProcessRunner.Result executeHookScript(String hookFile) throws Exception {
		try (final var runner = new ProcessRunner()) {
			String executor = "sh";
			if (isWindows()) {
				final var bashPath = findGitBashExecutable();
				if (bashPath.isEmpty()) {
					throw new RuntimeException("Could not find git bash executable");
				}

				executor = bashPath.orElseThrow();
			}

			return runner.exec(rootFolder(), Map.of("JAVA_HOME", System.getProperty("java.home")), null, List.of(executor, hookFile));
		}
	}

	private Optional<String> findGitBashExecutable() {
		// 1. Check environment variable
		final var envPath = System.getenv("GIT_BASH");
		if (envPath != null && new File(envPath).exists()) {
			return Optional.of(envPath);
		}

		// 2. Check common install paths
		final var commonPaths = List.of(
				"C:\\Program Files\\Git\\bin\\bash.exe",
				"C:\\Program Files (x86)\\Git\\bin\\bash.exe",
				"C:\\Program Files\\Git\\usr\\bin\\bash.exe");

		for (var path : commonPaths) {
			if (new File(path).exists()) {
				return Optional.of(path);
			}
		}

		// 3. Try bash from PATH
		try {
			Process process = new ProcessBuilder("bash", "--version").start();
			process.waitFor();
			return Optional.of("bash"); // just use "bash"
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
