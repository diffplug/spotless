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
package com.diffplug.spotless.maven.cpp;

import org.junit.Test;

import com.diffplug.spotless.maven.MavenIntegrationTest;

public class EclipseTest extends MavenIntegrationTest {

	@Test
	public void testConfigAndIncludes() throws Exception {
		writePomWithCppSteps("<eclipse><file>./cdt.properties</file></eclipse>");

		String input = "int main() {\n\tint a = 1;\n}";
		setFile("cdt.properties").toContent(
				"org.eclipse.cdt.core.formatter.tabulation.size=3\n" +
						"org.eclipse.cdt.core.formatter.tabulation.char=space");

		String path = "src/main/cpp/file1.c++";
		setFile(path).toContent(input);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).hasContent(input.replace("\t", "   "));
	}
}
