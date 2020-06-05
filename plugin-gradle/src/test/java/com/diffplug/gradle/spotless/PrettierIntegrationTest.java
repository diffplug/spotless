/*
 * Copyright 2015-2020 DiffPlug
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
import org.junit.experimental.categories.Category;

import com.diffplug.spotless.category.NpmTest;

@Category(NpmTest.class)
public class PrettierIntegrationTest extends GradleIntegrationHarness {
	@Test
	public void useInlineConfig() throws IOException {
		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"def prettierConfig = [:]",
				"prettierConfig['printWidth'] = 50",
				"prettierConfig['parser'] = 'typescript'",
				"spotless {",
				"    format 'mytypescript', {",
				"        target 'test.ts'",
				"        prettier().config(prettierConfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile.clean");
	}

	@Test
	public void useFileConfig() throws IOException {
		setFile(".prettierrc.yml").toResource("npm/prettier/config/.prettierrc.yml");
		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    format 'mytypescript', {",
				"        target 'test.ts'",
				"        prettier().configFile('.prettierrc.yml')",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile.clean");
	}

}
