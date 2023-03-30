/*
 * Copyright 2016-2023 DiffPlug
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.generic.LicenseHeaderStep.YearMode;

class LicenseHeaderStepTest extends ResourceHarness {
	private static final String FILE_NO_LICENSE = "license/FileWithoutLicenseHeader.test";
	private static final String package_ = LicenseHeaderStep.DEFAULT_JAVA_HEADER_DELIMITER;
	private static final String HEADER_WITH_$YEAR = "This is a fake license, $YEAR. ACME corp.";
	private static final String HEADER_WITH_RANGE_TO_$YEAR = "This is a fake license with range, 2009-$YEAR. ACME corp.";
	private static final String HEADER_WITH_$FILE = "This is a fake license, $FILE. ACME corp.";
	private static final String HEADER_WITH_$YEAR_$FILE = "This is a fake license, $FILE, $YEAR. ACME corp.";

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
		var otherFakeLicense = "This is a fake license. Copyright $YEAR ACME corp.";
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter(header(otherFakeLicense), package_).build())
				.test(getTestResource(FILE_NO_LICENSE), hasHeaderYear(otherFakeLicense, currentYear()))
				.testUnaffected(hasHeaderYear(otherFakeLicense, currentYear()))
				.test(hasHeader("This is a fake license. Copyright "), hasHeaderYear(otherFakeLicense, currentYear()))
				.test(hasHeader(" ACME corp."), hasHeaderYear(otherFakeLicense, currentYear()))
				.test(hasHeader("This is a fake license. Copyright ACME corp."), hasHeaderYear(otherFakeLicense, currentYear()))
				.test(hasHeader("This is a fake license. CopyrightACME corp."), hasHeaderYear(otherFakeLicense, currentYear()));

		//Check when token is of the format $today.year
		var HEADER_WITH_YEAR_INTELLIJ = "This is a fake license, $today.year. ACME corp.";
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

	@Test
	void should_skip_lines_matching_predefined_pattern() throws Throwable {
		StepHarness.forStep(LicenseHeaderStep.headerDelimiter("<!--\n  -- This is a fake license header.\n  -- All rights reserved.\n  -->", "^(?!<!--|\\s+--).*$")
				.withSkipLinesMatching("(?i)^(<\\?xml[^>]+>|<!doctype[^>]+>)$").build())
				.testResource("license/SkipLines.test", "license/SkipLinesHasLicense.test");
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

	private String hasHeaderFileName(String license, String fileName) throws IOException {
		return header(license).replace("$FILE", fileName) + getTestResource(FILE_NO_LICENSE);
	}

	private String hasHeaderYearFileName(String license, String year, String fileName) throws IOException {
		return header(license)
				.replace("$YEAR", year)
				.replace("$FILE", fileName) + getTestResource(FILE_NO_LICENSE);
	}

	private static String currentYear() {
		return String.valueOf(YearMonth.now().getYear());
	}

	@Test
	void efficient() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter("LicenseHeader\n", "contentstart").build();
		var alreadyCorrect = "LicenseHeader\ncontentstart";
		Assertions.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		// If no change is required, it should return the exact same string for efficiency reasons
		Assertions.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	void sanitized() throws Throwable {
		// The sanitizer should add a \n
		FormatterStep step = LicenseHeaderStep.headerDelimiter("LicenseHeader", "contentstart").build();
		var alreadyCorrect = "LicenseHeader\ncontentstart";
		Assertions.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		Assertions.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	void sanitizerDoesntGoTooFar() throws Throwable {
		// if the user wants extra lines after the header, we shouldn't clobber them
		FormatterStep step = LicenseHeaderStep.headerDelimiter("LicenseHeader\n\n", "contentstart").build();
		var alreadyCorrect = "LicenseHeader\n\ncontentstart";
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
		var currentYear = LocalDate.now(ZoneOffset.UTC).getYear();
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header(licenceWithAddress()), package_).withYearMode(YearMode.UPDATE_TO_TODAY).build();
		StepHarness.forStep(step).test(
				hasHeader(licenceWithAddress().replace("$YEAR", "2015")),
				hasHeader(licenceWithAddress().replace("$YEAR", "2015-" + currentYear)));
	}

	@Test
	void should_preserve_year_for_license_with_address() throws Throwable {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header(licenceWithAddress()), package_).withYearMode(YearMode.PRESERVE).build();
		StepHarness.forStep(step).test(
				hasHeader(licenceWithAddress().replace("$YEAR", "2015").replace("FooBar Inc. All", "FooBar Inc.  All")),
				hasHeader(licenceWithAddress().replace("$YEAR", "2015")));
	}

	@Test
	void should_apply_license_containing_filename_token() throws Exception {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$FILE), package_).build();
		StepHarnessWithFile.forStep(this, step)
				.test(new File("Test.java"), getTestResource(FILE_NO_LICENSE), hasHeaderFileName(HEADER_WITH_$FILE, "Test.java"))
				.testUnaffected(new File("Test.java"), hasHeaderFileName(HEADER_WITH_$FILE, "Test.java"));
	}

	@Test
	void should_update_license_containing_filename_token() throws Exception {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$FILE), package_).build();
		StepHarnessWithFile.forStep(this, step)
				.test(
						new File("After.java"),
						hasHeaderFileName(HEADER_WITH_$FILE, "Before.java"),
						hasHeaderFileName(HEADER_WITH_$FILE, "After.java"));
	}

	@Test
	void should_apply_license_containing_YEAR_filename_token() throws Exception {
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header(HEADER_WITH_$YEAR_$FILE), package_).build();
		StepHarnessWithFile.forStep(this, step)
				.test(
						new File("Test.java"),
						getTestResource(FILE_NO_LICENSE),
						hasHeaderYearFileName(HEADER_WITH_$YEAR_$FILE, currentYear(), "Test.java"))
				.testUnaffected(
						new File("Test.java"),
						hasHeaderYearFileName(HEADER_WITH_$YEAR_$FILE, currentYear(), "Test.java"));
	}

	void noPackage() throws Throwable {
		String header = header(getTestResource("license/TestLicense"));
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header, package_).build();
		StepHarness.forStep(step)
				.test(ResourceHarness.getTestResource("license/HelloWorld_java.test"), header + ResourceHarness.getTestResource("license/HelloWorld_java.test"))
				.test(ResourceHarness.getTestResource("license/HelloWorld_withImport_java.test"), header + ResourceHarness.getTestResource("license/HelloWorld_withImport_java.test"));
	}

	// The following demonstrate the use of 'module' keyword
	@Test
	void moduleInfo() throws Throwable {
		String header = header(getTestResource("license/TestLicense"));
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header, package_).build();
		StepHarness.forStep(step)
				.test(ResourceHarness.getTestResource("license/module-info.test"), header + ResourceHarness.getTestResource("license/module-info.test"));
	}
}
