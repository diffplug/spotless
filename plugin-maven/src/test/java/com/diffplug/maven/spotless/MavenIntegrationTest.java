/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.maven.spotless;

import static com.diffplug.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import com.diffplug.spotless.ResourceHarness;

public class MavenIntegrationTest extends ResourceHarness {

	private static final String LOCAL_MAVEN_REPOSITORY_DIR = "localMavenRepositoryDir";
	private static final String SPOTLESS_MAVEN_PLUGIN_VERSION = "spotlessMavenPluginVersion";
	private static final String JAVA_CONFIGURATION = "javaConfiguration";

	private final MustacheFactory mustacheFactory = new DefaultMustacheFactory();

	/**
	 * Each test gets its own temp folder, and we create a maven
	 * build there and run it.
	 *
	 * Because those test folders don't have a .gitattributes file,
	 * git on windows will default to \r\n. So now if you read a
	 * test file from the spotless test resources, and compare it
	 * to a build result, the line endings won't match.
	 *
	 * By sticking this .gitattributes file into the test directory,
	 * we ensure that the default Spotless line endings policy of
	 * GIT_ATTRIBUTES will use \n, so that tests match the test
	 * resources on win and linux.
	 */
	@Before
	public void gitAttributes() throws IOException {
		write(".gitattributes", "* text eol=lf");
	}

	protected void writePomWithJavaSteps(String... steps) throws IOException {
		String pomXmlContent = createPomXmlContent(steps);
		write("pom.xml", pomXmlContent);
	}

	protected MavenRunner mavenRunner() throws IOException {
		return MavenRunner.create()
				.withProjectDir(rootFolder());
	}

	private String createPomXmlContent(String... steps) throws IOException {
		Path pomXml = Paths.get("src", "test", "resources", "pom.xml.mustache");

		try (BufferedReader reader = Files.newBufferedReader(pomXml)) {
			Mustache mustache = mustacheFactory.compile(reader, "pom");
			StringWriter writer = new StringWriter();
			Map<String, String> params = buildPomXmlParams(steps);
			mustache.execute(writer, params);
			return writer.toString();
		}
	}

	private static Map<String, String> buildPomXmlParams(String... steps) {
		Map<String, String> params = new HashMap<>();
		params.put(LOCAL_MAVEN_REPOSITORY_DIR, getSystemProperty(LOCAL_MAVEN_REPOSITORY_DIR));
		params.put(SPOTLESS_MAVEN_PLUGIN_VERSION, getSystemProperty(SPOTLESS_MAVEN_PLUGIN_VERSION));

		String stepsXml = Arrays.stream(steps).collect(joining("\n", "<steps>\n", "\n</steps>"));
		params.put(JAVA_CONFIGURATION, stepsXml);

		return params;
	}

	private static String getSystemProperty(String name) {
		String value = System.getProperty(name);
		if (isNullOrEmpty(value)) {
			fail("System property '" + name + "' is not defined");
		}
		return value;
	}
}
