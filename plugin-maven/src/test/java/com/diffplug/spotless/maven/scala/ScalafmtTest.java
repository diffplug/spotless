/*
 * Copyright 2016-2021 DiffPlug
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
package com.diffplug.spotless.maven.scala;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class ScalafmtTest extends MavenIntegrationHarness {
	@Test
	void testScalafmtWithDefaultConfig() throws Exception {
		writePomWithScalaSteps("<scalafmt/>");

		runTest("scala/scalafmt/basic.clean_3.0.0");
	}

	@Test
	void testScalafmtWithCustomConfig() throws Exception {
		setFile("scalafmt.conf").toResource("scala/scalafmt/scalafmt.conf");

		writePomWithScalaSteps(
				"<scalafmt>",
				"  <file>${project.basedir}/scalafmt.conf</file>",
				"</scalafmt>");

		runTest("scala/scalafmt/basic.cleanWithCustomConf_3.0.0");
	}

	private void runTest(String s) throws Exception {
		String path = "src/main/scala/test.scala";
		setFile(path).toResource("scala/scalafmt/basic.dirty");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource(s);
	}
}
