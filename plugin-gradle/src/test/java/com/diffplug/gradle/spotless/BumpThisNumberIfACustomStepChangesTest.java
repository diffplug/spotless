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

import java.io.IOException;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

abstract class BumpThisNumberIfACustomStepChangesTest extends GradleIntegrationHarness {
	private boolean useConfigCache;

	BumpThisNumberIfACustomStepChangesTest(boolean useConfigCache) {
		this.useConfigCache = useConfigCache;
	}

	static class WithConfigCache extends BumpThisNumberIfACustomStepChangesTest {
		WithConfigCache() {
			super(true);
		}
	}

	static class WithoutConfigCache extends BumpThisNumberIfACustomStepChangesTest {
		WithoutConfigCache() {
			super(false);
		}
	}

	@Override
	public GradleRunner gradleRunner() throws IOException {
		if (useConfigCache) {
			setFile("gradle.properties").toLines("org.gradle.unsafe.configuration-cache=true",
					"org.gradle.configuration-cache=true");
			return super.gradleRunner().withGradleVersion(GradleVersionSupport.CONFIGURATION_CACHE.version);
		} else {
			return super.gradleRunner();
		}
	}

	private void writeBuildFile(String toInsert) throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    format 'misc', {",
				"        target file('README.md')",
				"        custom 'lowercase', { str -> str.toLowerCase(Locale.ROOT) }",
				toInsert,
				"    }",
				"}");
	}

	private void writeContentWithBadFormatting() throws IOException {
		setFile("README.md").toContent("ABC");
	}

	@Override
	public void applyIsUpToDate(boolean upToDate) throws IOException {
		super.applyIsUpToDate(upToDate);
		assertFile("README.md").hasContent("abc");
	}

	@Test
	void customRuleNeverUpToDate() throws IOException {
		writeBuildFile("");
		writeContentWithBadFormatting();
		applyIsUpToDate(false);
		checkIsUpToDate(false);
		if (useConfigCache) {
			// if the config cache is in-effect, then it's okay for custom rules to become "up-to-date"
			checkIsUpToDate(true);
		} else {
			checkIsUpToDate(false);
		}
	}

	@Test
	void unlessBumpThisNumberIfACustomStepChanges() throws IOException {
		writeBuildFile("bumpThisNumberIfACustomStepChanges(1)");
		writeContentWithBadFormatting();
		applyIsUpToDate(false);
		applyIsUpToDate(false);
		applyIsUpToDate(true);
		checkIsUpToDate(true);

		writeContentWithBadFormatting();
		applyIsUpToDate(false);
		checkIsUpToDate(false);
		checkIsUpToDate(true);
	}

	@Test
	void andRunsAgainIfNumberChanges() throws IOException {
		writeBuildFile("bumpThisNumberIfACustomStepChanges(1)");
		writeContentWithBadFormatting();
		applyIsUpToDate(false);
		checkIsUpToDate(false);
		checkIsUpToDate(true);

		writeBuildFile("bumpThisNumberIfACustomStepChanges(2)");
		checkIsUpToDate(false);
		checkIsUpToDate(true);
	}
}
