/*
 * Copyright 2016-2020 DiffPlug
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

import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JreVersion;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

public class GoogleJavaFormatStepTest extends ResourceHarness {
	@Test
	public void suggestJre11() throws Exception {
		try (StepHarness step = StepHarness.forStep(GoogleJavaFormatStep.create(TestProvisioner.mavenCentral()))) {
			if (JreVersion.thisVm() < 11) {
				step.testException("java/googlejavaformat/TextBlock.dirty", throwable -> {
					throwable.hasMessageStartingWith("You are running Spotless on JRE 8")
							.hasMessageEndingWith(", which limits you to google-java-format 1.7\n"
									+ "If you upgrade your build JVM to 11+, then you can use google-java-format 1.9, which may have fixed this problem.");
				});
			} else if (JreVersion.thisVm() < 13) {
				step.testException("java/googlejavaformat/TextBlock.dirty", throwable -> {
					throwable.isInstanceOf(InvocationTargetException.class)
							.extracting(exception -> exception.getCause().getMessage()).asString().contains("7:18: error: unclosed string literal");
				});
			} else {
				// JreVersion.thisVm() >= 13
				step.testResource("java/googlejavaformat/TextBlock.dirty", "java/googlejavaformat/TextBlock.clean");
			}
		}
	}

	@Test
	public void behavior18() throws Exception {
		// google-java-format requires JRE 11+
		JreVersion.assume11OrGreater();
		FormatterStep step = GoogleJavaFormatStep.create("1.8", TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormatted18.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormatted.test");
	}

	@Test
	public void behavior() throws Exception {
		FormatterStep step = GoogleJavaFormatStep.create("1.2", TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormatted.test")
				.testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormatted.test");
	}

	@Test
	public void behaviorWithAospStyle() throws Exception {
		FormatterStep step = GoogleJavaFormatStep.create("1.2", "AOSP", TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormattedAOSP.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormattedAOSP.test")
				.testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormattedAOSP.test")
				.testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormattedAOSP.test");
	}

	@Test
	public void equality() throws Exception {
		new SerializableEqualityTester() {
			String version = "1.2";
			String style = "";

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
			}

			@Override
			protected FormatterStep create() {
				String finalVersion = this.version;
				return GoogleJavaFormatStep.create(finalVersion, style, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}

	@Test
	public void fixWindowsBugForGfj1Point1() {
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
		Assert.assertEquals(input, GoogleJavaFormatStep.fixWindowsBug(input, "1.1"));
	}
}
