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
		LicenseHeaderStep step = new LicenseHeaderStep(getTestResource(KEY_LICENSE), JavaExtension.LICENSE_HEADER_DELIMITER);
		assertStep(step::format, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void fromFile() throws Throwable {
		LicenseHeaderStep step = new LicenseHeaderStep(createTestFile(KEY_LICENSE), JavaExtension.LICENSE_HEADER_DELIMITER);
		assertStep(step::format, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void efficient() throws Throwable {
		LicenseHeaderStep step = new LicenseHeaderStep("LicenseHeader\n", "contentstart");
		String alreadyCorrect = "LicenseHeader\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect));
		Assert.assertTrue(alreadyCorrect == step.format(alreadyCorrect));
	}
}
