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

import java.io.IOException;

import org.junit.jupiter.api.Test;

class IncludesExcludesTest extends MavenIntegrationHarness {

	private static final String JAVA_FORMATTED = "java/eclipse/JavaCodeFormatted.test";
	private static final String JAVA_UNFORMATTED = "java/eclipse/JavaCodeUnformatted.test";
	private static final String SCALA_UNFORMATTED = "scala/scalafmt/basic.dirty";
	private static final String SCALA_FORMATTED = "scala/scalafmt/basic.clean_3.0.0";

	@Test
	void testDefaultIncludesJava() throws Exception {
		String unformattedCorrectLocation1 = "src/main/java/test1.java";
		String unformattedCorrectLocation2 = "src/main/java/test2.java";
		String unformattedCorrectLocation3 = "src/test/java/test3.java";
		String unformattedCorrectLocation4 = "src/test/java/test4.java";
		String formattedCorrectLocation = "src/main/java/test5.java";
		String unformattedIncorrectLocation1 = "src/main/my-java/test6.java";
		String unformattedIncorrectLocation2 = "sources/main/java/test7.java";

		writePomWithJavaSteps(
				"<eclipse>",
				"  <file>${basedir}/formatter.xml</file>",
				"</eclipse>");

		setFile("formatter.xml").toResource("java/eclipse/formatter.xml");

		writeUnformattedJava(unformattedCorrectLocation1);
		writeUnformattedJava(unformattedCorrectLocation2);
		writeUnformattedJava(unformattedCorrectLocation3);
		writeUnformattedJava(unformattedCorrectLocation4);
		writeFormattedJava(formattedCorrectLocation);
		writeUnformattedJava(unformattedIncorrectLocation1);
		writeUnformattedJava(unformattedIncorrectLocation2);

		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFormattedJava(unformattedCorrectLocation1);
		assertFormattedJava(unformattedCorrectLocation2);
		assertFormattedJava(unformattedCorrectLocation3);
		assertFormattedJava(unformattedCorrectLocation4);
		assertFormattedJava(formattedCorrectLocation);
		assertUnformattedJava(unformattedIncorrectLocation1);
		assertUnformattedJava(unformattedIncorrectLocation2);
	}

	@Test
	void testDefaultIncludesScala() throws Exception {
		String unformattedCorrectLocation1 = "src/main/scala/test1.scala";
		String unformattedCorrectLocation2 = "src/main/scala/test2.sc";
		String unformattedCorrectLocation3 = "src/test/scala/test3.sc";
		String unformattedCorrectLocation4 = "src/test/scala/test4.scala";
		String formattedCorrectLocation = "src/test/scala/test5.scala";
		String unformattedIncorrectLocation1 = "src/main/not-scala/test6.sc";
		String unformattedIncorrectLocation2 = "scala/scala/scala/test7.scala";

		writePomWithScalaSteps("<scalafmt/>");

		writeUnformattedScala(unformattedCorrectLocation1);
		writeUnformattedScala(unformattedCorrectLocation2);
		writeUnformattedScala(unformattedCorrectLocation3);
		writeUnformattedScala(unformattedCorrectLocation4);
		writeFormattedScala(formattedCorrectLocation);
		writeUnformattedScala(unformattedIncorrectLocation1);
		writeUnformattedScala(unformattedIncorrectLocation2);

		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFormattedScala(unformattedCorrectLocation1);
		assertFormattedScala(unformattedCorrectLocation2);
		assertFormattedScala(unformattedCorrectLocation3);
		assertFormattedScala(unformattedCorrectLocation4);
		assertFormattedScala(formattedCorrectLocation);
		assertUnformattedScala(unformattedIncorrectLocation1);
		assertUnformattedScala(unformattedIncorrectLocation2);
	}

	@Test
	void testInclude() throws Exception {
		String unformattedDefaultLocation1 = "src/main/scala/test1.scala";
		String unformattedDefaultLocation2 = "src/test/scala/test2.scala";
		String unformattedCustomLocation1 = "src/main/my-scala/test3.scala";
		String unformattedCustomLocation2 = "src/test/sc/test4.sc";

		writePomWithScalaSteps(
				"<includes>",
				"  <include>src/**/my-scala/*.scala</include>",
				"  <include>src/test/sc/*.sc</include>",
				"</includes>",
				"<scalafmt/>");

		writeUnformattedScala(unformattedDefaultLocation1);
		writeUnformattedScala(unformattedDefaultLocation2);
		writeUnformattedScala(unformattedCustomLocation1);
		writeUnformattedScala(unformattedCustomLocation2);

		mavenRunner().withArguments("spotless:apply").runNoError();

		// includes override default ones, so files in default location should remain unformatted
		assertUnformattedScala(unformattedDefaultLocation1);
		assertUnformattedScala(unformattedDefaultLocation2);
		// files included via "<include>" should be formatted
		assertFormattedScala(unformattedCustomLocation1);
		assertFormattedScala(unformattedCustomLocation2);
	}

	@Test
	void testExclude() throws Exception {
		String unformatted1 = "src/main/scala/test1.scala";
		String unformatted2 = "src/main/scala/test2.sc";
		String unformatted3 = "src/test/scala/test3.scala";
		String unformatted4 = "src/test/scala/test4.sc";

		writePomWithScalaSteps(
				"<includes>",
				"  <include>src/main/scala/*.scala</include>",
				"</includes>",
				"<excludes>",
				"  <exclude>src/main/scala/*.sc</exclude>",
				"  <exclude>src/test/scala</exclude>",
				"</excludes>",
				"<scalafmt/>");

		writeUnformattedScala(unformatted1);
		writeUnformattedScala(unformatted2);
		writeUnformattedScala(unformatted3);
		writeUnformattedScala(unformatted4);

		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFormattedScala(unformatted1);
		assertUnformattedScala(unformatted2);
		assertUnformattedScala(unformatted3);
		assertUnformattedScala(unformatted4);
	}

	private void writeFormattedJava(String target) throws IOException {
		setFile(target).toResource(JAVA_FORMATTED);
	}

	private void writeUnformattedJava(String target) throws IOException {
		setFile(target).toResource(JAVA_UNFORMATTED);
	}

	private void assertFormattedJava(String target) throws IOException {
		assertFile(target).sameAsResource(JAVA_FORMATTED);
	}

	private void assertUnformattedJava(String target) throws IOException {
		assertFile(target).sameAsResource(JAVA_UNFORMATTED);
	}

	private void writeFormattedScala(String target) throws IOException {
		setFile(target).toResource(SCALA_FORMATTED);
	}

	private void writeUnformattedScala(String target) throws IOException {
		setFile(target).toResource(SCALA_UNFORMATTED);
	}

	private void assertFormattedScala(String target) throws IOException {
		assertFile(target).sameAsResource(SCALA_FORMATTED);
	}

	private void assertUnformattedScala(String target) throws IOException {
		assertFile(target).sameAsResource(SCALA_UNFORMATTED);
	}
}
