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
package com.diffplug.spotless.generic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;

public class LicenseHeaderStepTest extends ResourceHarness {
	private static final String KEY_LICENSE = "license/TestLicense";
	private static final String KEY_FILE_NOTAPPLIED = "license/MissingLicense.test";
	private static final String KEY_FILE_APPLIED = "license/HasLicense.test";

	private static final String KEY_FILE_WITHOUT_LICENSE = "license/FileWithoutLicenseHeader.test";
	// Templates to test with custom license contents
	private static final String KEY_LICENSE_WITH_PLACEHOLDER = "license/LicenseHeaderWithPlaceholder";
	private static final String KEY_FILE_WITH_LICENSE_AND_PLACEHOLDER = "license/FileWithLicenseHeaderAndPlaceholder.test";
	// Licenses to test $YEAR token replacement
	private static final String LICENSE_HEADER_YEAR = "This is a fake license, $YEAR. ACME corp.";
	// License to test $today.year token replacement
	private static final String LICENSE_HEADER_YEAR_INTELLIJ_TOKEN = "This is a fake license, $today.year. ACME corp.";
	// Special case where the characters immediately before and after the year token are the same,
	// start position of the second part might overlap the end position of the first part.
	private static final String LICENSE_HEADER_YEAR_VARIANT = "This is a fake license. Copyright $YEAR ACME corp.";

	// If this constant changes, don't forget to change the similarly-named one in
	// plugin-gradle/src/main/java/com/diffplug/gradle/spotless/JavaExtension.java as well
	private static final String LICENSE_HEADER_DELIMITER = "package ";

	@Test
	public void fromHeader() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromHeader(getTestResource(KEY_LICENSE), LICENSE_HEADER_DELIMITER);
		assertOnResources(step, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void fromFile() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromFile(createTestFile(KEY_LICENSE), StandardCharsets.UTF_8, LICENSE_HEADER_DELIMITER);
		assertOnResources(step, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void should_apply_license_containing_YEAR_token() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromFile(createLicenseWith(LICENSE_HEADER_YEAR), StandardCharsets.UTF_8, LICENSE_HEADER_DELIMITER);

		StepHarness.forStep(step)
				.test(getTestResource(KEY_FILE_WITHOUT_LICENSE), fileWithLicenseContaining(LICENSE_HEADER_YEAR, currentYear()))
				.testUnaffected(fileWithLicenseContaining(LICENSE_HEADER_YEAR, currentYear()))
				.testUnaffected(fileWithLicenseContaining(LICENSE_HEADER_YEAR, "2003"))
				.testUnaffected(fileWithLicenseContaining(LICENSE_HEADER_YEAR, "1990-2015"))
				.test(fileWithLicenseContaining("Something before license.*/\n/* \n * " + LICENSE_HEADER_YEAR, "2003"), fileWithLicenseContaining(LICENSE_HEADER_YEAR, currentYear()))
				.test(fileWithLicenseContaining(LICENSE_HEADER_YEAR + "\n **/\n/* Something after license.", "2003"), fileWithLicenseContaining(LICENSE_HEADER_YEAR, "2003"))
				.test(fileWithLicenseContaining(LICENSE_HEADER_YEAR, "not a year"), fileWithLicenseContaining(LICENSE_HEADER_YEAR, currentYear()));

		// Check with variant
		step = LicenseHeaderStep.createFromFile(createLicenseWith(LICENSE_HEADER_YEAR_VARIANT), StandardCharsets.UTF_8, LICENSE_HEADER_DELIMITER);

		StepHarness.forStep(step)
				.test(getTestResource(KEY_FILE_WITHOUT_LICENSE), fileWithLicenseContaining(LICENSE_HEADER_YEAR_VARIANT, currentYear()))
				.testUnaffected(fileWithLicenseContaining(LICENSE_HEADER_YEAR_VARIANT, currentYear()))
				.test(fileWithLicenseContaining("This is a fake license. Copyright "), fileWithLicenseContaining(LICENSE_HEADER_YEAR_VARIANT, currentYear()))
				.test(fileWithLicenseContaining(" ACME corp."), fileWithLicenseContaining(LICENSE_HEADER_YEAR_VARIANT, currentYear()))
				.test(fileWithLicenseContaining("This is a fake license. Copyright ACME corp."), fileWithLicenseContaining(LICENSE_HEADER_YEAR_VARIANT, currentYear()))
				.test(fileWithLicenseContaining("This is a fake license. CopyrightACME corp."), fileWithLicenseContaining(LICENSE_HEADER_YEAR_VARIANT, currentYear()));

		//Check when token is of the format $today.year
		step = LicenseHeaderStep.createFromFile(createLicenseWith(LICENSE_HEADER_YEAR_INTELLIJ_TOKEN), StandardCharsets.UTF_8, LICENSE_HEADER_DELIMITER);

		StepHarness.forStep(step)
				.test(fileWithLicenseContaining(LICENSE_HEADER_YEAR_INTELLIJ_TOKEN), fileWithLicenseContaining(LICENSE_HEADER_YEAR_INTELLIJ_TOKEN, currentYear(), "$today.year"));
	}

	@Test
	public void overwriteYearWithLatest() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromFile(createLicenseWith(LICENSE_HEADER_YEAR), StandardCharsets.UTF_8, LICENSE_HEADER_DELIMITER, "-", true);
		StepHarness.forStep(step)
				.testUnaffected(fileWithLicenseContaining(LICENSE_HEADER_YEAR, currentYear()))
				.test(fileWithLicenseContaining(LICENSE_HEADER_YEAR, "2003"), fileWithLicenseContaining(LICENSE_HEADER_YEAR, "2003-" + currentYear()))
				.test(fileWithLicenseContaining(LICENSE_HEADER_YEAR, "1990-2015"), fileWithLicenseContaining(LICENSE_HEADER_YEAR, "1990-" + currentYear()));
	}

