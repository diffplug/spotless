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
package com.diffplug.spotless.maven;

import static com.diffplug.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.junit.Before;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import com.diffplug.common.io.Resources;
import com.diffplug.spotless.ResourceHarness;

public class MavenIntegrationTest extends ResourceHarness {

	private static final String LOCAL_MAVEN_REPOSITORY_DIR = "localMavenRepositoryDir";
	private static final String SPOTLESS_MAVEN_PLUGIN_VERSION = "spotlessMavenPluginVersion";
	private static final String CONFIGURATION = "configuration";
	private static final String EXECUTIONS = "executions";
	private static final String MODULES = "modules";
	private static final String MODULE_NAME = "name";
	private static final String CHILD_ID = "childId";

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
		setFile(".gitattributes").toContent("* text eol=lf");
		// copy the mvnw resources
		copy("mvnw").setExecutable(true);
		copy("mvnw.cmd");
		copy(".mvn/wrapper/maven-wrapper.jar");
		copy(".mvn/wrapper/maven-wrapper.properties");
	}

	private File copy(String path) throws IOException {
		byte[] bytes = Resources.toByteArray(ResourceHarness.class.getResource("/" + path));
		Path target = newFile(path).toPath();
		Files.createDirectories(target.getParent());
		Files.write(target, bytes);
		return target.toFile();
	}

	protected void writePomWithFormatSteps(String... steps) throws IOException {
		writePom(formats(groupWithSteps("format", including("<include>src/**/java/**/*.java</include>"), steps)));
	}

	protected void writePomWithJavaSteps(String... steps) throws IOException {
		writePom(groupWithSteps("java", steps));
	}

	protected void writePomWithScalaSteps(String... steps) throws IOException {
		writePom(groupWithSteps("scala", steps));
	}

	protected void writePomWithKotlinSteps(String... steps) throws IOException {
		writePom(groupWithSteps("kotlin", steps));
	}

	protected void writePom(String... configuration) throws IOException {
		writePom(null, configuration);
	}

	protected void writePom(String[] executions, String[] configuration) throws IOException {
		String pomXmlContent = createPomXmlContent(executions, configuration);
		setFile("pom.xml").toContent(pomXmlContent);
	}

	protected MavenRunner mavenRunner() throws IOException {
		return MavenRunner.create()
				.withProjectDir(rootFolder())
				.withLocalRepository(new File(getSystemProperty(LOCAL_MAVEN_REPOSITORY_DIR)));
	}

	protected MultiModuleProjectCreator multiModuleProject() {
		return new MultiModuleProjectCreator();
	}

	private String createPomXmlContent(String[] executions, String[] configuration) throws IOException {
		Map<String, Object> params = buildPomXmlParams(executions, configuration, null);
		return createPomXmlContent("/pom-test.xml.mustache", params);
	}

	private String createPomXmlContent(String pomTemplate, Map<String, Object> params) throws IOException {
		URL url = MavenIntegrationTest.class.getResource(pomTemplate);
		try (BufferedReader reader = Resources.asCharSource(url, StandardCharsets.UTF_8).openBufferedStream()) {
			Mustache mustache = mustacheFactory.compile(reader, "pom");
			StringWriter writer = new StringWriter();
			mustache.execute(writer, params);
			return writer.toString();
		}
	}

	private static Map<String, Object> buildPomXmlParams(String[] executions, String[] configuration, String[] modules) {
		Map<String, Object> params = new HashMap<>();
		params.put(SPOTLESS_MAVEN_PLUGIN_VERSION, getSystemProperty(SPOTLESS_MAVEN_PLUGIN_VERSION));

		if (configuration != null) {
			params.put(CONFIGURATION, String.join("\n", configuration));
		}

		if (executions != null) {
			params.put(EXECUTIONS, String.join("\n", executions));
		}

		if (modules != null) {
			List<Map<String, String>> moduleNames = stream(modules).map(name -> singletonMap(MODULE_NAME, name)).collect(toList());
			params.put(MODULES, moduleNames);
		}

		return params;
	}

	private static String getSystemProperty(String name) {
		String value = System.getProperty(name);
		if (isNullOrEmpty(value)) {
			fail("System property '" + name + "' is not defined");
		}
		return value;
	}

	private static String[] groupWithSteps(String group, String[] includes, String... steps) {
		String[] result = new String[steps.length + includes.length + 2];
		result[0] = "<" + group + ">";
		System.arraycopy(includes, 0, result, 1, includes.length);
		System.arraycopy(steps, 0, result, includes.length + 1, steps.length);
		result[result.length - 1] = "</" + group + ">";
		return result;
	}

	private static String[] groupWithSteps(String group, String... steps) {
		return groupWithSteps(group, new String[]{}, steps);
	}

	private static String[] including(String... includes) {
		return groupWithSteps("includes", includes);
	}

	private static String[] formats(String... formats) {
		return groupWithSteps("formats", formats);
	}

	protected class MultiModuleProjectCreator {

		private String configSubProject;
		private SubProjectFile[] configSubProjectFiles;
		private String[] configuration;
		private final Map<String, List<SubProjectFile>> subProjects = new LinkedHashMap<>();

		protected MultiModuleProjectCreator withConfigSubProject(String name, SubProjectFile... files) {
			configSubProject = name;
			configSubProjectFiles = files;
			return this;
		}

		protected MultiModuleProjectCreator withConfiguration(String... lines) {
			configuration = lines;
			return this;
		}

		protected MultiModuleProjectCreator addSubProject(String name, SubProjectFile... files) {
			subProjects.put(name, asList(files));
			return this;
		}

		protected void create() throws IOException {
			createRootPom();
			createConfigSubProject();
			createSubProjects();
		}

		private void createRootPom() throws IOException {
			List<Object> modulesList = new ArrayList<>();
			modulesList.add(configSubProject);
			modulesList.addAll(subProjects.keySet());
			String[] modules = modulesList.toArray(new String[0]);

			Map<String, Object> rootPomParams = buildPomXmlParams(null, configuration, modules);
			setFile("pom.xml").toContent(createPomXmlContent("/multi-module/pom-parent.xml.mustache", rootPomParams));
		}

		private void createConfigSubProject() throws IOException {
			if (configSubProject != null) {
				String content = createPomXmlContent("/multi-module/pom-config.xml.mustache", emptyMap());
				setFile(configSubProject + "/pom.xml").toContent(content);

				createSubProjectFiles(configSubProject, asList(configSubProjectFiles));
			}
		}

		private void createSubProjects() throws IOException {
			for (Map.Entry<String, List<SubProjectFile>> entry : subProjects.entrySet()) {
				String subProjectName = entry.getKey();
				List<SubProjectFile> subProjectFiles = entry.getValue();

				String content = createPomXmlContent("/multi-module/pom-child.xml.mustache", singletonMap(CHILD_ID, subProjectName));
				setFile(subProjectName + "/pom.xml").toContent(content);

				createSubProjectFiles(subProjectName, subProjectFiles);
			}
		}

		private void createSubProjectFiles(String subProjectName, List<SubProjectFile> subProjectFiles) throws IOException {
			for (SubProjectFile file : subProjectFiles) {
				setFile(subProjectName + '/' + file.to).toResource(file.from);
			}
		}
	}

	protected static class SubProjectFile {

		private final String from;
		private final String to;

		private SubProjectFile(String from, String to) {
			this.from = from;
			this.to = to;
		}

		protected static SubProjectFile file(String from, String to) {
			return new SubProjectFile(from, to);
		}
	}
}
