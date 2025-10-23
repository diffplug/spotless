/*
 * Copyright 2016-2025 DiffPlug
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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

class EncodingTest extends GradleIntegrationHarness {
	@Test
	void defaultIsUtf8() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        custom 'replaceMicro', { it.replace('µ', 'A') }",
				"    }",
				"}");
		setFile("test.java").toContent("µ");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.java").hasContent("A");
	}

	@Test
	void globalIsRespected() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        custom 'replaceMicro', { it.replace('µ', 'A') }",
				"    }",
				"    encoding 'US-ASCII'",
				"}");
		setFile("test.java").toContent("µ");
		assertThat(gradleRunner().withArguments("spotlessApply").buildAndFail().getOutput()).doesNotContain("Encoding error!");
		assertFile("test.java").hasContent("µ");
	}

	@Test
	void globalIsRespectedButCanBeOverridden() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        custom 'replaceMicro', { it.replace('µ', 'A') }",
				"    }",
				"    format 'utf32', {",
				"        target file('utf32.encoded')",
				"        custom 'replaceMicro', { it.replace('µ', 'A') }",
				"        encoding 'UTF-32'",
				"    }",
				"    encoding 'US-ASCII'",
				"}");
		setFile("test.java").toContent("µ");
		setFile("utf32.encoded").toContent("µ", Charset.forName("UTF-32"));
		assertThat(gradleRunner().withArguments("spotlessApply").buildAndFail().getOutput()).doesNotContain("Encoding error!");
		assertFile("test.java").hasContent("µ");
		assertFile("utf32.encoded").hasContent("µ", Charset.forName("UTF-32"));
	}
}
