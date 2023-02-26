/*
 * Copyright 2016-2021 DiffPlug
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

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

class RemoveUnusedImportsStepTest {
	@Test
	void behavior() throws Exception {
		FormatterStep step = RemoveUnusedImportsStep.create(TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/removeunusedimports/JavaCodeUnformatted.test", "java/removeunusedimports/JavaCodeFormatted.test")
				.testResource("java/removeunusedimports/JavaCodeWithLicenseUnformatted.test", "java/removeunusedimports/JavaCodeWithLicenseFormatted.test")
				.testResource("java/removeunusedimports/JavaCodeWithLicensePackageUnformatted.test", "java/removeunusedimports/JavaCodeWithLicensePackageFormatted.test")
				.testResource("java/removeunusedimports/JavaCodeWithPackageUnformatted.test", "java/removeunusedimports/JavaCodeWithPackageFormatted.test")
				.testResource("java/removeunusedimports/Jdk17MultineStringBlockUnformatted.test", "java/removeunusedimports/Jdk17MultineStringBlockFormatted.test");
	}

	@Test
	void equality() throws Exception {
		new SerializableEqualityTester() {
			@Override
			protected void setupTest(API api) {
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return RemoveUnusedImportsStep.create(TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}
}
