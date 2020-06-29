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
package com.diffplug.spotless.generic;

import java.io.File;
import java.io.IOException;
import java.time.YearMonth;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.generic.LicenseHeaderStep.YearMode;

public class LicenseHeaderStepTest extends ResourceHarness {
	private static final String KEY_LICENSE = "license/TestLicense";
	private static final String KEY_FILE_NOTAPPLIED = "license/MissingLicense.test";
	private static final String KEY_FILE_APPLIED = "license/HasLicense.test";

	private static final String KEY_FILE_WITHOUT_LICENSE = "license/FileWithoutLicenseHeader.test";
	// Templates to test with custom license contents
	private static final String KEY_LICENSE_WITH_PLACEHOLDER = "license/LicenseHeaderWithPlaceholder";
	private static final String KEY_FILE_WITH_LICENSE_AND_PLACEHOLDER = "license/FileWithLicenseHeaderAndPlaceholder.test";
	// Licenses to test $YEAR token replacement
	private static final String HEADER_WITH_YEAR = "This is a fake license, $YEAR. ACME corp.";
	// License to test $today.year token replacement
	private static final String HEADER_WITH_YEAR_INTELLIJ = "This is a fake license, $today.year. ACME corp.";
	// Special case where the characters immediately before and after the year token are the same,
	// start position of the second part might overlap the end position of the first part.
	private static final String HEADER_WITH_YEAR_VARIANT = "This is a fake license. Copyright $YEAR ACME corp.";

	// If this constant changes, don't forget to change the similarly-named one in
	// plugin-gradle/src/main/java/com/diffplug/gradle/spotless/JavaExtension.java as well
	private static final String LICENSE_HEADER_DELIMITER = "package ";

