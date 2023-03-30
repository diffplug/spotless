/*
 * Copyright 2016-2023 DiffPlug
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MultiModuleProjectTest extends MavenIntegrationHarness {

	@Test
	void testConfigurationDependency() throws Exception {
		/*
		create a multi-module project with the following structure:

		    /junit-tmp-dir
		    ├── config
		    │   ├── pom.xml
		    │   └── src/main/resources/configs
		    │       ├── eclipse-formatter.xml
		    │       └── scalafmt.conf
		    ├── mvnw
		    ├── mvnw.cmd
		    ├── one
		    │   ├── pom.xml
		    │   └── src
		    │       ├── main/java/test1.java
		    │       └── test/java/test2.java
		    ├── two
		    │   ├── pom.xml
		    │   └── src
		    │       ├── main/java/test1.java
		    │       └── test/java/test2.java
		    ├── three
		    │   ├── pom.xml
		    │   └── src
		    │       ├── main/scala/test1.scala
		    │       └── test/scala/test2.scala
		    ├── pom.xml
		    ├── .mvn
		    ├── mvnw
		    └── mvnw.cmd
		 */
		multiModuleProject()
				.withConfigSubProject("config",
						file("java/eclipse/formatter.xml", "src/main/resources/configs/eclipse-formatter.xml"),
						file("scala/scalafmt/scalafmt.conf", "src/main/resources/configs/scalafmt.conf"))
				.withConfiguration(
						"<java>",
						"  <eclipse>",
						"    <file>configs/eclipse-formatter.xml</file>",
						"    <version>4.9</version>",
						"  </eclipse>",
						"</java>",
						"<scala>",
						"  <scalafmt>",
						"    <file>configs/scalafmt.conf</file>",
						"  </scalafmt>",
						"</scala>")
				.addSubProject("one",
						file("java/eclipse/JavaCodeUnformatted.test", "src/main/java/test1.java"),
						file("java/eclipse/JavaCodeUnformatted.test", "src/test/java/test2.java"))
				.addSubProject("two",
						file("java/eclipse/JavaCodeUnformatted.test", "src/main/java/test1.java"),
						file("java/eclipse/JavaCodeUnformatted.test", "src/test/java/test2.java"))
				.addSubProject("three",
						file("scala/scalafmt/basic.dirty", "src/main/scala/test1.scala"),
						file("scala/scalafmt/basic.dirty", "src/test/scala/test2.scala"))
				.create();

		// build config module that contains eclipse and scalafmt configuration files
		mavenRunner().withArguments("-f", "config", "clean", "install").runNoError();

		// format all files in the multi-module project
		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFile("one/src/main/java/test1.java").sameAsResource("java/eclipse/JavaCodeFormatted.test");
		assertFile("one/src/test/java/test2.java").sameAsResource("java/eclipse/JavaCodeFormatted.test");

		assertFile("two/src/main/java/test1.java").sameAsResource("java/eclipse/JavaCodeFormatted.test");
		assertFile("two/src/test/java/test2.java").sameAsResource("java/eclipse/JavaCodeFormatted.test");

		assertFile("three/src/main/scala/test1.scala").sameAsResource("scala/scalafmt/basic.cleanWithCustomConf_3.0.0");
		assertFile("three/src/test/scala/test2.scala").sameAsResource("scala/scalafmt/basic.cleanWithCustomConf_3.0.0");
	}

	private static final String CHILD_ID = "childId";

	protected MultiModuleProjectCreator multiModuleProject() {
		return new MultiModuleProjectCreator();
	}

	class MultiModuleProjectCreator {
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
			List<String> modulesList = new ArrayList<>();
			modulesList.add(configSubProject);
			modulesList.addAll(subProjects.keySet());
			var modules = modulesList.toArray(new String[0]);

			Map<String, Object> rootPomParams = buildPomXmlParams(null, null, null, configuration, modules, null, null);
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
				var subProjectName = entry.getKey();
				List<SubProjectFile> subProjectFiles = entry.getValue();

				String content = createPomXmlContent("/multi-module/pom-child.xml.mustache", singletonMap(CHILD_ID, subProjectName));
				setFile(subProjectName + "/pom.xml").toContent(content);

				createSubProjectFiles(subProjectName, subProjectFiles);
			}
		}

		private void createSubProjectFiles(String subProjectName, List<SubProjectFile> subProjectFiles) {
			for (SubProjectFile file : subProjectFiles) {
				setFile(subProjectName + '/' + file.to).toResource(file.from);
			}
		}
	}

	static class SubProjectFile {

		private final String from;
		private final String to;

		private SubProjectFile(String from, String to) {
			this.from = from;
			this.to = to;
		}
	}

	static SubProjectFile file(String from, String to) {
		return new SubProjectFile(from, to);
	}
}
