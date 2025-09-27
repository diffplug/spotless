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
package com.diffplug.spotless.maven.generic;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class LineEndingsTest extends MavenIntegrationHarness {

	@Test
	void fromContentToWindows() throws Exception {
		writePomWithFormatSteps(
				"<lineEndings>WINDOWS</lineEndings>");
		runToWindowsTest();
	}

	@Test
	void fromContentToUnix() throws Exception {
		writePomWithFormatSteps(
				"<lineEndings>UNIX</lineEndings>");
		runToUnixTest();
	}

	private void runToWindowsTest() throws Exception {
		runTest(getClassWithLineEndings("\n"), getClassWithLineEndings("\r\n"));
	}

	private void runToUnixTest() throws Exception {
		runTest(getClassWithLineEndings("\r\n"), getClassWithLineEndings("\n"));
	}

	private void runTest(String sourceContent, String targetContent) throws Exception {
		String path = "src/main/java/test.java";
		setFile(path).toContent(sourceContent);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).hasContent(targetContent);
	}

	private String getClassWithLineEndings(String lineEnding) {
		return "public class Java {" + lineEnding
				+ "    public static void main(String[] args) {" + lineEnding
				+ "        System.out.println(\"hello\");" + lineEnding
				+ "    }" + lineEnding
				+ "}";
	}
}
