/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.maven.shell;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;
import com.diffplug.spotless.tag.ShfmtTest;

@ShfmtTest
public class ShellTest extends MavenIntegrationHarness {
	@Test
	public void testFormatShellWithEditorconfig() throws Exception {
		String fileDir = "shell/shfmt/with-config/";
		setFile("shfmt.sh").toResource(fileDir + "shfmt.sh");
		setFile("scripts/other.sh").toResource(fileDir + "other.sh");
		setFile(".editorconfig").toResource(fileDir + ".editorconfig");

		writePomWithShellSteps("<shfmt/>");
		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFile("shfmt.sh").sameAsResource(fileDir + "shfmt.clean");
		assertFile("scripts/other.sh").sameAsResource(fileDir + "other.clean");
	}

	@Test
	public void testFormatShellWithoutEditorconfig() throws Exception {
		String fileDir = "shell/shfmt/without-config/";
		setFile("shfmt.sh").toResource(fileDir + "shfmt.sh");
		setFile("scripts/other.sh").toResource(fileDir + "other.sh");

		writePomWithShellSteps("<shfmt/>");
		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFile("shfmt.sh").sameAsResource(fileDir + "shfmt.clean");
		assertFile("scripts/other.sh").sameAsResource(fileDir + "other.clean");
	}
}
