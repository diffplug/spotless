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
package com.diffplug.spotless.maven.generic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.diffplug.spotless.maven.MavenIntegrationTest;

public class LineEndingsTest extends MavenIntegrationTest {

	@Test
	public void fromContentToWindows() throws Exception {
		writePomWithFormatSteps(
				"<lineEndings>WINDOWS</lineEndings>");
		runToWindowsTest();
	}

	@Test
	public void fromContentToUnix() throws Exception {
		writePomWithFormatSteps(
				"<lineEndings>UNIX</lineEndings>");
		runToUnixTest();
	}

	private void runToWindowsTest() throws Exception {
		runTest("lineEndings/JavaCode-UNIX.test", "lineEndings/JavaCode-WINDOWS.test");
	}

	private void runToUnixTest() throws Exception {
		runTest("lineEndings/JavaCode-WINDOWS.test", "lineEndings/JavaCode-UNIX.test");
	}

	private void runTest(String source, String target) throws Exception {
		setFile("src/main/java/test.java").toResource(source);
		mavenRunner().withArguments("spotless:apply").runNoError();
		String actual = read("src/main/java/test.java");
		assertThat(actual).isEqualTo(getTestResource(target));
	}
}
