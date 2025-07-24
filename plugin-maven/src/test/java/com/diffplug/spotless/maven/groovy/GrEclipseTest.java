/*
 * Copyright 2020-2025 DiffPlug
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
package com.diffplug.spotless.maven.groovy;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class GrEclipseTest extends MavenIntegrationHarness {

	@Test
	void testEclipse() throws Exception {
		writePomWithGrEclipse();

		String path = "src/main/groovy/test.groovy";
		setFile(path).toResource("groovy/greclipse/format/unformatted.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("groovy/greclipse/format/formatted.test");
	}

	@Test
	void doesNotFormatJavaFiles() throws Exception {
		writePomWithGrEclipse();

		String testJavaPath = "src/test/java/test.java";
		setFile(TEST_PATH).toResource("java/googlejavaformat/JavaCodeUnformatted.test");
		setFile(testJavaPath).toResource("java/googlejavaformat/JavaCodeUnformatted.test");

		String groovyPath = "src/main/groovy/test.groovy";
		String testGroovyPath = "src/test/groovy/test.groovy";
		setFile(groovyPath).toResource("groovy/greclipse/format/unformatted.test");
		setFile(testGroovyPath).toResource("groovy/greclipse/format/unformatted.test");

		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFile(TEST_PATH).sameAsResource("java/googlejavaformat/JavaCodeUnformatted.test");
		assertFile(testJavaPath).sameAsResource("java/googlejavaformat/JavaCodeUnformatted.test");

		assertFile(groovyPath).sameAsResource("groovy/greclipse/format/formatted.test");
		assertFile(testGroovyPath).sameAsResource("groovy/greclipse/format/formatted.test");
	}

	private void writePomWithGrEclipse() throws IOException {
		writePomWithGroovySteps(
				"<greclipse>",
				"  <file>${basedir}/greclipse.properties</file>",
				"  <version>4.25</version>",
				"</greclipse>");
		setFile("greclipse.properties").toResource("groovy/greclipse/format/greclipse.properties");
	}
}
