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
package com.diffplug.spotless.cli.steps;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.cli.CLIIntegrationHarness;
import com.diffplug.spotless.cli.SpotlessCLIRunner;

public class LicenseHeaderTest extends CLIIntegrationHarness {

	@Test
	void assertHeaderMustBeSpecified() {
		SpotlessCLIRunner.Result result = cliRunner()
				.withTargets("**/*.java")
				.withStep(LicenseHeader.class)
				.runAndFail();

		assertThat(result.stdErr())
				.containsPattern(".*Missing required.*header.*");
	}

	@Test
	void assertHeaderIsApplied() {
		setFile("TestFile.java").toContent("public class TestFile {}");

		SpotlessCLIRunner.Result result = cliRunner()
				.withTargets("TestFile.java")
				.withStep(LicenseHeader.class)
				.withOption("--header", "/* License */")
				.run();

		assertFile("TestFile.java").hasContent("/* License */\npublic class TestFile {}");
	}

	@Test
	void assertHeaderFileIsApplied() {
		setFile("TestFile.java").toContent("public class TestFile {}");
		setFile("header.txt").toContent("/* License */");

		SpotlessCLIRunner.Result result = cliRunner()
				.withTargets("TestFile.java")
				.withStep(LicenseHeader.class)
				.withOption("--header-file", "header.txt")
				.run();

		assertFile("TestFile.java").hasContent("/* License */\npublic class TestFile {}");
	}

	@Test
	void assertDelimiterIsApplied() {
		setFile("TestFile.java").toContent("/* keep me */\npublic class TestFile {}");

		SpotlessCLIRunner.Result result = cliRunner()
				.withTargets("TestFile.java")
				.withStep(LicenseHeader.class)
				.withOption("--header", "/* License */")
				.withOption("--delimiter", "\\/\\* keep me")
				.run();

		assertFile("TestFile.java").hasContent("/* License */\n/* keep me */\npublic class TestFile {}");
	}
}
