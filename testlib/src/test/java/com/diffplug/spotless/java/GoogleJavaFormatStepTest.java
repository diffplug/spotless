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
package com.diffplug.spotless.java;

import static org.junit.jupiter.api.condition.JRE.JAVA_15;
import static org.junit.jupiter.api.condition.JRE.JAVA_20;
import static org.junit.jupiter.api.condition.JRE.JAVA_21;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

class GoogleJavaFormatStepTest extends ResourceHarness {

	@Test
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
		FormatterStep step = GoogleJavaFormatStep.create(GoogleJavaFormatStep.defaultVersion(), TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormatted.test");
	}

	@Test
	@EnabledForJreRange(min = JAVA_21, max = JAVA_21)
	void versionBelowMinimumRequiredVersionIsNotAllowed() throws Exception {
		FormatterStep step = GoogleJavaFormatStep.create("1.2", "AOSP", TestProvisioner.mavenCentral());
		StepHarness.forStepNoRoundtrip(step)
				.expectLintsOfResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test")
				.toBe("LINE_UNDEFINED google-java-format(jvm-version) You are running Spotless on JVM 21. This requires google-java-format of at least 1.17.0 (you are using 1.2). (...)");
	}

	@Test
	@EnabledForJreRange(min = JAVA_21, max = JAVA_21)
	void versionBelowOneDotTenIsNotAllowed() throws Exception {
		FormatterStep step = GoogleJavaFormatStep.create("1.9", "AOSP", TestProvisioner.mavenCentral());
		StepHarness.forStepNoRoundtrip(step)
				.expectLintsOfResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test")
				.toBe("LINE_UNDEFINED google-java-format(jvm-version) You are running Spotless on JVM 21. This requires google-java-format of at least 1.17.0 (you are using 1.9). (...)");
	}

	@Test
	void behaviorWithAospStyle() throws Exception {
		FormatterStep step = GoogleJavaFormatStep.create(GoogleJavaFormatStep.defaultVersion(), "AOSP", TestProvisioner.mavenCentral());
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
	void behaviorWithSkipFormatJavadoc() throws Exception {
		try (StepHarness step = StepHarness.forStep(GoogleJavaFormatStep.create(GoogleJavaFormatStep.defaultGroupArtifact(), GoogleJavaFormatStep.defaultVersion(), GoogleJavaFormatStep.defaultStyle(), TestProvisioner.mavenCentral(), GoogleJavaFormatStep.defaultReflowLongStrings(), GoogleJavaFormatStep.defaultReorderImports(), false))) {
			step.testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormattedSkipJavadocFormatting.test")
					.testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormatted.test")
					.testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormatted.test")
					.testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormatted.test");
		}
	}

	@Test
	void behaviorWithCustomGroupArtifact() throws Exception {
		FormatterStep step = GoogleJavaFormatStep.create(GoogleJavaFormatStep.defaultGroupArtifact(), GoogleJavaFormatStep.defaultVersion(), GoogleJavaFormatStep.defaultStyle(), TestProvisioner.mavenCentral(), false);
		StepHarness.forStep(step)
				.testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormatted.test");
	}

	@Test
	void behaviorWithReorderImports() throws Exception {
		FormatterStep enabled = GoogleJavaFormatStep.create(GoogleJavaFormatStep.defaultGroupArtifact(), GoogleJavaFormatStep.defaultVersion(), "AOSP", TestProvisioner.mavenCentral(), GoogleJavaFormatStep.defaultReflowLongStrings(), true);
		FormatterStep disabled = GoogleJavaFormatStep.create(GoogleJavaFormatStep.defaultGroupArtifact(), GoogleJavaFormatStep.defaultVersion(), "AOSP", TestProvisioner.mavenCentral(), GoogleJavaFormatStep.defaultReflowLongStrings(), false);
		String unformatted = "java/googlejavaformat/JavaWithReorderImportsUnformatted.test";
		try (StepHarness step = StepHarness.forStep(enabled)) {
			step.testResource(unformatted, "java/googlejavaformat/JavaWithReorderImportsEnabledFormatted.test");
		}
		try (StepHarness step = StepHarness.forStep(disabled)) {
			step.testResource(unformatted, "java/googlejavaformat/JavaWithReorderImportsDisabledFormatted.test");
		}
	}

	@Test
	@EnabledForJreRange(max = JAVA_20)
	void equality() throws Exception {
		new SerializableEqualityTester() {
			String version = "1.10.0";
			String style = "";
			boolean reflowLongStrings = false;

			@Override
			protected void setupTest(API api) {
				// same version == same
				api.areDifferentThan();
				// change the version, and it's different
				version = "1.11.0";
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
	@EnabledForJreRange(max = JAVA_20)
	void equalityGroupArtifact() throws Exception {
		new SerializableEqualityTester() {
			String groupArtifact = GoogleJavaFormatStep.defaultGroupArtifact();
			String version = "1.11.0";
			String style = "";
			boolean reflowLongStrings = false;

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

}
