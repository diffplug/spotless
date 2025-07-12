package com.diffplug.spotless;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.GitPrePushHookInstaller.GitPreHookLogger;

class GitPrePushHookInstallerTest extends ResourceHarness {
	private final List<String> logs = new ArrayList<>();
	private final GitPreHookLogger logger = new GitPreHookLogger() {
		@Override
		public void info(String format, Object... arguments) {
			logs.add(String.format(format, arguments));
		}

		@Override
		public void error(String format, Object... arguments) {
			logs.add(String.format(format, arguments));
		}
	};

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
	public void should_not_create_pre_hook_file_when_gradlew_is_not_installed() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerGradle(logger, rootFolder());
		setFile(".git/config").toContent("");

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(2);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Failed to find gradlew in root directory");
		assertThat(newFile(".git/hooks/pre-push")).doesNotExist();
	}

	@Test
	public void should_not_create_pre_hook_file_when_hook_already_installed() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerGradle(logger, rootFolder());
		final var hookFile = setFile(".git/hooks/pre-push").toResource("git_pre_hook/pre-push.existing-added");

		setFile("gradlew").toContent("");
		setFile(".git/config").toContent("");

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(2);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Skipping, git pre-push hook already installed " + hookFile.getAbsolutePath());
		assertThat(hookFile).content().isEqualTo(getTestResource("git_pre_hook/pre-push.existing-added"));
	}

	@Test
	public void should_create_pre_hook_file_when_hook_file_does_not_exists() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerGradle(logger, rootFolder());
		final var gradlew = setFile("gradlew").toContent("");
		setFile(".git/config").toContent("");

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(3);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Git pre-push hook not found, creating it");
		assertThat(logs).element(2).isEqualTo("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

		final var content = gradleHookContent("git_pre_hook/pre-push.created");
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_append_to_existing_pre_hook_file_when_hook_file_exists() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerGradle(logger, rootFolder());
		final var gradlew = setFile("gradlew").toContent("");
		setFile(".git/config").toContent("");
		setFile(".git/hooks/pre-push").toResource("git_pre_hook/pre-push.existing");

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(2);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

		final var content = gradleHookContent("git_pre_hook/pre-push.existing-added");
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_create_pre_hook_file_for_maven_when_hook_file_does_not_exists() throws Exception {
		// given
		final var gradle = new GitPrePushHookInstallerMaven(logger, rootFolder());
		setFile(".git/config").toContent("");

		// when
		gradle.install();

		// then
		assertThat(logs).hasSize(3);
		assertThat(logs).element(0).isEqualTo("Installing git pre-push hook");
		assertThat(logs).element(1).isEqualTo("Git pre-push hook not found, creating it");
		assertThat(logs).element(2).isEqualTo("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push").getAbsolutePath());

		final var content = mavenHookContent("git_pre_hook/pre-push.created");
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	private String gradleHookContent(String resourcePath) {
		return getTestResource(resourcePath)
			.replace("${executor}", setFile("gradlew").toContent("").getAbsolutePath())
			.replace("${checkCommand}", "spotlessCheck")
			.replace("${applyCommand}", "spotlessApply");
	}

	private String mavenHookContent(String resourcePath) {
		return getTestResource(resourcePath)
			.replace("${executor}", "mvn")
			.replace("${checkCommand}", "spotless:check")
			.replace("${applyCommand}", "spotless:apply");
	}
}
