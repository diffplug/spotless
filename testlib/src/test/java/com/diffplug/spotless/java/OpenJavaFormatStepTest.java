/*
 * Copyright 2026 DiffPlug
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

import static org.junit.jupiter.api.condition.JRE.JAVA_21;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

// open-java-format is compiled for Java 21. It formats exactly like palantir-java-format, so it shares its resources.
@EnabledForJreRange(min = JAVA_21)
class OpenJavaFormatStepTest extends ResourceHarness {

	@Test
	void jvm13Features() throws Exception {
		try (StepHarness step = StepHarness.forStep(OpenJavaFormatStep.create(TestProvisioner.mavenCentral()))) {
			step.testResource("java/palantirjavaformat/TextBlock.dirty", "java/palantirjavaformat/TextBlock.clean");
		}
	}

	@Test
	void behavior() throws Exception {
		FormatterStep step = OpenJavaFormatStep.create(TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/palantirjavaformat/JavaCodeUnformatted.test", "java/palantirjavaformat/JavaCodeFormatted.test")
				.testResource("java/palantirjavaformat/JavaCodeWithLicenseUnformatted.test", "java/palantirjavaformat/JavaCodeWithLicenseFormatted.test")
				.testResource("java/palantirjavaformat/JavaCodeWithPackageUnformatted.test", "java/palantirjavaformat/JavaCodeWithPackageFormatted.test");
	}

	@Test
	void formatJavadoc() throws Exception {
		FormatterStep step = OpenJavaFormatStep.create(OpenJavaFormatStep.defaultVersion(), "OJF", true, TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/palantirjavaformat/JavaCodeWithJavaDocUnformatted.test", "java/palantirjavaformat/JavaCodeWithJavaDocFormatted.test")
				.testResource("java/palantirjavaformat/JavaCodeWithPackageUnformatted.test", "java/palantirjavaformat/JavaCodeWithPackageFormatted.test");
	}

	@Test
	void behaviorWithGoogleStyle() throws Exception {
		FormatterStep step = OpenJavaFormatStep.create(OpenJavaFormatStep.defaultVersion(), "GOOGLE", TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/palantirjavaformat/JavaCodeUnformatted.test", "java/palantirjavaformat/JavaCodeFormattedGoogle.test")
				.testResource("java/palantirjavaformat/JavaCodeWithLicenseUnformatted.test", "java/palantirjavaformat/JavaCodeWithLicenseFormattedGoogle.test")
				.testResource("java/palantirjavaformat/JavaCodeWithPackageUnformatted.test", "java/palantirjavaformat/JavaCodeWithPackageFormattedGoogle.test");
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			String style = "OJF";
			boolean formatJavadoc;

			@Override
			protected void setupTest(API api) {
				// same settings == same
				api.areDifferentThan();

				// change the style, and it's different
				style = "AOSP";
				api.areDifferentThan();
				style = "OJF";

				// change the format Java doc flag, and it's different
				formatJavadoc = true;
				api.areDifferentThan();
				formatJavadoc = false;
			}

			@Override
			protected FormatterStep create() {
				return OpenJavaFormatStep.create(OpenJavaFormatStep.defaultVersion(), style, formatJavadoc, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}
}
