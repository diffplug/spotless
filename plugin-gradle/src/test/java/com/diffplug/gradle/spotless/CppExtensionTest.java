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

public class CppExtensionTest extends GradleIntegrationTest {

	@Test
	public void testEclipseFormatter() throws IOException {
		setFile("cdt.properties").toContent(
				"org.eclipse.cdt.core.formatter.tabulation.size=3\n" +
						"org.eclipse.cdt.core.formatter.tabulation.char=space");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    cpp {",
				"        eclipse().configFile('./cdt.properties')",
				"    }",
				"}");

		String input = "int main() {\n\tint a = 1;\n}";
		setFile("src/main/test.c").toContent(input);
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/test.c").hasContent(input.replace("\t", "   "));
	}

}
