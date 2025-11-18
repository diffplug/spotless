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
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.GitPrePushHookInstaller.GitPreHookLogger;

class GitPrePushHookInstallerTest extends ResourceHarness {
	private static final String OS = System.getProperty("os.name");

	private final List<String> logs = new CopyOnWriteArrayList<>();
	private final GitPreHookLogger logger = new GitPreHookLogger() {
		@Override
		public void info(String format, Object... arguments) {
			logs.add(format.formatted(arguments));
		}

		@Override
		public void warn(String format, Object... arguments) {
			logs.add(format.formatted(arguments));
		}

		@Override
		public void error(String format, Object... arguments) {
			logs.add(format.formatted(arguments));
		}
	};

	@BeforeEach
	public void beforeEach() {
		System.setProperty("os.name", "linux");

		final var hookFile = newFile(".git/hooks/pre-push");
		if (hookFile.exists()) {
			hookFile.delete();
		}
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
		assertThat(logs).containsExactly(
				"Installing git pre-push hook",
				"Git not found in root directory");
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
		assertThat(logs).containsExactly(
				"Installing git pre-push hook",
				"Git pre-push hook not found, creating it",
				"Local gradle wrapper (gradlew) not found, falling back to global command 'gradle'",
				"Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

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
		assertThat(logs).containsExactly(
				"Installing git pre-push hook",
				"Git pre-push hook already installed, reinstalling it",
				"Git pre-push hook installed successfully to the file " + hookFile.getAbsolutePath());

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
		assertThat(logs).containsExactly(
				"Installing git pre-push hook",
				"Git pre-push hook already installed, reinstalling it",
				"Git pre-push hook installed successfully to the file " + hookFile.getAbsolutePath());

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
		assertThat(logs).containsExactly(
				"Installing git pre-push hook",
				"Git pre-push hook not found, creating it",
				"Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

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
		assertThat(logs).containsExactly(
				"Installing git pre-push hook",
				"Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

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
		assertThat(logs).containsExactly(
				"Installing git pre-push hook",
				"Git pre-push hook not found, creating it",
				"Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

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
		assertThat(logs).containsExactly(
				"Installing git pre-push hook",
				"Git pre-push hook not found, creating it",
				"Local maven wrapper (mvnw) not found, falling back to global command 'mvn'",
				"Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

		final var content = mavenHookContent("git_pre_hook/pre-push.created-tpl", ExecutorType.GLOBAL);
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_use_maven_bat_wrapper_when_exists_for_windows() {
		// given
		System.setProperty("os.name", "Windows 10");
		final var batFile = setFile("mvnw.bat").toContent("");
		setFile("mvnw.cmd").toContent("");

		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());

		// when
		final var hook = gradle.preHookTemplate(MAVEN, "spotless:check", "spotless:apply");

		// then
		assertThat(hook).contains("SPOTLESS_EXECUTOR=" + fileAbsolutePath(batFile));
	}

	@Test
	public void should_use_maven_cmd_wrapper_when_exists_for_windows() {
		// given
		System.setProperty("os.name", "Windows 10");
		final var executorFile = setFile("mvnw.cmd").toContent("");

		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());

		// when
		final var hook = gradle.preHookTemplate(MAVEN, "spotless:check", "spotless:apply");

		// then
		assertThat(hook).contains("SPOTLESS_EXECUTOR=" + fileAbsolutePath(executorFile));
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
		final var executorFile = setFile("gradlew.bat").toContent("");
		setFile("gradlew.cmd").toContent("");
		setFile("gradlew").toContent("");

		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());

		// when
		final var hook = gradle.preHookTemplate(GRADLE, "spotlessCheck", "spotlessApply");

		// then
		assertThat(hook).contains("SPOTLESS_EXECUTOR=" + fileAbsolutePath(executorFile));
	}

	@Test
	public void should_use_gradle_cmd_wrapper_when_exists_for_windows() {
		// given
		System.setProperty("os.name", "Windows 10");
		final var executorFile = setFile("gradlew.cmd").toContent("");
		setFile("gradlew").toContent("");

		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());

		// when
		final var hook = gradle.preHookTemplate(GRADLE, "spotlessCheck", "spotlessApply");

		// then
		assertThat(hook).contains("SPOTLESS_EXECUTOR=" + fileAbsolutePath(executorFile));
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

	@Test
	public void should_handle_parallel_installation() {
		// given
		setFile(".git/config").toContent("");

		// when
		parallelRun(() -> {
			final var gradle = new GitPrePushHookInstallerGradle(logger, rootFolder());
			gradle.install();
		});

		// then
		assertThat(logs).contains(
				"Installing git pre-push hook",
				"Git pre-push hook not found, creating it",
				"Parallel Spotless Git pre-push hook installation detected, skipping installation",
				"Local gradle wrapper (gradlew) not found, falling back to global command 'gradle'",
				"Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

		final var content = gradleHookContent("git_pre_hook/pre-push.created-tpl", ExecutorType.GLOBAL);
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	private String gradleHookContent(String resourcePath, ExecutorType executorType) {
		return getTestResource(resourcePath)
				.replace("${executor}", executorType == ExecutorType.WRAPPER ? fileAbsolutePath(newFile("gradlew")) : "gradle")
				.replace("${checkCommand}", "spotlessCheck")
				.replace("${applyCommand}", "spotlessApply");
	}

	private String mavenHookContent(String resourcePath, ExecutorType executorType) {
		return getTestResource(resourcePath)
				.replace("${executor}", executorType == ExecutorType.WRAPPER ? fileAbsolutePath(newFile("mvnw")) : "mvn")
				.replace("${checkCommand}", "spotless:check")
				.replace("${applyCommand}", "spotless:apply");
	}

	private String fileAbsolutePath(File file) {
		return file.getAbsolutePath().replace("\\", "/");
	}

	private void parallelRun(ThrowableRun runnable) {
		IntStream.range(0, 5)
				.mapToObj(i -> new Thread(() -> {
					try {
						runnable.run();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}))
				.peek(Thread::start)
				.collect(toList())
				.forEach(t -> {
					try {
						t.join();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				});
	}

	private enum ExecutorType {
		WRAPPER, GLOBAL
	}

	private interface ThrowableRun {
		void run() throws Exception;
	}
}
