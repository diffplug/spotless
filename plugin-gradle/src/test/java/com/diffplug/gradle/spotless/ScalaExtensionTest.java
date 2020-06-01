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
package com.diffplug.gradle.spotless;

import java.io.IOException;

import org.junit.Test;

public class ScalaExtensionTest extends GradleIntegrationTest {
	@Test
	public void integrationScalafmt() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"    id 'scala'",
				"}",
				"repositories { mavenCentral() }",
				"dependencies {",
				"    implementation 'org.scala-lang:scala-library:2.13.2'",
				"}",
				"spotless {",
				"    scala {",
				"        scalafmt().configFile('scalafmt.conf')",
				"    }",
				"}");
		setFile("scalafmt.conf").toResource("scala/scalafmt/scalafmt.conf");
		setFile("src/main/scala/basic.scala").toResource("scala/scalafmt/basic.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/scala/basic.scala").sameAsResource("scala/scalafmt/basic.cleanWithCustomConf_2.0.1");
	}

	@Test
	public void integrationScalafix() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"    id 'scala'",
				"}",
				"repositories { mavenCentral() }",
				"dependencies {",
				"    implementation 'org.scala-lang:scala-library:2.12.11'",
				"}",
				"spotless {",
				"    scala {",
				"        scalafix()",
				"    }",
				"}");
		setFile(".scalafix.conf").toResource("scala/scalafix/integration/.scalafix.conf");
		setFile("src/main/scala/basic.scala").toResource("scala/scalafix/integration/basic.dirty");
		// Scalafix uses "user.dir" as the working directory
		final String originalDir = System.getProperty("user.dir");
		System.setProperty("user.dir", rootFolder().toString());
		gradleRunner().withArguments("spotlessApply").build();
		System.setProperty("user.dir", originalDir);
		assertFile("src/main/scala/basic.scala").sameAsResource("scala/scalafix/integration/basic.clean");
	}

	@Test
	public void integrationScalafix_0_9_1() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"    id 'scala'",
				"}",
				"repositories { mavenCentral() }",
				"dependencies {",
				"    implementation 'org.scala-lang:scala-library:2.12.11'",
				"}",
				"spotless {",
				"    scala {",
				"        scalafix('0.9.1', '2.11.12')",
				"    }",
				"}");
		setFile(".scalafix.conf").toResource("scala/scalafix/integration/.scalafix.conf");
		setFile("src/main/scala/basic.scala").toResource("scala/scalafix/integration/basic.dirty");
		final String originalDir = System.getProperty("user.dir");
		// Scalafix uses "user.dir" as the working directory
		System.setProperty("user.dir", rootFolder().toString());
		gradleRunner().withArguments("spotlessApply").build();
		System.setProperty("user.dir", originalDir);
		assertFile("src/main/scala/basic.scala").sameAsResource("scala/scalafix/integration/basic.clean");
	}

	@Test
	public void integrationScalafixTestdir() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"    id 'scala'",
				"}",
				"repositories { mavenCentral() }",
				"dependencies {",
				"    implementation 'org.scala-lang:scala-library:2.12.11'",
				"}",
				"spotless {",
				"    scala {",
				"        scalafix()",
				"    }",
				"}");
		setFile(".scalafix.conf").toResource("scala/scalafix/integration/.scalafix.conf");
		setFile("src/test/scala/basic.scala").toResource("scala/scalafix/integration/basic.dirty");
		// Scalafix uses "user.dir" as the working directory
		final String originalDir = System.getProperty("user.dir");
		System.setProperty("user.dir", rootFolder().toString());
		gradleRunner().withArguments("spotlessApply").build();
		System.setProperty("user.dir", originalDir);
		assertFile("src/test/scala/basic.scala").sameAsResource("scala/scalafix/integration/basic.clean");
	}
}
