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

import java.io.IOException;

import org.junit.jupiter.api.Test;

class SpotlessInstallPrePushHookMojoTest extends MavenIntegrationHarness {

	@Test
	public void should_create_pre_hook_file_when_hook_file_does_not_exists() throws Exception {
		// given
		setFile(".git/config").toContent("");
		setFile("license.txt").toResource("license/TestLicense");
		writePomWithJavaLicenseHeaderStep();

		// when
		final var output = mavenRunner()
				.withArguments("spotless:install-git-pre-push-hook")
				.runNoError()
				.stdOutUtf8();

		// then
		assertThat(output).contains("Installing git pre-push hook");
		assertThat(output).contains("Git pre-push hook not found, creating it");
		assertThat(output).contains("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push"));

		final var content = getTestResource("git_pre_hook/pre-push.created")
				.replace("${executor}", "mvn")
				.replace("${checkCommand}", "spotless:check")
				.replace("${applyCommand}", "spotless:apply");
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	@Test
	public void should_append_to_existing_pre_hook_file_when_hook_file_exists() throws Exception {
		// given
		setFile(".git/config").toContent("");
		setFile("license.txt").toResource("license/TestLicense");
		setFile(".git/hooks/pre-push").toResource("git_pre_hook/pre-push.existing");

		writePomWithJavaLicenseHeaderStep();

		// when
		final var output = mavenRunner()
				.withArguments("spotless:install-git-pre-push-hook")
				.runNoError()
				.stdOutUtf8();

		// then
		assertThat(output).contains("Installing git pre-push hook");
		assertThat(output).contains("Git pre-push hook installed successfully to the file " + newFile(".git/hooks/pre-push"));

		final var content = getTestResource("git_pre_hook/pre-push.existing-added")
				.replace("${executor}", "mvn")
				.replace("${checkCommand}", "spotless:check")
				.replace("${applyCommand}", "spotless:apply");
		assertFile(".git/hooks/pre-push").hasContent(content);
	}

	private void writePomWithJavaLicenseHeaderStep() throws IOException {
		writePomWithJavaSteps(
				"<licenseHeader>",
				"  <file>${basedir}/license.txt</file>",
				"</licenseHeader>");
	}
}
