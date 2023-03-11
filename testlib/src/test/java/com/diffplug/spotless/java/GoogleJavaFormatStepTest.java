/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.java;

import static org.junit.jupiter.api.condition.JRE.JAVA_13;
import static org.junit.jupiter.api.condition.JRE.JAVA_15;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

class GoogleJavaFormatStepTest extends ResourceHarness {

	@Test
	@EnabledForJreRange(min = JAVA_13)
	void jvm13Features() throws Exception {
		try (StepHarness step = StepHarness.forStep(GoogleJavaFormatStep.create(TestProvisioner.mavenCentral()))) {
			step.testResource("java/googlejavaformat/TextBlock.dirty", "java/googlejavaformat/TextBlock.clean");
		}
	}

	@Test
	@EnabledForJreRange(max = JAVA_15) // google-java-format requires JRE 11+
	void behavior18() throws Exception {
		FormatterStep step = GoogleJavaFormatStep.create("1.8", TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormatted18.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormatted.test");
	}

	@Test
	void behavior() throws Exception {
		FormatterStep step = GoogleJavaFormatStep.create("1.2", TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormatted.test");
	}

	@Test
	void behaviorWithAospStyle() throws Exception {
		FormatterStep step = GoogleJavaFormatStep.create("1.2", "AOSP", TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormattedAOSP.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormattedAOSP.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormattedAOSP.test")
				.testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormattedAOSP.test");
	}

	@Test
	void behaviorWithReflowLongStrings() throws Exception {
		try (StepHarness step = StepHarness.forStep(GoogleJavaFormatStep.create(GoogleJavaFormatStep.defaultVersion(), GoogleJavaFormatStep.defaultStyle(), TestProvisioner.mavenCentral(), true))) {
			if (Jvm.version() >= 11) {
				step.testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormattedReflowLongStrings.test")
						.testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormattedReflowLongStrings.test")
						.testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormattedReflowLongStrings.test")
						.testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormattedReflowLongStrings.test");
			}
		}
	}

	@Test
	void behaviorWithCustomGroupArtifact() throws Exception {
		FormatterStep step = GoogleJavaFormatStep.create(GoogleJavaFormatStep.defaultGroupArtifact(), "1.2", GoogleJavaFormatStep.defaultStyle(), TestProvisioner.mavenCentral(), false);
		StepHarness.forStep(step)
				.testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormatted.test");
	}

	@Test
	void equality() throws Exception {
		new SerializableEqualityTester() {
			String version = "1.2";
			String style = "";
			boolean reflowLongStrings = false;

			@Override
			protected void setupTest(API api) {
				// same version == same
				api.areDifferentThan();
				// change the version, and it's different
				version = "1.1";
				api.areDifferentThan();
				// change the style, and it's different
				style = "AOSP";
				api.areDifferentThan();
				// change the reflowLongStrings flag, and it's different
				reflowLongStrings = true;
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				String finalVersion = this.version;
				return GoogleJavaFormatStep.create(finalVersion, style, TestProvisioner.mavenCentral(), reflowLongStrings);
			}
		}.testEquals();
	}

	@Test
	void equalityGroupArtifact() throws Exception {
		new SerializableEqualityTester() {
			String groupArtifact = GoogleJavaFormatStep.defaultGroupArtifact();
			final String version = "1.11.0";
			final String style = "";
			final boolean reflowLongStrings = false;

			@Override
			protected void setupTest(API api) {
				// same version == same
				api.areDifferentThan();
				// change the groupArtifact, and it's different
				groupArtifact = "io.opil:google-java-format";
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return GoogleJavaFormatStep.create(groupArtifact, version, style, TestProvisioner.mavenCentral(), reflowLongStrings);
			}
		}.testEquals();
	}

	@Test
	void fixWindowsBugForGfj1Point1() {
		fixWindowsBugTestcase("");
		fixWindowsBugTestcase(
				"",
				"import somepackage;",
				"");
		fixWindowsBugTestcase(
				"import somepackage;",
				"",
				"public class SomeClass {}");
		fixWindowsBugTestcase(
				"/** Some license */",
				"import somepackage;",
				"",
				"public class SomeClass {}");
		fixWindowsBugTestcase(
				"package thispackage;",
				"",
				"import somepackage;",
				"",
				"public class SomeClass {}");
		fixWindowsBugTestcase(
				"/*",
				" * A License.",
				" */",
				"",
				"package thispackage;",
				"",
				"import somepackage;",
				"",
				"public class SomeClass {}");
	}

	private void fixWindowsBugTestcase(String... lines) {
		String input = StringPrinter.buildStringFromLines(lines);
		Assertions.assertEquals(input, GoogleJavaFormatStep.fixWindowsBug(input, "1.1"));
	}
}
