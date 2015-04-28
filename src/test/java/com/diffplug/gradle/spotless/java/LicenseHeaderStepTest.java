package com.diffplug.gradle.spotless.java;

import org.junit.Test;

import com.diffplug.gradle.spotless.ResourceTest;

public class LicenseHeaderStepTest extends ResourceTest {
	private static final String KEY_LICENSE = "TestLicense";
	private static final String KEY_FILE_NOTAPPLIED = "Java8CodeFormatted.test";
	private static final String KEY_FILE_APPLIED = "JavaCodeFormattedWithLicense.test";

	@Test
	public void fromString() throws Throwable {
		LicenseHeaderStep step = new LicenseHeaderStep(getTestResource(KEY_LICENSE));
		assertStep(step::format, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void fromFile() throws Throwable {
		LicenseHeaderStep step = new LicenseHeaderStep(createTestFile(KEY_LICENSE));
		assertStep(step::format, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}
}