	@Test
	public void fromHeader() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(getTestResource(KEY_LICENSE), LICENSE_HEADER_DELIMITER).build();
		assertOnResources(step, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void should_apply_license_containing_YEAR_token() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(licenseWith(HEADER_WITH_YEAR), LICENSE_HEADER_DELIMITER).build())
				.test(getTestResource(KEY_FILE_WITHOUT_LICENSE), fileContainingYear(HEADER_WITH_YEAR, currentYear()))
				.testUnaffected(fileContainingYear(HEADER_WITH_YEAR, currentYear()))
				.testUnaffected(fileContainingYear(HEADER_WITH_YEAR, "2003"))
				.testUnaffected(fileContainingYear(HEADER_WITH_YEAR, "1990-2015"))
				.test(fileContainingYear("Something before license.*/\n/* \n * " + HEADER_WITH_YEAR, "2003"), fileContainingYear(HEADER_WITH_YEAR, currentYear()))
				.test(fileContainingYear(HEADER_WITH_YEAR + "\n **/\n/* Something after license.", "2003"), fileContainingYear(HEADER_WITH_YEAR, "2003"))
				.test(fileContainingYear(HEADER_WITH_YEAR, "not a year"), fileContainingYear(HEADER_WITH_YEAR, currentYear()));
		// Check with variant
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(licenseWith(HEADER_WITH_YEAR_VARIANT), LICENSE_HEADER_DELIMITER).build())
				.test(getTestResource(KEY_FILE_WITHOUT_LICENSE), fileContainingYear(HEADER_WITH_YEAR_VARIANT, currentYear()))
				.testUnaffected(fileContainingYear(HEADER_WITH_YEAR_VARIANT, currentYear()))
				.test(fileContaining("This is a fake license. Copyright "), fileContainingYear(HEADER_WITH_YEAR_VARIANT, currentYear()))
				.test(fileContaining(" ACME corp."), fileContainingYear(HEADER_WITH_YEAR_VARIANT, currentYear()))
				.test(fileContaining("This is a fake license. Copyright ACME corp."), fileContainingYear(HEADER_WITH_YEAR_VARIANT, currentYear()))
				.test(fileContaining("This is a fake license. CopyrightACME corp."), fileContainingYear(HEADER_WITH_YEAR_VARIANT, currentYear()));

		//Check when token is of the format $today.year
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(licenseWith(HEADER_WITH_YEAR_INTELLIJ), LICENSE_HEADER_DELIMITER).build())
				.test(fileContaining(HEADER_WITH_YEAR_INTELLIJ), fileWithLicenseContaining(HEADER_WITH_YEAR_INTELLIJ, currentYear(), "$today.year"));
	}

	private String fileWithLicenseContaining(String license, String yearContent, String token) throws IOException {
		return getTestResource(KEY_FILE_WITH_LICENSE_AND_PLACEHOLDER).replace("__LICENSE_PLACEHOLDER__", license).replace(token, yearContent);
	}

	@Test
	public void updateYearWithLatest() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(licenseWith(HEADER_WITH_YEAR), LICENSE_HEADER_DELIMITER)
				.withYearMode(YearMode.UPDATE_TO_TODAY)
				.build();
		StepHarness.forStep(step)
				.testUnaffected(fileContainingYear(HEADER_WITH_YEAR, currentYear()))
				.test(fileContainingYear(HEADER_WITH_YEAR, "2003"), fileContainingYear(HEADER_WITH_YEAR, "2003-" + currentYear()))
				.test(fileContainingYear(HEADER_WITH_YEAR, "1990-2015"), fileContainingYear(HEADER_WITH_YEAR, "1990-" + currentYear()));
	}

	@Test
	public void should_apply_license_containing_YEAR_token_with_non_default_year_separator() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(licenseWith(HEADER_WITH_YEAR), LICENSE_HEADER_DELIMITER).withYearSeparator(", ").build())
				.testUnaffected(fileContainingYear(HEADER_WITH_YEAR, "1990, 2015"))
				.test(fileContainingYear(HEADER_WITH_YEAR, "1990-2015"), fileContainingYear(HEADER_WITH_YEAR, "1990, 2015"));
	}

	@Test
	public void should_apply_license_containing_YEAR_token_with_special_character_in_year_separator() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(licenseWith(HEADER_WITH_YEAR), LICENSE_HEADER_DELIMITER).withYearSeparator("(").build())
				.testUnaffected(fileContainingYear(HEADER_WITH_YEAR, "1990(2015"))
				.test(fileContainingYear(HEADER_WITH_YEAR, "1990-2015"), fileContainingYear(HEADER_WITH_YEAR, "1990(2015"));
	}

	@Test
	public void should_apply_license_containing_YEAR_token_with_custom_separator() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(licenseWith(HEADER_WITH_YEAR), LICENSE_HEADER_DELIMITER).build())
				.test(getTestResource(KEY_FILE_WITHOUT_LICENSE), fileContainingYear(HEADER_WITH_YEAR, currentYear()))
				.testUnaffected(fileContainingYear(HEADER_WITH_YEAR, currentYear()))
				.testUnaffected(fileContainingYear(HEADER_WITH_YEAR, "2003"))
				.testUnaffected(fileContainingYear(HEADER_WITH_YEAR, "1990-2015"))
				.test(fileContainingYear(HEADER_WITH_YEAR, "not a year"), fileContainingYear(HEADER_WITH_YEAR, currentYear()));
	}

	private String licenseWith(String contents) throws IOException {
		return getTestResource(KEY_LICENSE_WITH_PLACEHOLDER).replace("__LICENSE_PLACEHOLDER__", contents);
	}

	private String fileContaining(String license) throws IOException {
		return fileContainingYear(license, "");
	}

	private String fileContainingYear(String license, String yearContent) throws IOException {
		return getTestResource(KEY_FILE_WITH_LICENSE_AND_PLACEHOLDER).replace("__LICENSE_PLACEHOLDER__", license).replace("$YEAR", yearContent);
	}

	private String currentYear() {
		return String.valueOf(YearMonth.now().getYear());
	}

	@Test
	public void efficient() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter("LicenseHeader\n", "contentstart").build();
		String alreadyCorrect = "LicenseHeader\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		// If no change is required, it should return the exact same string for efficiency reasons
		Assert.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	public void sanitized() throws Throwable {
		// The sanitizer should add a \n
		FormatterStep step = LicenseHeaderStep.headerDelimiter("LicenseHeader", "contentstart").build();
		String alreadyCorrect = "LicenseHeader\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		Assert.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	public void sanitizerDoesntGoTooFar() throws Throwable {
		// if the user wants extra lines after the header, we shouldn't clobber them
		FormatterStep step = LicenseHeaderStep.headerDelimiter("LicenseHeader\n\n", "contentstart").build();
		String alreadyCorrect = "LicenseHeader\n\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		Assert.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	public void equality() {
		new SerializableEqualityTester() {
			LicenseHeaderStep.Builder builder = LicenseHeaderStep.headerDelimiter("LICENSE", "package")
					.withYearSeparator("-")
					.withYearMode(YearMode.PRESERVE);

			@Override
			protected void setupTest(API api) {
				api.areDifferentThan();

				builder = builder.withDelimiter("crate");
				api.areDifferentThan();

				builder = builder.withHeaderString("APACHE $YEAR");
				api.areDifferentThan();

				builder = builder.withDelimiter("package");
				api.areDifferentThan();

				builder = builder.withYearSeparator(" - ");
				api.areDifferentThan();

				builder = builder.withYearMode(YearMode.UPDATE_TO_TODAY);
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return builder.build();
			}
		}.testEquals();
	}
}
