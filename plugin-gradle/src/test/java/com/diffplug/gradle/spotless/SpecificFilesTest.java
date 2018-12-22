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

public class SpecificFilesTest extends GradleIntegrationTest {
	private String testFile(int number, boolean absolute) throws IOException {
		String rel = "src/main/java/test" + number + ".java";
		if (absolute) {
			return rootFolder() + "/" + rel;
		} else {
			return rel;
		}
	}

	private String testFile(int number) throws IOException {
		return testFile(number, false);
	}

	private String fixture(boolean formatted) {
		return "java/googlejavaformat/JavaCode" + (formatted ? "F" : "Unf") + "ormatted.test";
	}

	private void integration(String patterns, boolean firstFormatted, boolean secondFormatted, boolean thirdFormatted)
			throws IOException {

		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"apply plugin: 'java'",
				"spotless {",
				"    java {",
				"        googleJavaFormat('1.2')",
				"    }",
				"}");

		setFile(testFile(1)).toResource(fixture(false));
		setFile(testFile(2)).toResource(fixture(false));
		setFile(testFile(3)).toResource(fixture(false));

		gradleRunner()
				.withArguments("spotlessApply", "-PspotlessFiles=" + patterns)
				.build();

		assertFile(testFile(1)).sameAsResource(fixture(firstFormatted));
		assertFile(testFile(2)).sameAsResource(fixture(secondFormatted));
		assertFile(testFile(3)).sameAsResource(fixture(thirdFormatted));
	}

	@Test
	public void singleFile() throws IOException {
		integration(testFile(2, true), false, true, false);
	}

	@Test
	public void multiFile() throws IOException {
		integration(testFile(1, true) + "," + testFile(3, true), true, false, true);
	}

	@Test
	public void regexp() throws IOException {
		integration(".*/src/main/java/test(1|3).java", true, false, true);
	}
}
