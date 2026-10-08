/*
 * Copyright 2025-2026 DiffPlug
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
package com.diffplug.gradle.spotless;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

class SpotlessInstallPrePushHookTaskTest extends GradleIntegrationHarness {

	@Test
	public void should_create_pre_hook_file_when_hook_file_does_not_exists() throws Exception {
		// given
		gitCommand("init", "--quiet");
		newFile(".git/hooks").mkdirs();
		setGradleFile();

		// when
		var output = getExecutionOutput();

		// then
		assertThat(output).contains("Installing git pre-push hook");
		assertThat(output).contains("Git pre-push hook not found, creating it");
		assertThat(output).contains("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push"));

		final var content = getTestResource("git_pre_hook/pre-push.created-tpl")
				.replace("${executor}", "gradle")
				.replace("${checkCommand}", "spotlessCheck")
				.replace("${applyCommand}", "spotlessApply");
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_append_to_existing_pre_hook_file_when_hook_file_exists() throws Exception {
		// given
		gitCommand("init", "--quiet");
		setFile(".git/hooks/pre-push").toResource("git_pre_hook/pre-push.existing");
		setGradleFile();

		// when
		final var output = getExecutionOutput();

		// then
		assertThat(output).contains("Installing git pre-push hook");
		assertThat(output).contains("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push"));

		final var content = getTestResource("git_pre_hook/pre-push.existing-installed-end-tpl")
				.replace("${executor}", "gradle")
				.replace("${checkCommand}", "spotlessCheck")
				.replace("${applyCommand}", "spotlessApply");
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_use_hooksPath_if_set_in_config() throws Exception {
		// given
		gitCommand("init", "--quiet");
		gitCommand("config", "--local", "core.hooksPath", ".githooks");
		setGradleFile();

		// when
		final var output = getExecutionOutput();

		// then
		assertThat(output).contains("Installing git pre-push hook");
		assertThat(output).contains("Git pre-push hook installed successfully to the file " + newFile(".githooks/pre-push"));

		final var content = getTestResource("git_pre_hook/pre-push.created-tpl")
				.replace("${executor}", "gradle")
				.replace("${checkCommand}", "spotlessCheck")
				.replace("${applyCommand}", "spotlessApply");
		assertFile(".githooks/pre-push").hasContent(content);
	}

	@Test
	public void should_skip_when_not_in_git_worktree() throws Exception {
		setGradleFile();

		// when
		final var output = getExecutionOutput();

		// then
		assertThat(output).containsPattern("Skipping Spotless pre-push hook installation - directory .* is not a supported Git repository");
	}

	@Test
	public void should_skip_when_prehook_config_not_in_parent_project() throws Exception {
		gitCommand("init", "--quiet");
		var outsideHooks = rootFolder().toPath().resolveSibling(rootFolder().getName() + "-outside-hooks");
		gitCommand("config", "--local", "core.hooksPath", outsideHooks.toString());
		setGradleFile();

		// when
		final var output = getExecutionOutput();

		// then
		assertThat(outsideHooks).doesNotExist();
		assertThat(output).containsPattern("Skipping Spotless pre-push hook installation - not supported destination dir");
	}

	@Test
	public void should_skip_when_prehook_file_a_directory() throws Exception {
		gitCommand("init", "--quiet");
		gitCommand("config", "--local", "core.hooksPath", ".githooks");
		Files.createDirectories(newFile(".githooks/pre-push").toPath());
		setGradleFile();

		// when
		final var output = getExecutionOutput();

		// then
		assertThat(output).containsPattern("Skipping Spotless pre-push hook installation - not supported destination dir");
	}

	@Test
	public void should_skip_when_prehook_config_a_symlink() throws Exception {
		gitCommand("init", "--quiet");
		gitCommand("config", "--local", "core.hooksPath", ".githooks");

		var target = setFile("target-hook").toContent("original content");
		Files.createDirectories(newFile(".githooks").toPath());

		var hook = newFile(".githooks/pre-push").toPath();
		Files.createSymbolicLink(hook, target.toPath());

		setGradleFile();

		// when
		final var output = getExecutionOutput();

		// then
		assertThat(Files.isSymbolicLink(hook)).isTrue();
		assertThat(Files.readString(target.toPath())).isEqualTo("original content");
		assertThat(output).containsPattern("Skipping Spotless pre-push hook installation - not supported destination dir");
	}

	private String getExecutionOutput() throws IOException {
		return gradleRunner()
				.withArguments("spotlessInstallGitPrePushHook", "--system-prop=org.gradle.configuration-cache=true")
				.build()
				.getOutput();
	}

	private void setGradleFile() {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }");
	}

	private void gitCommand(String... args) throws Exception {
		var command = new java.util.ArrayList<String>();
		command.add("git");
		command.addAll(java.util.List.of(args));

		var process = new ProcessBuilder(command)
				.directory(rootFolder())
				.redirectErrorStream(true)
				.start();

		String output;
		try (var stream = process.getInputStream()) {
			output = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
		assertThat(process.waitFor())
				.as("git %s: %s", String.join(" ", args), output)
				.isZero();
	}
}
