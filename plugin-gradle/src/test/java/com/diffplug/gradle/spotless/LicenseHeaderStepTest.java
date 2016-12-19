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

import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.generic.LicenseHeaderStep;

public class LicenseHeaderStepTest extends ResourceHarness {
	private static final String KEY_LICENSE = "license/TestLicense";
	private static final String KEY_FILE_NOTAPPLIED = "license/MissingLicense.test";
	private static final String KEY_FILE_APPLIED = "license/HasLicense.test";

	@Test
	public void fromString() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromHeader(getTestResource(KEY_LICENSE), JavaExtension.LICENSE_HEADER_DELIMITER);
		assertOnResources(step, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void fromFile() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromFile(createTestFile(KEY_LICENSE), StandardCharsets.UTF_8, JavaExtension.LICENSE_HEADER_DELIMITER);
		assertOnResources(step, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void efficient() throws Throwable {
		LicenseHeaderStep step = new LicenseHeaderStep("LicenseHeader\n", "contentstart");
		String alreadyCorrect = "LicenseHeader\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect));
		// If no change is required, it should return the exact same string for efficiency reasons
		Assert.assertSame(alreadyCorrect, step.format(alreadyCorrect));
	}

	@Test
	public void sanitized() throws Throwable {
		// The sanitizer should add a \n
		LicenseHeaderStep step = new LicenseHeaderStep("LicenseHeader", "contentstart");
		String alreadyCorrect = "LicenseHeader\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect));
		Assert.assertSame(alreadyCorrect, step.format(alreadyCorrect));
	}

	@Test
	public void sanitizerDoesntGoTooFar() throws Throwable {
		// if the user wants extra lines after the header, we shouldn't clobber them
		LicenseHeaderStep step = new LicenseHeaderStep("LicenseHeader\n\n", "contentstart");
		String alreadyCorrect = "LicenseHeader\n\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect));
		Assert.assertSame(alreadyCorrect, step.format(alreadyCorrect));
	}
}
