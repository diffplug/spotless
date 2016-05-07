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

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.gradle.spotless.LicenseHeaderStep;
import com.diffplug.gradle.spotless.java.JavaExtension;

public class LicenseHeaderStepTest extends ResourceTest {
	private static final String KEY_LICENSE = "TestLicense";
	private static final String KEY_FILE_NOTAPPLIED = "Java8CodeFormatted.test";
	private static final String KEY_FILE_APPLIED = "JavaCodeFormattedWithLicense.test";

	@Test
	public void fromString() throws Throwable {
		LicenseHeaderStep step = new LicenseHeaderStep(getTestResource(KEY_LICENSE), JavaExtension.LICENSE_HEADER_DELIMITER, LineEnding.DERIVED);
		assertStep(step::format, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void fromFile() throws Throwable {
		LicenseHeaderStep step = new LicenseHeaderStep(createTestFile(KEY_LICENSE), JavaExtension.LICENSE_HEADER_DELIMITER, LineEnding.DERIVED);
		assertStep(step::format, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void efficient() throws Throwable {
		LicenseHeaderStep step = new LicenseHeaderStep("LicenseHeader\n", "contentstart", LineEnding.UNIX);
		String alreadyCorrect = "LicenseHeader\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect));
		// If no change is required, it should return the exact same string for efficiency reasons
		Assert.assertTrue(alreadyCorrect == step.format(alreadyCorrect));
	}

	@Test
	public void sanitized() throws Throwable {
		// The sanitizer should add the line separator for the LineEnding
		LicenseHeaderStep step = new LicenseHeaderStep("LicenseHeader", "contentstart", LineEnding.WINDOWS);
		String alreadyCorrect = "LicenseHeader\r\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect));
		Assert.assertTrue(alreadyCorrect == step.format(alreadyCorrect));
	}

	@Test
	public void sanitizerDoesntGoTooFar() throws Throwable {
		// if the user wants extra lines after the header, we shouldn't clobber them
		LicenseHeaderStep step = new LicenseHeaderStep("LicenseHeader\n\n", "contentstart", LineEnding.UNIX);
		String alreadyCorrect = "LicenseHeader\n\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect));
		Assert.assertTrue(alreadyCorrect == step.format(alreadyCorrect));
	}
}
