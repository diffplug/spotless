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
package com.diffplug.spotless.maven.scala;

import org.junit.Test;

import com.diffplug.spotless.maven.MavenIntegrationTest;

public class ScalafmtTest extends MavenIntegrationTest {
	@Test
	public void testScalafmtWithDefaultConfig() throws Exception {
		writePomWithScalaSteps("<scalafmt/>");

		setFile("src/main/scala/test.scala").toResource("scala/scalafmt/basic.dirty");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("src/main/scala/test.scala").sameAsResource("scala/scalafmt/basic.clean");
	}

	@Test
	public void testScalafmtWithCustomConfig() throws Exception {
		writePomWithScalaSteps(
				"<scalafmt>",
				"  <file>${project.basedir}/scalafmt.conf</file>",
				"</scalafmt>");
		setFile("scalafmt.conf").toResource("scala/scalafmt/scalafmt.conf");

		setFile("src/main/scala/test.scala").toResource("scala/scalafmt/basic.dirty");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("src/main/scala/test.scala").sameAsResource("scala/scalafmt/basic.cleanWithCustomConf");
	}
}
