/*
 * Copyright 2021-2023 DiffPlug
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
package com.diffplug.spotless.maven.incremental;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;
import com.diffplug.spotless.maven.MavenRunner;

class UpToDateCheckingTest extends MavenIntegrationHarness {

	private static final String DISABLED_MESSAGE = "Up-to-date checking disabled";

	@Test
	void upToDateCheckingEnabledByDefault() throws Exception {
		writePom(
				"<java>",
				"  <googleJavaFormat/>",
				"</java>");

		List<File> files = writeUnformattedFiles(1);
		String output = runSpotlessApply();

		assertThat(output).doesNotContain(DISABLED_MESSAGE);
		assertFormatted(files);
	}

	@Test
	void explicitlyEnableUpToDateChecking() throws Exception {
		writePomWithUpToDateCheckingEnabled(true);

		List<File> files = writeUnformattedFiles(1);
		String output = runSpotlessApply();

		assertThat(output).doesNotContain(DISABLED_MESSAGE);
		assertFormatted(files);
	}

	@Test
	void explicitlyDisableUpToDateChecking() throws Exception {
		writePomWithUpToDateCheckingEnabled(false);

		List<File> files = writeUnformattedFiles(1);
		String output = runSpotlessApply();

		assertThat(output).contains(DISABLED_MESSAGE);
		assertFormatted(files);
	}

	@Test
	void enableUpToDateCheckingWithPluginDependencies() throws Exception {
		writePomWithPluginManagementAndDependency();

		List<File> files = writeUnformattedFiles(1);
		String output = runSpotlessApply();

		assertThat(output).doesNotContain(DISABLED_MESSAGE);
		assertFormatted(files);
	}

	@Test
	void enableUpToDateCheckingWithPluginDependenciesMaven3_6_3() throws Exception {
		writePomWithPluginManagementAndDependency();

		setFile(".mvn/wrapper/maven-wrapper.properties").toContent("distributionUrl=https://repo1.maven.org/maven2/org/apache/maven/apache-maven/3.6.3/apache-maven-3.6.3-bin.zip\n");

		List<File> files = writeUnformattedFiles(1);
		String output = runSpotlessApply();

		assertThat(output).doesNotContain(DISABLED_MESSAGE);
		assertFormatted(files);
	}

	@Test
	void enableUpToDateCheckingCustomIndexFile() throws Exception {
		Path tempDirectory = newFolder("index-files").toPath();
		Path indexFile = tempDirectory.resolve("com.diffplug.spotless/spotless-maven-plugin-tests.index");
		assertThat(indexFile.getParent()).doesNotExist();
		assertThat(indexFile).doesNotExist();
		writePomWithUpToDateCheckingEnabledIndexFile(true, tempDirectory + "/${project.groupId}/${project.artifactId}.index");

		List<File> files = writeUnformattedFiles(1);
		String output = runSpotlessApply();

		assertThat(output).doesNotContain(DISABLED_MESSAGE);
		assertFormatted(files);
		assertThat(indexFile.getParent()).exists();
		assertThat(indexFile).exists();
	}

	@Test
	void disableUpToDateCheckingCustomIndexFile() throws Exception {
		Path tempDirectory = newFolder("index-files").toPath();
		Path indexFile = tempDirectory.resolve("com.diffplug.spotless/spotless-maven-plugin-tests.index");
		Files.createDirectories(indexFile.getParent());
		Files.createFile(indexFile);
		assertThat(indexFile.getParent()).exists();
		assertThat(indexFile).exists();
		writePomWithUpToDateCheckingEnabledIndexFile(false, tempDirectory + "/${project.groupId}/${project.artifactId}.index");

		List<File> files = writeUnformattedFiles(1);
		String output = runSpotlessApply();

		assertThat(output).contains(DISABLED_MESSAGE);
		assertFormatted(files);
		assertThat(indexFile.getParent()).exists();
		assertThat(indexFile).doesNotExist();
	}

	@Test
	void spotlessApplyRecordsCorrectlyFormattedFiles() throws Exception {
		writePomWithUpToDateCheckingEnabled(true);
		List<File> files = writeFormattedFiles(5);

		String applyOutput1 = runSpotlessApply();
		assertSpotlessApplyDidNotSkipAnyFiles(applyOutput1);
		assertFormatted(files);

		String applyOutput2 = runSpotlessApply();
		assertSpotlessApplySkipped(files, applyOutput2);

		String checkOutput = runSpotlessCheck();
		assertSpotlessCheckSkipped(files, checkOutput);
	}

	@Test
	void spotlessApplyRecordsUnformattedFiles() throws Exception {
		writePomWithUpToDateCheckingEnabled(true);
		List<File> files = writeUnformattedFiles(4);

		String applyOutput1 = runSpotlessApply();
		assertSpotlessApplyDidNotSkipAnyFiles(applyOutput1);
		assertFormatted(files);

		String applyOutput2 = runSpotlessApply();
		assertSpotlessApplySkipped(files, applyOutput2);

		String checkOutput = runSpotlessCheck();
		assertSpotlessCheckSkipped(files, checkOutput);
	}

	@Test
	void spotlessCheckRecordsCorrectlyFormattedFiles() throws Exception {
		writePomWithUpToDateCheckingEnabled(true);
		List<File> files = writeFormattedFiles(7);

		String checkOutput1 = runSpotlessCheck();
		assertSpotlessCheckDidNotSkipAnyFiles(checkOutput1);

		String checkOutput2 = runSpotlessCheck();
		assertSpotlessCheckSkipped(files, checkOutput2);

		String applyOutput = runSpotlessApply();
		assertSpotlessApplySkipped(files, applyOutput);
	}

	@Test
	void spotlessCheckRecordsUnformattedFiles() throws Exception {
		writePomWithUpToDateCheckingEnabled(true);
		List<File> files = writeUnformattedFiles(6);

		String checkOutput1 = runSpotlessCheckOnUnformattedFiles();
		assertSpotlessCheckDidNotSkipAnyFiles(checkOutput1);

		String checkOutput2 = runSpotlessCheckOnUnformattedFiles();
		assertSpotlessCheckDidNotSkipAnyFiles(checkOutput2);

		String applyOutput = runSpotlessApply();
		assertSpotlessApplyDidNotSkipAnyFiles(applyOutput);
		assertFormatted(files);

		String checkOutput3 = runSpotlessCheck();
		assertSpotlessCheckSkipped(files, checkOutput3);
	}

	private void writePomWithPluginManagementAndDependency() throws IOException {
		setFile("pom.xml").toContent(createPomXmlContent("/pom-test-management.xml.mustache",
				null,
				null,
				new String[]{
						"<java>",
						"  <googleJavaFormat/>",
						"</java>"},
				new String[]{
						"<dependencies>",
						"  <dependency>",
						"    <groupId>javax.inject</groupId>",
						"    <artifactId>javax.inject</artifactId>",
						"    <version>1</version>",
						"  </dependency>",
						"</dependencies>"},
				null));
	}

	private void writePomWithUpToDateCheckingEnabled(boolean enabled) throws IOException {
		writePom(
				"<java>",
				"  <googleJavaFormat/>",
				"</java>",
				"<upToDateChecking>",
				"  <enabled>" + enabled + "</enabled>",
				"</upToDateChecking>");
	}

	private void writePomWithUpToDateCheckingEnabledIndexFile(boolean enabled, String indexFile) throws IOException {
		writePom(
				"<java>",
				"  <googleJavaFormat/>",
				"</java>",
				"<upToDateChecking>",
				"  <enabled>" + enabled + "</enabled>",
				"  <indexFile>" + indexFile + "</indexFile>",
				"</upToDateChecking>");
	}

	private List<File> writeFormattedFiles(int count) throws IOException {
		return writeFiles("java/googlejavaformat/JavaCodeFormatted18.test", "formatted", count);
	}

	private List<File> writeUnformattedFiles(int count) throws IOException {
		return writeFiles("java/googlejavaformat/JavaCodeUnformatted.test", "unformatted", count);
	}

	private List<File> writeFiles(String resource, String suffix, int count) throws IOException {
		List<File> result = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			String path = "src/main/java/test_" + suffix + "_" + i + ".java";
			File file = setFile(path).toResource(resource);
			result.add(file);
		}
		return result;
	}

	private String runSpotlessApply() throws Exception {
		return mavenRunnerForGoal("apply").runNoError().stdOutUtf8();
	}

	private String runSpotlessCheck() throws Exception {
		return mavenRunnerForGoal("check").runNoError().stdOutUtf8();
	}

	private String runSpotlessCheckOnUnformattedFiles() throws Exception {
		return mavenRunnerForGoal("check").runHasError().stdOutUtf8();
	}

	private MavenRunner mavenRunnerForGoal(String goal) throws IOException {
		// -X enables debug logging
		return mavenRunner().withArguments("-X", "spotless:" + goal);
	}

	private void assertFormatted(List<File> files) throws IOException {
		for (File file : files) {
			assertFile(file).sameAsResource("java/googlejavaformat/JavaCodeFormatted18.test");
		}
	}

	private void assertSpotlessApplyDidNotSkipAnyFiles(String applyOutput) {
		assertThat(applyOutput).doesNotContain("Spotless will not format");
	}

	private void assertSpotlessCheckDidNotSkipAnyFiles(String checkOutput) {
		assertThat(checkOutput).doesNotContain("Spotless will not format");
	}

	private void assertSpotlessApplySkipped(List<File> files, String applyOutput) {
		for (File file : files) {
			assertThat(applyOutput).contains("Spotless will not format an up-to-date file: " + file);
		}
	}

	private void assertSpotlessCheckSkipped(List<File> files, String checkOutput) {
		for (File file : files) {
			assertThat(checkOutput).contains("Spotless will not check an up-to-date file: " + file);
		}
	}
}
