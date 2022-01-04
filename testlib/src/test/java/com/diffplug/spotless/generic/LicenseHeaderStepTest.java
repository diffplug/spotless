/*
 * Copyright 2016-2022 DiffPlug
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.generic.LicenseHeaderStep.YearMode;

class LicenseHeaderStepTest extends ResourceHarness {
	private static final String FILE_NO_LICENSE = "license/FileWithoutLicenseHeader.test";
	private static final String package_ = "package ";
	private static final String HEADER_WITH_$YEAR = "This is a fake license, $YEAR. ACME corp.";
	private static final String HEADER_WITH_RANGE_TO_$YEAR = "This is a fake license with range, 2009-$YEAR. ACME corp.";

	@Test
	void parseExistingYear() throws Exception {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR), package_).build())
				// has existing
				.test(hasHeader("This is a fake license, 2007. ACME corp."), hasHeader("This is a fake license, 2007. ACME corp."))
				// if prefix changes, the year will get set to today
				.test(hasHeader("This is a license, 2007. ACME corp."), hasHeader("This is a fake license, 2007. ACME corp."))
				// if suffix changes, the year will get set to today
				.test(hasHeader("This is a fake license, 2007. Other corp."), hasHeader("This is a fake license, 2007. ACME corp."));
	}

	@Test
	void fromHeader() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(getTestResource("license/TestLicense"), package_).build();
		StepHarness.forStep(step)
				.testResource("license/MissingLicense.test", "license/HasLicense.test");
	}

	@Test
	void should_apply_license_containing_YEAR_token() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR), package_).build())
				.test(getTestResource(FILE_NO_LICENSE), hasHeaderYear(currentYear()))
				.testUnaffected(hasHeaderYear(currentYear()))
				.testUnaffected(hasHeaderYear("2003"))
				.testUnaffected(hasHeaderYear("1990-2015"))
				.test(hasHeaderYear("Something before license.*/\n/* \n * " + HEADER_WITH_$YEAR, "2003"), hasHeaderYear("2003"))
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
	void updateYearWithLatest() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR), package_)
				.withYearMode(YearMode.UPDATE_TO_TODAY)
				.build();
		StepHarness.forStep(step)
				.testUnaffected(hasHeaderYear(currentYear()))
				.test(hasHeaderYear("2003"), hasHeaderYear("2003-" + currentYear()))
				.test(hasHeaderYear("1990-2015"), hasHeaderYear("1990-" + currentYear()));
	}

	@Test
	void should_apply_license_containing_YEAR_token_with_non_default_year_separator() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR), package_).withYearSeparator(", ").build())
				.testUnaffected(hasHeaderYear("1990, 2015"))
				.test(hasHeaderYear("1990-2015"), hasHeaderYear("1990, 2015"));
	}

	@Test
	void should_apply_license_containing_YEAR_token_with_special_character_in_year_separator() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR), package_).withYearSeparator("(").build())
				.testUnaffected(hasHeaderYear("1990(2015"))
				.test(hasHeaderYear("1990-2015"), hasHeaderYear("1990(2015"));
	}

	@Test
	void should_apply_license_containing_YEAR_token_with_custom_separator() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR), package_).build())
				.test(getTestResource(FILE_NO_LICENSE), hasHeaderYear(currentYear()))
				.testUnaffected(hasHeaderYear(currentYear()))
				.testUnaffected(hasHeaderYear("2003"))
				.testUnaffected(hasHeaderYear("1990-2015"))
				.test(hasHeaderYear("not a year"), hasHeaderYear(currentYear()));
	}

	@Test
	void should_remove_header_when_empty() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter("", package_).build())
				.testUnaffected(getTestResource("license/MissingLicense.test"))
				.test(getTestResource("license/HasLicense.test"), getTestResource("license/MissingLicense.test"));
	}

	private String licenceWithAddress() {
		return "Copyright &#169; $YEAR FooBar Inc. All Rights Reserved.\n" +
				" *\n" +
				" * Use of this software is covered by inscrutable legal protection and\n" +
				" * complex automation. Violaters of undisclosed terms must expect\n" +
				" * unforeseen consequences.\n" +
				" *\n" +
				" * FooBar, Inc.\n" +
				" * 9 Food Truck\n" +
				" * Perry Derry, TX 55656 USA";
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

	private String hasHeaderWithRangeAndWithYearTo(String toYear) throws IOException {
		return hasHeaderYear(HEADER_WITH_RANGE_TO_$YEAR, toYear);
	}

	private static String currentYear() {
		return String.valueOf(YearMonth.now().getYear());
	}

	@Test
	void efficient() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter("LicenseHeader\n", "contentstart").build();
		String alreadyCorrect = "LicenseHeader\ncontentstart";
		Assertions.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		// If no change is required, it should return the exact same string for efficiency reasons
		Assertions.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	void sanitized() throws Throwable {
		// The sanitizer should add a \n
		FormatterStep step = LicenseHeaderStep.headerDelimiter("LicenseHeader", "contentstart").build();
		String alreadyCorrect = "LicenseHeader\ncontentstart";
		Assertions.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		Assertions.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	void sanitizerDoesntGoTooFar() throws Throwable {
		// if the user wants extra lines after the header, we shouldn't clobber them
		FormatterStep step = LicenseHeaderStep.headerDelimiter("LicenseHeader\n\n", "contentstart").build();
		String alreadyCorrect = "LicenseHeader\n\ncontentstart";
		Assertions.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		Assertions.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	void equality() {
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

	@Test
	void should_apply_license_containing_YEAR_token_in_range() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_RANGE_TO_$YEAR), package_).withYearMode(YearMode.UPDATE_TO_TODAY).build();
		StepHarness.forStep(step).test(hasHeaderWithRangeAndWithYearTo("2015"), hasHeaderWithRangeAndWithYearTo(currentYear()));
	}

	@Test
	void should_update_year_for_license_with_address() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header(licenceWithAddress()), package_).withYearMode(YearMode.UPDATE_TO_TODAY).build();
		StepHarness.forStep(step).test(
				hasHeader(licenceWithAddress().replace("$YEAR", "2015")),
				hasHeader(licenceWithAddress().replace("$YEAR", "2015-2022")));
	}

	@Test
	void should_preserve_year_for_license_with_address() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header(licenceWithAddress()), package_).withYearMode(YearMode.PRESERVE).build();
		StepHarness.forStep(step).test(
				hasHeader(licenceWithAddress().replace("$YEAR", "2015").replace("FooBar Inc. All", "FooBar Inc.  All")),
				hasHeader(licenceWithAddress().replace("$YEAR", "2015")));
	}
}
