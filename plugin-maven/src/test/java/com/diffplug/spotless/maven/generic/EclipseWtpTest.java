/*
 * Copyright 2016-2023 DiffPlug
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

class EclipseWtpTest extends MavenIntegrationHarness {

	@Test
	void testType() throws Exception {
		writePomWithFormatSteps(
				"<eclipseWtp>",
				"<type>XML</type>",
				"</eclipseWtp>");
		runTest();
	}

	private void runTest() throws Exception {
		var notFormatted = "<a><b>   c</b></a>";
		var formatted = "<a>\n\t<b> c</b>\n</a>";
		//writePomWithFormatSteps includes java. WTP does not care about file extensions.
		setFile("src/main/java/test.java").toContent(notFormatted);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("src/main/java/test.java").hasContent(formatted);
	}
}
