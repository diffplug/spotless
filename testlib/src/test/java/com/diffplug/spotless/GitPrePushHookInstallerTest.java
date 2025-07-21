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
package com.diffplug.spotless;

import static com.diffplug.spotless.GitPrePushHookInstaller.Executor.GRADLE;
import static com.diffplug.spotless.GitPrePushHookInstaller.Executor.MAVEN;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.GitPrePushHookInstaller.GitPreHookLogger;

class GitPrePushHookInstallerTest extends ResourceHarness {
	private final static String OS = System.getProperty("os.name");

	private final List<String> logs = new ArrayList<>();
	private final GitPreHookLogger logger = new GitPreHookLogger() {
		@Override
		public void info(String format, Object... arguments) {
			logs.add(String.format(format, arguments));
		}

		@Override
		public void warn(String format, Object... arguments) {
			logs.add(String.format(format, arguments));
		}

		@Override
		public void error(String format, Object... arguments) {
			logs.add(String.format(format, arguments));
		}
	};

	@BeforeEach
	public void beforeEach() {
		System.setProperty("os.name", "linux");
	}

	@AfterEach
	public void afterEach() {
		System.setProperty("os.name", OS);
	}

	@Test
	public void should_not_create_pre_hook_file_when_git_is_not_installed() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerGradle(logger, rootFolder());

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(2);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Git not found in root directory");
		assertThat(newFile(".git/hooks/pre-push")).doesNotExist();
	}

	@Test
	public void should_use_global_gradle_when_gradlew_is_not_installed() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerGradle(logger, rootFolder());
		setFile(".git/config").toContent("");

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(4);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Git pre-push hook not found, creating it");
		assertThat(logs).element(2).isEqualTo("Local gradle wrapper (gradlew) not found, falling back to global command 'gradle'");
		assertThat(logs).element(3).isEqualTo("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

		final var content = gradleHookContent("git_pre_hook/pre-push.created-tpl", ExecutorType.GLOBAL);
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_reinstall_pre_hook_file_when_hook_already_installed() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerGradle(logger, rootFolder());
		final var installedGlobally = gradleHookContent("git_pre_hook/pre-push.existing-installed-end-tpl", ExecutorType.GLOBAL);
		final var hookFile = setFile(".git/hooks/pre-push").toContent(installedGlobally);

		setFile("gradlew").toContent("");
		setFile(".git/config").toContent("");

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(3);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Git pre-push hook already installed, reinstalling it");
		assertThat(logs).element(2).isEqualTo("Git pre-push hook installed successfully to the file " + hookFile.getAbsolutePath());

		final var content = gradleHookContent("git_pre_hook/pre-push.existing-installed-end-tpl", ExecutorType.WRAPPER);
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_reinstall_pre_hook_file_when_hook_already_installed_in_the_middle_of_file() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerGradle(logger, rootFolder());
		final var installedGlobally = gradleHookContent("git_pre_hook/pre-push.existing-installed-middle-tpl", ExecutorType.GLOBAL);
		final var hookFile = setFile(".git/hooks/pre-push").toContent(installedGlobally);

		setFile("gradlew").toContent("");
		setFile(".git/config").toContent("");

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(3);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Git pre-push hook already installed, reinstalling it");
		assertThat(logs).element(2).isEqualTo("Git pre-push hook installed successfully to the file " + hookFile.getAbsolutePath());

		final var content = gradleHookContent("git_pre_hook/pre-push.existing-reinstalled-middle-tpl", ExecutorType.WRAPPER);
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_reinstall_a_few_times_pre_hook_file_when_hook_already_installed_in_the_middle_of_file() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerGradle(logger, rootFolder());
		final var installedGlobally = gradleHookContent("git_pre_hook/pre-push.existing-installed-middle-tpl", ExecutorType.GLOBAL);
		setFile(".git/hooks/pre-push").toContent(installedGlobally);

		setFile("gradlew").toContent("");
		setFile(".git/config").toContent("");

		// when
		gradle.install();
		gradle.install();
		gradle.install();

		// then
		final var content = gradleHookContent("git_pre_hook/pre-push.existing-reinstalled-middle-tpl", ExecutorType.WRAPPER);
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_create_pre_hook_file_when_hook_file_does_not_exists() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerGradle(logger, rootFolder());
		setFile("gradlew").toContent("");
		setFile(".git/config").toContent("");

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(3);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Git pre-push hook not found, creating it");
		assertThat(logs).element(2).isEqualTo("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

		final var content = gradleHookContent("git_pre_hook/pre-push.created-tpl", ExecutorType.WRAPPER);
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_append_to_existing_pre_hook_file_when_hook_file_exists() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerGradle(logger, rootFolder());
		setFile("gradlew").toContent("");
		setFile(".git/config").toContent("");
		setFile(".git/hooks/pre-push").toResource("git_pre_hook/pre-push.existing");

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(2);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

		final var content = gradleHookContent("git_pre_hook/pre-push.existing-installed-end-tpl", ExecutorType.WRAPPER);
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_create_pre_hook_file_for_maven_when_hook_file_does_not_exists() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());
		setFile("mvnw").toContent("");
		setFile(".git/config").toContent("");

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(3);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Git pre-push hook not found, creating it");
		assertThat(logs).element(2).isEqualTo("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

		final var content = mavenHookContent("git_pre_hook/pre-push.created-tpl", ExecutorType.WRAPPER);
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_use_global_maven_when_maven_wrapper_is_not_installed() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());
		setFile(".git/config").toContent("");

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(4);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Git pre-push hook not found, creating it");
		assertThat(logs).element(2).isEqualTo("Local maven wrapper (mvnw) not found, falling back to global command 'mvn'");
		assertThat(logs).element(3).isEqualTo("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

		final var content = mavenHookContent("git_pre_hook/pre-push.created-tpl", ExecutorType.GLOBAL);
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_use_maven_bat_wrapper_when_exists_for_windows() {
		// given
		System.setProperty("os.name", "Windows 10");
		setFile("mvnw.bat").toContent("");
		setFile("mvnw.cmd").toContent("");

		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());

		// when
		final var hook = gradle.preHookTemplate(MAVEN, "spotless:check", "spotless:apply");

		// then
		assertThat(hook).contains("SPOTLESS_EXECUTOR=./mvnw.bat");
	}

	@Test
	public void should_use_maven_cmd_wrapper_when_exists_for_windows() {
		// given
		System.setProperty("os.name", "Windows 10");
		setFile("mvnw.cmd").toContent("");

		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());

		// when
		final var hook = gradle.preHookTemplate(MAVEN, "spotless:check", "spotless:apply");

		// then
		assertThat(hook).contains("SPOTLESS_EXECUTOR=./mvnw.cmd");
	}

	@Test
	public void should_use_maven_global_when_bat_and_cmd_files_not_exists_for_windows() {
		// given
		System.setProperty("os.name", "Windows 10");
		setFile("mvnw").toContent("");

		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());

		// when
		final var hook = gradle.preHookTemplate(MAVEN, "spotless:check", "spotless:apply");

		// then
		assertThat(hook).contains("SPOTLESS_EXECUTOR=mvn");
	}

	@Test
	public void should_use_gradle_bat_wrapper_when_exists_for_windows() {
		// given
		System.setProperty("os.name", "Windows 10");
		setFile("gradlew.bat").toContent("");
		setFile("gradlew.cmd").toContent("");
		setFile("gradlew").toContent("");

		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());

		// when
		final var hook = gradle.preHookTemplate(GRADLE, "spotlessCheck", "spotlessApply");

		// then
		assertThat(hook).contains("SPOTLESS_EXECUTOR=./gradlew.bat");
	}

	@Test
	public void should_use_gradle_cmd_wrapper_when_exists_for_windows() {
		// given
		System.setProperty("os.name", "Windows 10");
		setFile("gradlew.cmd").toContent("");
		setFile("gradlew").toContent("");

		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());

		// when
		final var hook = gradle.preHookTemplate(GRADLE, "spotlessCheck", "spotlessApply");

		// then
		assertThat(hook).contains("SPOTLESS_EXECUTOR=./gradlew.cmd");
	}

	@Test
	public void should_use_gradle_global_when_bat_and_cmd_files_not_exists_for_windows() {
		// given
		System.setProperty("os.name", "Windows 10");
		setFile("gradlew").toContent("");

		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());

		// when
		final var hook = gradle.preHookTemplate(GRADLE, "spotlessCheck", "spotlessApply");

		// then
		assertThat(hook).contains("SPOTLESS_EXECUTOR=gradle");
	}

	private String gradleHookContent(String resourcePath, ExecutorType executorType) {
		return getTestResource(resourcePath)
				.replace("${executor}", executorType == ExecutorType.WRAPPER ? "./" + newFile("gradlew").getName() : "gradle")
				.replace("${checkCommand}", "spotlessCheck")
				.replace("${applyCommand}", "spotlessApply");
	}

	private String mavenHookContent(String resourcePath, ExecutorType executorType) {
		return getTestResource(resourcePath)
				.replace("${executor}", executorType == ExecutorType.WRAPPER ? "./" + newFile("mvnw").getName() : "mvn")
				.replace("${checkCommand}", "spotless:check")
				.replace("${applyCommand}", "spotless:apply");
	}

	private enum ExecutorType {
		WRAPPER, GLOBAL
	}
}
