/*
 * Copyright 2026 DiffPlug
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

import static org.junit.jupiter.api.condition.JRE.JAVA_21;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

@EnabledForJreRange(min = JAVA_21) // open-java-format is compiled for Java 21
class OpenJavaFormatTest extends MavenIntegrationHarness {
	@Test
	void specificVersionDefaultStyle() throws Exception {
		writePomWithJavaSteps(
				"<openJavaFormat>",
				"  <version>2.98.0.1</version>",
				"</openJavaFormat>");

		runTest("java/palantirjavaformat/JavaCodeFormatted.test", "java/palantirjavaformat/JavaCodeUnformatted.test");
	}

	@Test
	void formatJavaDoc() throws Exception {
		writePomWithJavaSteps(
				"<openJavaFormat>",
				"  <version>2.98.0.1</version>",
				"  <formatJavadoc>true</formatJavadoc>",
				"</openJavaFormat>");

		runTest("java/palantirjavaformat/JavaCodeWithJavaDocFormatted.test", "java/palantirjavaformat/JavaCodeWithJavaDocUnformatted.test");
	}

	private void runTest(String targetResource, String sourceResource) throws Exception {
		String path = "src/main/java/test.java";
		setFile(path).toResource(sourceResource);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource(targetResource);
	}
}
