/*
 * Copyright 2026 DiffPlug
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
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SpotlessReportMojoTest extends MavenIntegrationHarness {
	private static final String CLEAN = "class Clean {}\n";
	private static final String DIRTY = "class Dirty {}   ";
	private static final String MAVEN_3_9 = "distributionUrl=https://repo1.maven.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip\n";
	private static final String[] STEPS = {"<trimTrailingWhitespace/>", "<endWithNewline/>"};

	@Test
	void reportShowsFilesThatNeedFormattingWithoutChangingThem() throws Exception {
		writePomWithFormatSteps(STEPS);
		setFile("src/main/java/Clean.java").toContent(CLEAN);
		setFile("src/main/java/Dirty.java").toContent(DIRTY);

		String output = mavenRunner().withArguments("spotless:report").runNoError().stdOutUtf8();

		assertThat(output).contains("Spotless report: 2 files, 1 needs formatting.");
		assertFile("src/main/java/Dirty.java").hasContent(DIRTY);
		assertThat(read("target/site/spotless.html")).contains("src/main/java/Dirty.java").contains("1 needs formatting");
		assertThat(read("target/site/spotless-src.main.java.Dirty.java.html")).contains("+class Dirty {}");
	}

	@Test
	void reportLeavesTheUpToDateIndexAlone() throws Exception {
		writePomWithFormatSteps(STEPS);
		setFile("src/main/java/Clean.java").toContent(CLEAN);
		mavenRunner().withArguments("spotless:apply").runNoError();
		byte[] index = Files.readAllBytes(newFile("target/spotless-index").toPath());
		setFile("src/main/java/Dirty.java").toContent(DIRTY);

		String output = mavenRunner().withArguments("spotless:report").runNoError().stdOutUtf8();

		// Clean.java is in the index, but the report still looks at it
		assertThat(output).contains("Spotless report: 2 files, 1 needs formatting.");
		assertThat(Files.readAllBytes(newFile("target/spotless-index").toPath())).isEqualTo(index);
	}

	@ParameterizedTest
	@ValueSource(strings = {"spotless.report.skip", "spotless.skip"})
	void reportCanBeSkipped(String property) throws Exception {
		writePomWithFormatSteps(STEPS);
		setFile("src/main/java/Dirty.java").toContent(DIRTY);

		String output = mavenRunner().withArguments("spotless:report", "-D" + property + "=true").runNoError().stdOutUtf8();

		assertThat(output).contains("Spotless report skipped");
		assertThat(newFile("target/site/spotless.html")).doesNotExist();
	}

	@Test
	void siteWithMavenSitePlugin3_12_UsesDoxia1() throws Exception {
		writePomForSite("3.12.1", new String[0], "");
		setFile("src/main/java/Dirty.java").toContent(DIRTY);

		mavenRunner().withArguments("site").runNoError();

		assertSiteHasSpotlessReport();
	}

	@Test
	void siteWithMavenSitePlugin3_21_UsesDoxia2() throws Exception {
		// maven-site-plugin 3.20 and later need Maven 3.6.3 or later
		setFile(".mvn/wrapper/maven-wrapper.properties").toContent(MAVEN_3_9);
		writePomForSite("3.21.0", new String[0], "");
		setFile("src/main/java/Dirty.java").toContent(DIRTY);

		mavenRunner().withArguments("site").runNoError();

		assertSiteHasSpotlessReport();
	}

	@Test
	void siteReportTakesBuildConfigurationButPrefersReporting() throws Exception {
		setFile(".mvn/wrapper/maven-wrapper.properties").toContent(MAVEN_3_9);
		writePomForSite("3.21.0",
				// the site owns the output directory, and <reporting> wins over <build>
				new String[]{"<outputDirectory>${project.build.directory}/elsewhere</outputDirectory>", "<maxDiffLines>1</maxDiffLines>"},
				"<configuration><maxDiffLines>2</maxDiffLines></configuration>");
		setFile("src/main/java/Dirty.java").toContent(DIRTY);

		mavenRunner().withArguments("site").runNoError();

		// the diff has 4 lines: the hunk header, the removed line, "\ No newline at end of file" and the added line
		assertThat(read("target/site/spotless-src.main.java.Dirty.java.html")).contains("2 more lines of the diff are not shown.");
		assertThat(newFile("target/elsewhere")).doesNotExist();
	}

	@Test
	void skipInBuildConfigurationSkipsTheSiteReport() throws Exception {
		writePomForSite("3.12.1", new String[]{"<skip>true</skip>"}, "");
		setFile("src/main/java/Dirty.java").toContent(DIRTY);

		mavenRunner().withArguments("site").runNoError();

		assertThat(newFile("target/site/spotless.html")).doesNotExist();
	}

	/**
	 * @param buildConfiguration extra plugin-level configuration in {@code <build>}, next to the formats
	 * @param reportingConfiguration XML inside the {@code <reporting>} entry for the plugin, which has no version: it takes the one from {@code <build>}
	 */
	private void writePomForSite(String sitePluginVersion, String[] buildConfiguration, String reportingConfiguration) throws IOException {
		String[] formats = formats(groupWithSteps("format", including("src/**/java/**/*.java"), STEPS));
		String[] configuration = new String[formats.length + buildConfiguration.length];
		System.arraycopy(formats, 0, configuration, 0, formats.length);
		System.arraycopy(buildConfiguration, 0, configuration, formats.length, buildConfiguration.length);
		writePomWithReporting(
				new String[]{
						"<plugin>",
						"  <groupId>org.apache.maven.plugins</groupId>",
						"  <artifactId>maven-site-plugin</artifactId>",
						"  <version>" + sitePluginVersion + "</version>",
						"</plugin>"},
				configuration,
				// excludeDefaults: otherwise the site plugin adds the latest maven-project-info-reports-plugin, which may not suit the Maven version under test
				"<reporting><excludeDefaults>true</excludeDefaults><plugins><plugin><groupId>com.diffplug.spotless</groupId><artifactId>spotless-maven-plugin</artifactId>"
						+ reportingConfiguration + "</plugin></plugins></reporting>");
	}

	private void assertSiteHasSpotlessReport() throws IOException {
		assertThat(read("target/site/spotless.html")).contains("src/main/java/Dirty.java").contains("1 needs formatting");
		assertThat(read("target/site/spotless-src.main.java.Dirty.java.html")).contains("+class Dirty {}");
		assertThat(read("target/site/project-reports.html")).contains("Spotless");
		assertFile("src/main/java/Dirty.java").hasContent(DIRTY);
	}
}
