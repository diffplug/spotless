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
	private static final String FILE_NO_LICENSE = "license/FileWithoutLicenseHeader.test";
	private static final String package_ = "package ";
	private static final String HEADER_WITH_$YEAR = "This is a fake license, $YEAR. ACME corp.";

	@Test
	public void parseExistingYear() throws Exception {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR), package_).build())
				// has existing
				.test(hasHeader("This is a fake license, 2007. ACME corp."), hasHeader("This is a fake license, 2007. ACME corp."))
				// if prefix changes, the year will get set to today
				.test(hasHeader("This is a license, 2007. ACME corp."), hasHeader("This is a fake license, " + currentYear() + ". ACME corp."))
				// if suffix changes, the year will get set to today
				.test(hasHeader("This is a fake license, 2007. Other corp."), hasHeader("This is a fake license, " + currentYear() + ". ACME corp."));
	}

	@Test
	public void fromHeader() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(getTestResource("license/TestLicense"), package_).build();
		StepHarness.forStep(step)
				.testResource("license/MissingLicense.test", "license/HasLicense.test");
	}

	@Test
	public void should_apply_license_containing_YEAR_token() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR), package_).build())
				.test(getTestResource(FILE_NO_LICENSE), hasHeaderYear(currentYear()))
				.testUnaffected(hasHeaderYear(currentYear()))
				.testUnaffected(hasHeaderYear("2003"))
				.testUnaffected(hasHeaderYear("1990-2015"))
				.test(hasHeaderYear("Something before license.*/\n/* \n * " + HEADER_WITH_$YEAR, "2003"), hasHeaderYear(currentYear()))
				.test(hasHeaderYear(HEADER_WITH_$YEAR + "\n **/\n/* Something after license.", "2003"), hasHeaderYear("2003"))
				.test(hasHeaderYear("not a year"), hasHeaderYear(currentYear()));
		// Check with variant
		String otherFakeLicense = "This is a fake license. Copyright $YEAR ACME corp.";
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(otherFakeLicense), package_).build())
				.test(getTestResource(FILE_NO_LICENSE), hasHeaderYear(otherFakeLicense, currentYear()))
				.testUnaffected(hasHeaderYear(otherFakeLicense, currentYear()))
				.test(hasHeader("This is a fake license. Copyright "), hasHeaderYear(otherFakeLicense, currentYear()))
				.test(hasHeader(" ACME corp."), hasHeaderYear(otherFakeLicense, currentYear()))
				.test(hasHeader("This is a fake license. Copyright ACME corp."), hasHeaderYear(otherFakeLicense, currentYear()))
				.test(hasHeader("This is a fake license. CopyrightACME corp."), hasHeaderYear(otherFakeLicense, currentYear()));

		//Check when token is of the format $today.year
		String HEADER_WITH_YEAR_INTELLIJ = "This is a fake license, $today.year. ACME corp.";
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_YEAR_INTELLIJ), package_).build())
				.test(hasHeader(HEADER_WITH_YEAR_INTELLIJ), hasHeader(HEADER_WITH_YEAR_INTELLIJ.replace("$today.year", currentYear())));
	}

	@Test
	public void updateYearWithLatest() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR), package_)
				.withYearMode(YearMode.UPDATE_TO_TODAY)
				.build();
		StepHarness.forStep(step)
				.testUnaffected(hasHeaderYear(currentYear()))
				.test(hasHeaderYear("2003"), hasHeaderYear("2003-" + currentYear()))
				.test(hasHeaderYear("1990-2015"), hasHeaderYear("1990-" + currentYear()));
	}

	@Test
	public void should_apply_license_containing_YEAR_token_with_non_default_year_separator() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR), package_).withYearSeparator(", ").build())
				.testUnaffected(hasHeaderYear("1990, 2015"))
				.test(hasHeaderYear("1990-2015"), hasHeaderYear("1990, 2015"));
	}

	@Test
	public void should_apply_license_containing_YEAR_token_with_special_character_in_year_separator() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR), package_).withYearSeparator("(").build())
				.testUnaffected(hasHeaderYear("1990(2015"))
				.test(hasHeaderYear("1990-2015"), hasHeaderYear("1990(2015"));
	}

	@Test
	public void should_apply_license_containing_YEAR_token_with_custom_separator() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR), package_).build())
				.test(getTestResource(FILE_NO_LICENSE), hasHeaderYear(currentYear()))
				.testUnaffected(hasHeaderYear(currentYear()))
				.testUnaffected(hasHeaderYear("2003"))
				.testUnaffected(hasHeaderYear("1990-2015"))
				.test(hasHeaderYear("not a year"), hasHeaderYear(currentYear()));
	}

	private String header(String contents) throws IOException {
		return "/*\n" +
				" * " + contents + "\n" +
				" **/\n";
	}

	private String hasHeader(String header) throws IOException {
		return header(header) + getTestResource(FILE_NO_LICENSE);
	}

	private String hasHeaderYear(String license, String years) throws IOException {
		return header(license).replace("$YEAR", years) + getTestResource(FILE_NO_LICENSE);
	}

	private String hasHeaderYear(String years) throws IOException {
		return hasHeaderYear(HEADER_WITH_$YEAR, years);
	}

	private static String currentYear() {
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
			LicenseHeaderStep builder = LicenseHeaderStep.headerDelimiter("LICENSE", "package")
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
