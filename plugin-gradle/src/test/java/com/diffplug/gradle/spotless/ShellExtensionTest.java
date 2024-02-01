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
package com.diffplug.gradle.spotless;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.tag.ShfmtTest;

@ShfmtTest
public class ShellExtensionTest extends GradleIntegrationHarness {
	@Test
	void shfmtWithEditorconfig() throws IOException {
		String fileDir = "shell/shfmt/singlefile/with-config/";
		setFile(".editorconfig").toResource(fileDir + ".editorconfig");
		setFile("build.gradle.kts").toLines(
				"plugins {",
				"  id(\"com.diffplug.spotless\")",
				"}",
				"spotless {",
				"  shell {",
				"    shfmt()",
				"  }",
				"}");

		setFile("shfmt.sh").toResource(fileDir + "shfmt.sh");

		gradleRunner().withArguments("spotlessApply").build();
		assertFile("shfmt.sh").sameAsResource(fileDir + "shfmt.clean");
	}

	@Test
	void shfmtMultipleFilesWithEditorconfig() throws IOException {
		String fileDir = "shell/shfmt/multifile/with-config/";

		setFile(".editorconfig").toResource(fileDir + ".editorconfig");
		setFile("build.gradle.kts").toLines(
				"plugins {",
				"  id(\"com.diffplug.spotless\")",
				"}",
				"spotless {",
				"  shell {",
				"    shfmt()",
				"  }",
				"}");

		setFile("shfmt.sh").toResource(fileDir + "shfmt.sh");
		setFile("other.sh").toResource(fileDir + "other.sh");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("other.sh").sameAsResource(fileDir + "other.clean");
		assertFile("shfmt.sh").sameAsResource(fileDir + "shfmt.clean");
	}

	@Test
	void shfmtWithoutEditorconfig() throws IOException {
		String fileDir = "shell/shfmt/singlefile/without-config/";

		setFile("build.gradle.kts").toLines(
				"plugins {",
				"  id(\"com.diffplug.spotless\")",
				"}",
				"spotless {",
				"  shell {",
				"    shfmt()",
				"  }",
				"}");

		setFile("shfmt.sh").toResource(fileDir + "shfmt.sh");

		gradleRunner().withArguments("spotlessApply").build();
		assertFile("shfmt.sh").sameAsResource(fileDir + "shfmt.clean");
	}

	@Test
	void shfmtMultipleFilesWithoutEditorconfig() throws IOException {
		String fileDir = "shell/shfmt/multifile/without-config/";

		setFile("build.gradle.kts").toLines(
				"plugins {",
				"  id(\"com.diffplug.spotless\")",
				"}",
				"spotless {",
				"  shell {",
				"    shfmt()",
				"  }",
				"}");

		setFile("shfmt.sh").toResource(fileDir + "shfmt.sh");
		setFile("other.sh").toResource(fileDir + "other.sh");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("other.sh").sameAsResource(fileDir + "other.clean");
		assertFile("shfmt.sh").sameAsResource(fileDir + "shfmt.clean");
	}
}
