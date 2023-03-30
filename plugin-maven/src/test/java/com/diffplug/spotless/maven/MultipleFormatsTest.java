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
package com.diffplug.spotless.maven;

import org.junit.jupiter.api.Test;

class MultipleFormatsTest extends MavenIntegrationHarness {

	@Test
	void testMultipleFormatsWithDifferentIncludes() throws Exception {
		writePom(
				"<formats>",
				"  <format>",
				"    <includes>",
				"      <include>src/**/java/**/*.java</include>",
				"    </includes>",
				"    <replace>",
				"      <name>Greetings to Mars</name>",
				"      <search>World</search>",
				"      <replacement>Mars</replacement>",
				"    </replace>",
				"    <licenseHeader>",
				"      <content>// License Header #1</content>",
				"      <delimiter>package</delimiter>",
				"    </licenseHeader>",
				"  </format>",
				"  <format>",
				"    <includes>",
				"      <include>src/**/txt/**/*.txt</include>",
				"    </includes>",
				"    <replace>",
				"      <name>Greetings to Titan</name>",
				"      <search>World</search>",
				"      <replacement>Titan</replacement>",
				"    </replace>",
				"    <licenseHeader>",
				"      <content>// License Header #2</content>",
				"      <delimiter>Just</delimiter>",
				"    </licenseHeader>",
				"  </format>",
				"</formats>");

		var path1 = "src/main/java/test1.java";
		var path2 = "src/main/java/test2.java";

		var path3 = "src/main/txt/test1.txt";
		var path4 = "src/main/txt/test2.txt";
		var path5 = "src/main/txt/test3.txt";

		setFile(path1).toContent("package test;\npublic class JavaWorld1 {}");
		setFile(path2).toContent("package test;\npublic class JavaWorld2 {}");

		setFile(path3).toContent("Just a text file #1\nHello World!");
		setFile(path4).toContent("Just a text file #2\nHello World!");
		setFile(path5).toContent("Just a text file #3\nHello World!");

		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFile(path1).hasContent("// License Header #1\npackage test;\npublic class JavaMars1 {}");
		assertFile(path2).hasContent("// License Header #1\npackage test;\npublic class JavaMars2 {}");

		assertFile(path3).hasContent("// License Header #2\nJust a text file #1\nHello Titan!");
		assertFile(path4).hasContent("// License Header #2\nJust a text file #2\nHello Titan!");
		assertFile(path5).hasContent("// License Header #2\nJust a text file #3\nHello Titan!");
	}
}