	@Test
	public void should_apply_license_containing_YEAR_token_with_non_default_year_separator() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromFile(createLicenseWith(LICENSE_HEADER_YEAR), StandardCharsets.UTF_8, LICENSE_HEADER_DELIMITER, ", ");

		StepHarness.forStep(step)
				.testUnaffected(fileWithLicenseContaining(LICENSE_HEADER_YEAR, "1990, 2015"))
				.test(fileWithLicenseContaining(LICENSE_HEADER_YEAR, "1990-2015"), fileWithLicenseContaining(LICENSE_HEADER_YEAR, "1990, 2015"));
	}

	@Test
	public void should_apply_license_containing_YEAR_token_with_special_character_in_year_separator() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromFile(createLicenseWith(LICENSE_HEADER_YEAR), StandardCharsets.UTF_8, LICENSE_HEADER_DELIMITER, "(");

		StepHarness.forStep(step)
				.testUnaffected(fileWithLicenseContaining(LICENSE_HEADER_YEAR, "1990(2015"))
				.test(fileWithLicenseContaining(LICENSE_HEADER_YEAR, "1990-2015"), fileWithLicenseContaining(LICENSE_HEADER_YEAR, "1990(2015"));
	}

	@Test
	public void should_apply_license_containing_YEAR_token_with_custom_separator() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromFile(createLicenseWith(LICENSE_HEADER_YEAR), StandardCharsets.UTF_8, LICENSE_HEADER_DELIMITER);

		StepHarness.forStep(step)
				.test(getTestResource(KEY_FILE_WITHOUT_LICENSE), fileWithLicenseContaining(LICENSE_HEADER_YEAR, currentYear()))
				.testUnaffected(fileWithLicenseContaining(LICENSE_HEADER_YEAR, currentYear()))
				.testUnaffected(fileWithLicenseContaining(LICENSE_HEADER_YEAR, "2003"))
				.testUnaffected(fileWithLicenseContaining(LICENSE_HEADER_YEAR, "1990-2015"))
				.test(fileWithLicenseContaining(LICENSE_HEADER_YEAR, "not a year"), fileWithLicenseContaining(LICENSE_HEADER_YEAR, currentYear()));
	}

	private File createLicenseWith(String contents) throws IOException {
		return createTestFile(KEY_LICENSE_WITH_PLACEHOLDER, c -> c.replace("__LICENSE_PLACEHOLDER__", contents));
	}

	private String fileWithLicenseContaining(String license) throws IOException {
		return fileWithLicenseContaining(license, "");
	}

	private String fileWithLicenseContaining(String license, String yearContent) throws IOException {
		return getTestResource(KEY_FILE_WITH_LICENSE_AND_PLACEHOLDER).replace("__LICENSE_PLACEHOLDER__", license).replace("$YEAR", yearContent);
	}

	private String fileWithLicenseContaining(String license, String yearContent, String token) throws IOException {
		return getTestResource(KEY_FILE_WITH_LICENSE_AND_PLACEHOLDER).replace("__LICENSE_PLACEHOLDER__", license).replace(token, yearContent);
	}

	private String currentYear() {
		return String.valueOf(YearMonth.now().getYear());
	}

	@Test
	public void efficient() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromHeader("LicenseHeader\n", "contentstart");
		String alreadyCorrect = "LicenseHeader\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		// If no change is required, it should return the exact same string for efficiency reasons
		Assert.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	public void sanitized() throws Throwable {
		// The sanitizer should add a \n
		FormatterStep step = LicenseHeaderStep.createFromHeader("LicenseHeader", "contentstart");
		String alreadyCorrect = "LicenseHeader\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		Assert.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	public void sanitizerDoesntGoTooFar() throws Throwable {
		// if the user wants extra lines after the header, we shouldn't clobber them
		FormatterStep step = LicenseHeaderStep.createFromHeader("LicenseHeader\n\n", "contentstart");
		String alreadyCorrect = "LicenseHeader\n\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		Assert.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	public void equality() {
		new SerializableEqualityTester() {
			String header = "LICENSE";
			String delimiter = "package";

			@Override
			protected void setupTest(API api) {
				api.areDifferentThan();

				delimiter = "crate";
				api.areDifferentThan();

				header = "APACHE";
				api.areDifferentThan();

				delimiter = "package";
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return LicenseHeaderStep.createFromHeader(header, delimiter);
			}
		}.testEquals();
	}
}
