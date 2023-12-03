/*
 * Copyright 2022-2023 DiffPlug
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

class PalantirJavaFormatStepTest extends ResourceHarness {

	@Test
	@EnabledForJreRange(min = JAVA_13)
	void jvm13Features() throws Exception {
		try (StepHarness step = StepHarness.forStep(PalantirJavaFormatStep.create(TestProvisioner.mavenCentral()))) {
			step.testResource("java/palantirjavaformat/TextBlock.dirty", "java/palantirjavaformat/TextBlock.clean");
		}
	}

	@Test
	void behavior2() throws Exception {
		FormatterStep step = PalantirJavaFormatStep.create(TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/palantirjavaformat/JavaCodeUnformatted.test", "java/palantirjavaformat/JavaCodeFormatted.test")
				.testResource("java/palantirjavaformat/JavaCodeWithLicenseUnformatted.test", "java/palantirjavaformat/JavaCodeWithLicenseFormatted.test")
				.testResource("java/palantirjavaformat/JavaCodeWithPackageUnformatted.test", "java/palantirjavaformat/JavaCodeWithPackageFormatted.test");
	}

	@Test
	void behavior() throws Exception {
		FormatterStep step = PalantirJavaFormatStep.create("1.1.0", TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/palantirjavaformat/JavaCodeUnformatted.test", "java/palantirjavaformat/JavaCodeFormatted.test")
				.testResource("java/palantirjavaformat/JavaCodeWithLicenseUnformatted.test", "java/palantirjavaformat/JavaCodeWithLicenseFormatted.test")
				.testResource("java/palantirjavaformat/JavaCodeWithPackageUnformatted.test", "java/palantirjavaformat/JavaCodeWithPackageFormatted.test");
	}

	@Test
	void behaviorWithGoogleStyle() throws Exception {
		FormatterStep step = PalantirJavaFormatStep.create("1.1.0", "GOOGLE", TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/palantirjavaformat/JavaCodeUnformatted.test", "java/palantirjavaformat/JavaCodeFormattedGoogle.test")
				.testResource("java/palantirjavaformat/JavaCodeWithLicenseUnformatted.test", "java/palantirjavaformat/JavaCodeWithLicenseFormattedGoogle.test")
				.testResource("java/palantirjavaformat/JavaCodeWithPackageUnformatted.test", "java/palantirjavaformat/JavaCodeWithPackageFormattedGoogle.test");
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			String version = "1.1.0";
			String style = "";

			@Override
			protected void setupTest(API api) {
				// same version == same
				api.areDifferentThan();
				// change the version, and it's different
				version = "1.0.0";
				api.areDifferentThan();
				// change the style, and it's different
				style = "AOSP";
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				String finalVersion = this.version;
				return PalantirJavaFormatStep.create(finalVersion, style, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}
}
