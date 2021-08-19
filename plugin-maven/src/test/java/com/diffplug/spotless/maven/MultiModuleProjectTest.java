/*
 * Copyright 2016-2021 DiffPlug
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

import static com.diffplug.spotless.maven.MavenIntegrationHarness.SubProjectFile.file;

import org.junit.Test;

public class MultiModuleProjectTest extends MavenIntegrationHarness {

	@Test
	public void testConfigurationDependency() throws Exception {
		/*
		create a multi-module project with the following stucture:

		    /junit-tmp-dir
		    ├── config
		    │   ├── pom.xml
		    │   └── src/main/resources/configs
		    │       ├── eclipse-formatter.xml
		    │       └── scalafmt.conf
		    ├── mvnw
		    ├── mvnw.cmd
		    ├── one
		    │   ├── pom.xml
		    │   └── src
		    │       ├── main/java/test1.java
		    │       └── test/java/test2.java
		    ├── two
		    │   ├── pom.xml
		    │   └── src
		    │       ├── main/java/test1.java
		    │       └── test/java/test2.java
		    ├── three
		    │   ├── pom.xml
		    │   └── src
		    │       ├── main/scala/test1.scala
		    │       └── test/scala/test2.scala
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
						"    <version>4.7.1</version>",
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
}
