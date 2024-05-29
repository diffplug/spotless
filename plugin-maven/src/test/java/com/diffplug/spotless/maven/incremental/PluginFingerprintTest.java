/*
 * Copyright 2021-2024 DiffPlug
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.NeverUpToDateStep;
import com.diffplug.spotless.SerializedFunction;
import com.diffplug.spotless.maven.MavenIntegrationHarness;

class PluginFingerprintTest extends MavenIntegrationHarness {

	private static final String VERSION_1 = "1.0.0";
	private static final String VERSION_2 = "2.0.0";

	private static final List<Formatter> FORMATTERS = singletonList(formatter(formatterStep("default")));

	@Test
	void sameFingerprintWhenVersionAndFormattersAreTheSame() throws Exception {
		MavenProject project = mavenProject(VERSION_1);

		PluginFingerprint fingerprint1 = PluginFingerprint.from(project, FORMATTERS);
		PluginFingerprint fingerprint2 = PluginFingerprint.from(project, FORMATTERS);

		assertThat(fingerprint1).isEqualTo(fingerprint2);
	}

	@Test
	void differentFingerprintForDifferentPluginVersions() throws Exception {
		MavenProject project1 = mavenProject(VERSION_1);
		MavenProject project2 = mavenProject(VERSION_2);

		PluginFingerprint fingerprint1 = PluginFingerprint.from(project1, FORMATTERS);
		PluginFingerprint fingerprint2 = PluginFingerprint.from(project2, FORMATTERS);

		assertThat(fingerprint1).isNotEqualTo(fingerprint2);
	}

	@Test
	void differentFingerprintForFormattersWithDifferentSteps() throws Exception {
		MavenProject project1 = mavenProject(VERSION_1);
		MavenProject project2 = mavenProject(VERSION_1);

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
		MavenProject project1 = mavenProject(VERSION_1);
		MavenProject project2 = mavenProject(VERSION_1);

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

	@Test
	void failsForProjectWithoutSpotlessPlugin() {
		MavenProject projectWithoutSpotless = new MavenProject();

		assertThatThrownBy(() -> PluginFingerprint.from(projectWithoutSpotless, FORMATTERS))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Spotless plugin absent from the project");
	}

	@Test
	void buildsFingerprintForProjectWithSpotlessPluginInBuildPlugins() {
		MavenProject project = new MavenProject();
		Plugin spotlessPlugin = new Plugin();
		spotlessPlugin.setGroupId("com.diffplug.spotless");
		spotlessPlugin.setArtifactId("spotless-maven-plugin");
		spotlessPlugin.setVersion("1.2.3");
		project.getBuild().addPlugin(spotlessPlugin);

		PluginFingerprint fingerprint = PluginFingerprint.from(project, Collections.emptyList());

		assertThat(fingerprint).isNotNull();
	}

	@Test
	void buildsFingerprintForProjectWithSpotlessPluginInPluginManagement() {
		MavenProject project = new MavenProject();
		Plugin spotlessPlugin = new Plugin();
		spotlessPlugin.setGroupId("com.diffplug.spotless");
		spotlessPlugin.setArtifactId("spotless-maven-plugin");
		spotlessPlugin.setVersion("1.2.3");
		project.getBuild().addPlugin(spotlessPlugin);
		PluginManagement pluginManagement = new PluginManagement();
		pluginManagement.addPlugin(spotlessPlugin);
		project.getBuild().setPluginManagement(pluginManagement);

		PluginFingerprint fingerprint = PluginFingerprint.from(project, Collections.emptyList());

		assertThat(fingerprint).isNotNull();
	}

	private MavenProject mavenProject(String spotlessVersion) throws Exception {
		String xml = createPomXmlContent(spotlessVersion, new String[0], new String[0]);
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
		return NeverUpToDateStep.create(name, SerializedFunction.identity());
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
