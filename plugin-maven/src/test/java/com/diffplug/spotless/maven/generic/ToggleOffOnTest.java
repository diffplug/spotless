/*
 * Copyright 2016-2026 DiffPlug
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
package com.diffplug.spotless.maven.generic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class ToggleOffOnTest extends MavenIntegrationHarness {
	@Test
	void toggleOffOn() throws Exception {
		writePomWithJavaSteps(
				"<indent>",
				"  <tabs>true</tabs>",
				"  <spacesPerTab>1</spacesPerTab>",
				"</indent>",
				"<toggleOffOn />");
		setFile("src/main/java/Main.java").toLines(
				" Here is some stuff",
				" No matter",
				"//spotless:off",
				" This won't get tabbed",
				"//spotless:on",
				" But this will get tabbed.");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("src/main/java/Main.java").hasLines(
				"	Here is some stuff",
				"	No matter",
				"//spotless:off",
				" This won't get tabbed",
				"//spotless:on",
				"	But this will get tabbed.");
	}

	@Test
	void toggleOffOnCustom() throws Exception {
		writePomWithJavaSteps(
				"<indent>",
				"  <tabs>true</tabs>",
				"  <spacesPerTab>1</spacesPerTab>",
				"</indent>",
				"<toggleOffOn>",
				"  <off>//off</off>",
				"  <on>//on</on>",
				"</toggleOffOn>");
		setFile("src/main/java/Main.java").toLines(
				" Here is some stuff",
				" No matter",
				"//off",
				" This won't get tabbed",
				"//on",
				" But this will get tabbed.");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("src/main/java/Main.java").hasLines(
				"	Here is some stuff",
				"	No matter",
				"//off",
				" This won't get tabbed",
				"//on",
				"	But this will get tabbed.");
	}
	private static final String WILDCARD_LINT = "Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this";

	private void writeWildcardImports() {
		setFile("src/main/java/Main.java").toLines(
				"import java.util.*;",
				"// spotless:off",
				"import java.io.*;",
				"// spotless:on",
				"",
				"public class Main {}");
	}

	@Test
	void lintsInsideToggleAreReportedUnderTheirOwnStep() throws Exception {
		writePomWithJavaSteps("<toggleOffOn/>", "<forbidWildcardImports/>");
		writeWildcardImports();

		expectSelfieErrorMsg(mavenRunner().withArguments("spotless:check").runHasError()).toBe("""
				Failed to execute goal com.diffplug.spotless:spotless-maven-plugin:VERSION:check (default-cli) on project spotless-maven-plugin-tests: Unable to format file ${PROJECT_DIR}/src/main/java/Main.java
				Step 'forbidWildcardImports' found problem in 'Main.java':
				Main.java:L1 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this
				""");
	}

	@Test
	void lintsWithoutToggleUseTheSameStepName() throws Exception {
		writePomWithJavaSteps("<forbidWildcardImports/>");
		writeWildcardImports();

		expectSelfieErrorMsg(mavenRunner().withArguments("spotless:check").runHasError()).toBe("""
				Failed to execute goal com.diffplug.spotless:spotless-maven-plugin:VERSION:check (default-cli) on project spotless-maven-plugin-tests: Unable to format file ${PROJECT_DIR}/src/main/java/Main.java
				Step 'forbidWildcardImports' found problem in 'Main.java':
				Main.java:L1 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this
				Main.java:L3 forbidWildcardImports(import java.io.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this
				""");
	}

	@Test
	void lintsInsideToggleAreSuppressibleByTheirOwnStep() throws Exception {
		writePom(
				"<java>",
				"  <toggleOffOn/>",
				"  <forbidWildcardImports/>",
				"</java>",
				"<lintSuppressions>",
				"  <lintSuppression>",
				"    <step>forbidWildcardImports</step>",
				"  </lintSuppression>",
				"</lintSuppressions>");
		writeWildcardImports();

		mavenRunner().withArguments("spotless:check").runNoError();
	}

	@Test
	void lintsInsideToggleAreStillSuppressibleByToggleStep() throws Exception {
		writePom(
				"<java>",
				"  <toggleOffOn/>",
				"  <forbidWildcardImports/>",
				"</java>",
				"<lintSuppressions>",
				"  <lintSuppression>",
				"    <step>toggle</step>",
				"  </lintSuppression>",
				"</lintSuppressions>");
		writeWildcardImports();

		mavenRunner().withArguments("spotless:check").runNoError();
	}

	/** The configuration from https://github.com/diffplug/spotless/issues/2859 */
	@Test
	void issue2859() throws Exception {
		writePomWithJavaSteps("<toggleOffOn/>", "<removeUnusedImports/>", "<forbidWildcardImports/>");
		setFile("src/main/java/org/example/ImportMess.java").toLines(
				"package org.example;",
				"",
				"import java.sql.PreparedStatement;",
				"import java.util.*;",
				"",
				"public class ImportMess {",
				"  List<String> names = new ArrayList<>();",
				"}");

		String output = mavenRunner().withArguments("spotless:apply").runHasError().stdOutUtf8();

		assertThat(output).contains("src/main/java/org/example/ImportMess.java:L3 forbidWildcardImports(import java.util.*;) " + WILDCARD_LINT);
		assertThat(output).doesNotContain(" toggle(");
		assertFile("src/main/java/org/example/ImportMess.java").hasLines(
				"package org.example;",
				"",
				"import java.util.*;",
				"",
				"public class ImportMess {",
				"  List<String> names = new ArrayList<>();",
				"}");
	}
}
