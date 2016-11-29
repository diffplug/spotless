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

import org.junit.Assert;
import org.junit.Test;

public class BumpThisNumberIfACustomRuleChangesTest extends GradleIntegrationTest {
	private void writeBuildFile(String toInsert) throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    format 'misc', {",
				"        target file('README.md')",
				"        customLazyGroovy('lowercase') {",
				"             return { str -> str.toLowerCase(Locale.US) }",
				"        }",
				toInsert,
				"    }",
				"}");
	}

	private void writeContentWithBadFormatting() throws IOException {
		write("README.md", "ABC");
	}

	private void assertApplyWorks() throws IOException {
		gradleRunner().withArguments("spotlessApply").build();
		String result = read("README.md");
		Assert.assertEquals("abc\n", result);
	}

	@Test
	public void customRuleNeverUpToDate() throws IOException {
		writeBuildFile("");
		writeContentWithBadFormatting();
		assertApplyWorks();

		checkIsUpToDate(false);
		checkIsUpToDate(false);
	}

	@Test
	public void unlessBumpThisNumberIfACustomRuleChanges() throws IOException {
		writeBuildFile("bumpThisNumberIfACustomRuleChanges(1)");
		writeContentWithBadFormatting();
		assertApplyWorks();

		checkIsUpToDate(false);
		checkIsUpToDate(true);
	}

	@Test
	public void andRunsAgainIfNumberChanges() throws IOException {
		writeBuildFile("bumpThisNumberIfACustomRuleChanges(1)");
		writeContentWithBadFormatting();
		assertApplyWorks();

		checkIsUpToDate(false);
		checkIsUpToDate(true);

		writeBuildFile("bumpThisNumberIfACustomRuleChanges(2)");
		checkIsUpToDate(false);
		checkIsUpToDate(true);
	}
}
