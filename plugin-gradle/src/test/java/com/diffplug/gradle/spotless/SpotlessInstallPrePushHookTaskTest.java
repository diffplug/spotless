package com.diffplug.gradle.spotless;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpotlessInstallPrePushHookTaskTest extends GradleIntegrationHarness {

	@Test
	public void should_create_pre_hook_file_when_hook_file_does_not_exists() throws Exception {
		// given
		final var gradlew = setFile("gradlew").toContent("");
		setFile(".git/config").toContent("");
		setFile("build.gradle").toLines(
			"plugins {",
			"    id 'java'",
			"    id 'com.diffplug.spotless'",
			"}",
			"repositories { mavenCentral() }"
		);

		// when
		var output = gradleRunner()
			.withArguments("spotlessInstallGitPrePushHook")
			.build()
			.getOutput();

		// then
		assertThat(output).contains("Installing git pre-push hook");
		assertThat(output).contains("Git pre-push hook not found, creating it");
		assertThat(output).contains("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push"));

		final var content = getTestResource("git_pre_hook/pre-push.created")
			.replace("${executor}", gradlew.getAbsolutePath())
			.replace("${checkCommand}", "spotlessCheck")
			.replace("${applyCommand}", "spotlessApply");
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_append_to_existing_pre_hook_file_when_hook_file_exists() throws Exception {
		// given
		final var gradlew = setFile("gradlew").toContent("");
		setFile(".git/config").toContent("");
		setFile("build.gradle").toLines(
			"plugins {",
			"    id 'java'",
			"    id 'com.diffplug.spotless'",
			"}",
			"repositories { mavenCentral() }"
		);
		setFile(".git/hooks/pre-push").toResource("git_pre_hook/pre-push.existing");

		// when
		final var output = gradleRunner()
			.withArguments("spotlessInstallGitPrePushHook")
			.build()
			.getOutput();

		// then
		assertThat(output).contains("Installing git pre-push hook");
		assertThat(output).contains("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push"));

		final var content = getTestResource("git_pre_hook/pre-push.existing-added")
			.replace("${executor}", gradlew.getAbsolutePath())
			.replace("${checkCommand}", "spotlessCheck")
			.replace("${applyCommand}", "spotlessApply");
		assertFile(".git/hooks/pre-push").hasContent(content);
	}
}
