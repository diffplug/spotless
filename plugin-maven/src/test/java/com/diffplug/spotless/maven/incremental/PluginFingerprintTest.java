/*
 * Copyright 2021-2022 DiffPlug
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.maven.MavenIntegrationHarness;

class PluginFingerprintTest extends MavenIntegrationHarness {

	private static final String VERSION_1 = "1.0.0";
	private static final String VERSION_2 = "2.0.0";

	private static final String[] EXECUTION_1 = {
			"<execution>",
			"  <id>check</id>",
			"  <goals>",
			"    <goal>check</goal>",
			"  </goals>",
			"</execution>"
	};
	private static final String[] EXECUTION_2 = {};

	private static final String[] CONFIGURATION_1 = {
			"<googleJavaFormat>",
			"  <version>1.2</version>",
			"</googleJavaFormat>"
	};
	private static final String[] CONFIGURATION_2 = {
			"<googleJavaFormat>",
			"  <version>1.8</version>",
			"  <reflowLongStrings>true</reflowLongStrings>",
			"</googleJavaFormat>"
	};

	private static final List<Formatter> FORMATTERS = singletonList(formatter(formatterStep("default")));

	@Test
	void sameFingerprint() throws Exception {
		String xml1 = createPomXmlContent(VERSION_1, EXECUTION_1, CONFIGURATION_1);
		String xml2 = createPomXmlContent(VERSION_1, EXECUTION_1, CONFIGURATION_1);

		MavenProject project1 = mavenProject(xml1);
		MavenProject project2 = mavenProject(xml2);

		PluginFingerprint fingerprint1 = PluginFingerprint.from(project1, FORMATTERS);
		PluginFingerprint fingerprint2 = PluginFingerprint.from(project2, FORMATTERS);

		assertThat(fingerprint1).isEqualTo(fingerprint2);
	}

	@Test
	void differentFingerprintForDifferentPluginVersion() throws Exception {
		String xml1 = createPomXmlContent(VERSION_1, EXECUTION_1, CONFIGURATION_1);
		String xml2 = createPomXmlContent(VERSION_2, EXECUTION_1, CONFIGURATION_1);

		MavenProject project1 = mavenProject(xml1);
		MavenProject project2 = mavenProject(xml2);

		PluginFingerprint fingerprint1 = PluginFingerprint.from(project1, FORMATTERS);
		PluginFingerprint fingerprint2 = PluginFingerprint.from(project2, FORMATTERS);

		assertThat(fingerprint1).isNotEqualTo(fingerprint2);
	}

	@Test
	void differentFingerprintForDifferentExecution() throws Exception {
		String xml1 = createPomXmlContent(VERSION_2, EXECUTION_1, CONFIGURATION_1);
		String xml2 = createPomXmlContent(VERSION_2, EXECUTION_2, CONFIGURATION_1);

		MavenProject project1 = mavenProject(xml1);
		MavenProject project2 = mavenProject(xml2);

		PluginFingerprint fingerprint1 = PluginFingerprint.from(project1, FORMATTERS);
		PluginFingerprint fingerprint2 = PluginFingerprint.from(project2, FORMATTERS);

		assertThat(fingerprint1).isNotEqualTo(fingerprint2);
	}

	@Test
	void differentFingerprintForDifferentConfiguration() throws Exception {
		String xml1 = createPomXmlContent(VERSION_1, EXECUTION_2, CONFIGURATION_2);
		String xml2 = createPomXmlContent(VERSION_1, EXECUTION_2, CONFIGURATION_1);

		MavenProject project1 = mavenProject(xml1);
		MavenProject project2 = mavenProject(xml2);

		PluginFingerprint fingerprint1 = PluginFingerprint.from(project1, FORMATTERS);
		PluginFingerprint fingerprint2 = PluginFingerprint.from(project2, FORMATTERS);

		assertThat(fingerprint1).isNotEqualTo(fingerprint2);
	}

	@Test
	void differentFingerprintForFormattersWithDifferentSteps() throws Exception {
		String xml1 = createPomXmlContent(VERSION_1, EXECUTION_1, CONFIGURATION_1);
		String xml2 = createPomXmlContent(VERSION_1, EXECUTION_1, CONFIGURATION_1);

		MavenProject project1 = mavenProject(xml1);
		MavenProject project2 = mavenProject(xml2);

		FormatterStep step1 = formatterStep("step1");
		FormatterStep step2 = formatterStep("step2");
		FormatterStep step3 = formatterStep("step3");
		List<Formatter> formatters1 = singletonList(formatter(step1, step2));
		List<Formatter> formatters2 = singletonList(formatter(step2, step3));

		PluginFingerprint fingerprint1 = PluginFingerprint.from(project1, formatters1);
		PluginFingerprint fingerprint2 = PluginFingerprint.from(project2, formatters2);

		assertThat(fingerprint1).isNotEqualTo(fingerprint2);
	}

	@Test
	void differentFingerprintForFormattersWithDifferentLineEndings() throws Exception {
		String xml1 = createPomXmlContent(VERSION_1, EXECUTION_1, CONFIGURATION_1);
		String xml2 = createPomXmlContent(VERSION_1, EXECUTION_1, CONFIGURATION_1);

		MavenProject project1 = mavenProject(xml1);
		MavenProject project2 = mavenProject(xml2);

		FormatterStep step = formatterStep("step");
		List<Formatter> formatters1 = singletonList(formatter(LineEnding.UNIX, step));
		List<Formatter> formatters2 = singletonList(formatter(LineEnding.WINDOWS, step));

		PluginFingerprint fingerprint1 = PluginFingerprint.from(project1, formatters1);
		PluginFingerprint fingerprint2 = PluginFingerprint.from(project2, formatters2);

		assertThat(fingerprint1).isNotEqualTo(fingerprint2);
	}

	@Test
	void emptyFingerprint() {
		PluginFingerprint fingerprint = PluginFingerprint.empty();

		assertThat(fingerprint.value()).isEmpty();
	}

	private static MavenProject mavenProject(String xml) throws Exception {
		return new MavenProject(readPom(xml));
	}

	private static Model readPom(String xml) throws Exception {
		byte[] bytes = xml.getBytes(UTF_8);
		try (XmlStreamReader xmlReader = ReaderFactory.newXmlReader(new ByteArrayInputStream(bytes))) {
			MavenXpp3Reader pomReader = new MavenXpp3Reader();
			return pomReader.read(xmlReader);
		}
	}

	private static FormatterStep formatterStep(String name) {
		return FormatterStep.createNeverUpToDate(name, input -> input);
	}

	private static Formatter formatter(FormatterStep... steps) {
		return formatter(LineEnding.UNIX, steps);
	}

	private static Formatter formatter(LineEnding lineEnding, FormatterStep... steps) {
		return Formatter.builder()
				.rootDir(Paths.get(""))
				.lineEndingsPolicy(lineEnding.createPolicy())
				.encoding(UTF_8)
				.steps(Arrays.asList(steps))
				.exceptionPolicy(new FormatExceptionPolicyStrict())
				.build();
	}
}
