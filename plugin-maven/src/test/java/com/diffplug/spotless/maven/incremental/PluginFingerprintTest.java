/*
 * Copyright 2021 DiffPlug
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.ByteArrayInputStream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.junit.jupiter.api.Test;

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

	@Test
	void sameFingerprint() throws Exception {
		String xml1 = createPomXmlContent(VERSION_1, EXECUTION_1, CONFIGURATION_1);
		String xml2 = createPomXmlContent(VERSION_1, EXECUTION_1, CONFIGURATION_1);

		MavenProject project1 = mavenProject(xml1);
		MavenProject project2 = mavenProject(xml2);

		PluginFingerprint fingerprint1 = PluginFingerprint.from(project1);
		PluginFingerprint fingerprint2 = PluginFingerprint.from(project2);

		assertEquals(fingerprint1, fingerprint2);
	}

	@Test
	void differentFingerprintForDifferentPluginVersion() throws Exception {
		String xml1 = createPomXmlContent(VERSION_1, EXECUTION_1, CONFIGURATION_1);
		String xml2 = createPomXmlContent(VERSION_2, EXECUTION_1, CONFIGURATION_1);

		MavenProject project1 = mavenProject(xml1);
		MavenProject project2 = mavenProject(xml2);

		PluginFingerprint fingerprint1 = PluginFingerprint.from(project1);
		PluginFingerprint fingerprint2 = PluginFingerprint.from(project2);

		assertNotEquals(fingerprint1, fingerprint2);
	}

	@Test
	void differentFingerprintForDifferentExecution() throws Exception {
		String xml1 = createPomXmlContent(VERSION_2, EXECUTION_1, CONFIGURATION_1);
		String xml2 = createPomXmlContent(VERSION_2, EXECUTION_2, CONFIGURATION_1);

		MavenProject project1 = mavenProject(xml1);
		MavenProject project2 = mavenProject(xml2);

		PluginFingerprint fingerprint1 = PluginFingerprint.from(project1);
		PluginFingerprint fingerprint2 = PluginFingerprint.from(project2);

		assertNotEquals(fingerprint1, fingerprint2);
	}

	@Test
	void differentFingerprintForDifferentConfiguration() throws Exception {
		String xml1 = createPomXmlContent(VERSION_1, EXECUTION_2, CONFIGURATION_2);
		String xml2 = createPomXmlContent(VERSION_1, EXECUTION_2, CONFIGURATION_1);

		MavenProject project1 = mavenProject(xml1);
		MavenProject project2 = mavenProject(xml2);

		PluginFingerprint fingerprint1 = PluginFingerprint.from(project1);
		PluginFingerprint fingerprint2 = PluginFingerprint.from(project2);

		assertNotEquals(fingerprint1, fingerprint2);
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
}
