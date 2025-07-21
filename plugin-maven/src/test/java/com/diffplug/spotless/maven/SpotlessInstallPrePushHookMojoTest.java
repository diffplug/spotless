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
import java.util.Locale;

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

		final var hookContent = getHookContent("git_pre_hook/pre-push.created-tpl");
		assertFile(".git/hooks/pre-push").hasContent(hookContent);
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

		final var hookContent = getHookContent("git_pre_hook/pre-push.existing-installed-end-tpl");
		assertFile(".git/hooks/pre-push").hasContent(hookContent);
	}

	private void writePomWithJavaLicenseHeaderStep() throws IOException {
		writePomWithJavaSteps(
				"<licenseHeader>",
				"  <file>${basedir}/license.txt</file>",
				"</licenseHeader>");
	}

	private String getHookContent(String resourceFile) {
		final var executorFile = executorWrapperFile();
		final var executorPath = executorFile.exists() ? executorFile.getName() : "mvn";
		return getTestResource(resourceFile)
				.replace("${executor}", "./" + executorPath)
				.replace("${checkCommand}", "spotless:check")
				.replace("${applyCommand}", "spotless:apply");
	}

	private File executorWrapperFile() {
		if (System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win")) {
			final var bat = newFile("mvnw.bat");
			if (bat.exists()) {
				return bat;
			}

			return newFile("mvnw.cmd");
		}

		return newFile("mvnw");
	}
}
