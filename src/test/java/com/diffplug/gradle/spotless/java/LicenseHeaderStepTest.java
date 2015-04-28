package com.diffplug.gradle.spotless.java;

import org.gradle.api.GradleException;
import org.junit.Test;

import com.diffplug.gradle.spotless.ResourceTest;

public class LicenseHeaderStepTest extends ResourceTest {
	private static final String KEY_LICENSE = "TestLicense";
	private static final String KEY_FILE_NOTAPPLIED = "Java8CodeFormatted.test";
	private static final String KEY_FILE_APPLIED = "JavaCodeFormattedWithLicense.test";

	@Test
	public void fromString() throws Exception {
		LicenseHeaderStep step = LicenseHeaderStep.load(getTestResource(KEY_LICENSE), null).get();
		super.assertStep(step, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void fromFile() throws Exception {
		LicenseHeaderStep step = LicenseHeaderStep.load(null, createTestFile(KEY_LICENSE)).get();
		super.assertStep(step, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test(expected = GradleException.class)
	public void doubleSpecified() throws Exception {
		LicenseHeaderStep.load(getTestResource(KEY_LICENSE), createTestFile(KEY_LICENSE));
	}
}
