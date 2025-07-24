/*
 * Copyright 2021-2025 DiffPlug
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

import static org.assertj.core.api.Assumptions.assumeThat;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

public class NativeCmdTest extends MavenIntegrationHarness {

	@Test
	public void fromStdInToStdOut() throws Exception {
		// This will only work if /usr/bin/sed is available
		assumeThat(new File("/usr/bin/sed")).exists();
		writePomWithFormatSteps(
				"<nativeCmd>",
				"  <name>Greetings to Mars</name>",
				"  <pathToExe>/usr/bin/sed</pathToExe>",
				"  <arguments>",
				"    <argument>s/World/Mars/g</argument>",
				"  </arguments>",
				"</nativeCmd>");
		runTest("Hello World", "Hello Mars");
	}

	private void runTest(String sourceContent, String targetContent) throws Exception {
		setFile(TEST_PATH).toContent(sourceContent);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(TEST_PATH).hasContent(targetContent);
	}
}
