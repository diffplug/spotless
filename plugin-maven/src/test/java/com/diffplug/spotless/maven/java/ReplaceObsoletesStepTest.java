/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless.maven.java;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class ReplaceObsoletesStepTest extends MavenIntegrationHarness {
	@Test
	void testSortPomCfg() throws Exception {
		writePomWithJavaSteps("<replaceObsoletes/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/replaceobsoletes/SortPomCfgPre.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("java/replaceobsoletes/SortPomCfgPost.test");
	}

	@Test
	void testSystemLineSeparator() throws Exception {
		writePomWithJavaSteps("<replaceObsoletes/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/replaceobsoletes/SystemLineSeparatorPre.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("java/replaceobsoletes/SystemLineSeparatorPost.test");
	}

	@Test
	void testBooleanInitializers() throws Exception {
		writePomWithJavaSteps("<replaceObsoletes/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/replaceobsoletes/BooleanInitializersPre.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("java/replaceobsoletes/BooleanInitializersPost.test");
	}

	@Test
	void testNullInitializers() throws Exception {
		writePomWithJavaSteps("<replaceObsoletes/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/replaceobsoletes/NullInitializersPre.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("java/replaceobsoletes/NullInitializersPost.test");
	}

	@Test
	void testIntInitializers() throws Exception {
		writePomWithJavaSteps("<replaceObsoletes/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/replaceobsoletes/IntInitializersPre.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("java/replaceobsoletes/IntInitializersPost.test");
	}

	@Test
	void testEnumPublicStatic() throws Exception {
		writePomWithJavaSteps("<replaceObsoletes/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/replaceobsoletes/EnumPublicStaticPre.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("java/replaceobsoletes/EnumPublicStaticPost.test");
	}

	@Test
	void testInterfacePublicStatic() throws Exception {
		writePomWithJavaSteps("<replaceObsoletes/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/replaceobsoletes/InterfacePublicStaticPre.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("java/replaceobsoletes/InterfacePublicStaticPost.test");
	}

	@Test
	@Disabled("feature envy: (edge case/hard to implement) having a dedicated method")
	void testSQLTokenizedFormatterPost() throws Exception {
		writePomWithJavaSteps("<replaceObsoletes/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/replaceobsoletes/SQLTokenizedFormatterPre.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("java/replaceobsoletes/SQLTokenizedFormatterPost.test");
	}

}
