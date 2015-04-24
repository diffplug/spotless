package com.github.youribonnaffe.gradle.format;

import org.junit.Assert;
import org.junit.Test;

public class LicenseEnforcerTest extends ResourceTest {
	@Test
	public void enforceNoop() throws Exception {
		FormatterStep enforcer = LicenseEnforcer.load(null, null);
		Assert.assertTrue(enforcer.isClean("asklgbslkbd"));
	}

	private static final String KEY_LICENSE = "TestLicense";
	private static final String KEY_FILE_NOTAPPLIED = "Java8CodeFormatted.test";
	private static final String KEY_FILE_APPLIED = "JavaCodeFormattedWithLicense.test";

	@Test
	public void enforceString() throws Exception {
		FormatterStep enforcer = LicenseEnforcer.load(getTestResource(KEY_LICENSE), null);
		Assert.assertFalse(enforcer.isClean(getTestResource(KEY_FILE_NOTAPPLIED)));
		assertContent(KEY_FILE_APPLIED, enforcer.format(getTestResource(KEY_FILE_NOTAPPLIED)));
	}

	@Test
	public void enforceFile() throws Exception {
		FormatterStep enforcer = LicenseEnforcer.load(null, getTestFile(KEY_LICENSE));
		Assert.assertFalse(enforcer.isClean(getTestResource(KEY_FILE_NOTAPPLIED)));
		assertContent(KEY_FILE_APPLIED, enforcer.format(getTestResource(KEY_FILE_NOTAPPLIED)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void enforceError() throws Exception {
		FormatterStep enforcer = LicenseEnforcer.load(getTestResource(KEY_LICENSE), getTestFile(KEY_LICENSE));
		Assert.assertFalse(enforcer.isClean(getTestResource(KEY_FILE_NOTAPPLIED)));
		assertContent(KEY_FILE_APPLIED, enforcer.format(getTestResource(KEY_FILE_NOTAPPLIED)));
	}
}
