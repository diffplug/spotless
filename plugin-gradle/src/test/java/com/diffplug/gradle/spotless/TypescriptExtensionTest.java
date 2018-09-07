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

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.spotless.category.NpmTest;

@Category(NpmTest.class)
public class TypescriptExtensionTest extends GradleIntegrationTest {
	@Test
	public void useTsfmtInlineConfig() throws IOException {
		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"def tsfmtconfig = [:]",
				"tsfmtconfig['indentSize'] = 1",
				"tsfmtconfig['convertTabsToSpaces'] = true",
				"spotless {",
				"    typescript {",
				"        target 'test.ts'",
				"        tsfmt().config(tsfmtconfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean");
	}

	@Test
	public void useTsfmtFileConfig() throws IOException {
		File formattingFile = setFile("tsfmt.json").toLines(
				"{",
				"	\"indentSize\": 1,",
				"	\"convertTabsToSpaces\": true",
				"}");
		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    typescript {",
				"        target 'test.ts'",
				"        tsfmt().configFile('tsfmt', '" + formattingFile.getAbsolutePath() + "')",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean");
	}

	@Test
	public void usePrettier() throws IOException {
		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    typescript {",
				"        target 'test.ts'",
				"        prettier()",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/filetypes/typescript/typescript.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/prettier/filetypes/typescript/typescript.clean");
	}
}
