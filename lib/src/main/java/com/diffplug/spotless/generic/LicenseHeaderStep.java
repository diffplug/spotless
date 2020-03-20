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
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.SerializableFileFilter;

/** Prefixes a license header before the package statement. */
public final class LicenseHeaderStep implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final String NAME = "licenseHeader";
	private static final String DEFAULT_YEAR_DELIMITER = "-";
	private static final List<String> YEAR_TOKENS = Arrays.asList("$YEAR", "$today.year");

	private static final SerializableFileFilter UNSUPPORTED_JVM_FILES_FILTER = SerializableFileFilter.skipFilesNamed(
			"package-info.java", "package-info.groovy", "module-info.java");

	private final String licenseHeader;
	private final boolean hasYearToken;
	private final Pattern delimiterPattern;
	private final Pattern yearMatcherPattern;
	private final String licenseHeaderBeforeYearToken;
	private final String licenseHeaderAfterYearToken;
	private final String licenseHeaderWithYearTokenReplaced;

	/** Creates a FormatterStep which forces the start of each file to match a license header. */
	public static FormatterStep createFromHeader(String licenseHeader, String delimiter) {
		return createFromHeader(licenseHeader, delimiter, DEFAULT_YEAR_DELIMITER);
	}

	public static FormatterStep createFromHeader(String licenseHeader, String delimiter, String yearSeparator) {
		Objects.requireNonNull(licenseHeader, "licenseHeader");
		Objects.requireNonNull(delimiter, "delimiter");
		Objects.requireNonNull(yearSeparator, "yearSeparator");
		return FormatterStep.create(LicenseHeaderStep.NAME,
				new LicenseHeaderStep(licenseHeader, delimiter, yearSeparator),
				step -> step::format);
	}

	/**
	 * Creates a FormatterStep which forces the start of each file to match the license header
	 * contained in the given file.
	 */
	public static FormatterStep createFromFile(File licenseHeaderFile, Charset encoding, String delimiter) {
		return createFromFile(licenseHeaderFile, encoding, delimiter, DEFAULT_YEAR_DELIMITER);
	}

	/**
	 * Creates a FormatterStep which forces the start of each file to match the license header
	 * contained in the given file.
	 */
	public static FormatterStep createFromFile(File licenseHeaderFile, Charset encoding, String delimiter, String yearSeparator) {
		Objects.requireNonNull(licenseHeaderFile, "licenseHeaderFile");
		Objects.requireNonNull(encoding, "encoding");
		Objects.requireNonNull(delimiter, "delimiter");
		Objects.requireNonNull(yearSeparator, "yearSeparator");
		return FormatterStep.createLazy(LicenseHeaderStep.NAME,
				() -> new LicenseHeaderStep(licenseHeaderFile, encoding, delimiter, yearSeparator),
				step -> step::format);
	}

	public static String name() {
		return NAME;
	}

	public static String defaultYearDelimiter() {
		return DEFAULT_YEAR_DELIMITER;
	}

	public static SerializableFileFilter unsupportedJvmFilesFilter() {
		return UNSUPPORTED_JVM_FILES_FILTER;
	}

	/** The license that we'd like enforced. */
	private LicenseHeaderStep(String licenseHeader, String delimiter, String yearSeparator) {
		if (delimiter.contains("\n")) {
			throw new IllegalArgumentException("The delimiter must not contain any newlines.");
		}
		// sanitize the input license
		licenseHeader = LineEnding.toUnix(licenseHeader);
		if (!licenseHeader.endsWith("\n")) {
			licenseHeader = licenseHeader + "\n";
		}
		this.licenseHeader = licenseHeader;
		this.delimiterPattern = Pattern.compile('^' + delimiter, Pattern.UNIX_LINES | Pattern.MULTILINE);

		Optional<String> yearToken = getYearToken(licenseHeader);
		this.hasYearToken = yearToken.isPresent();
		if (hasYearToken) {
			int yearTokenIndex = licenseHeader.indexOf(yearToken.get());
			licenseHeaderBeforeYearToken = licenseHeader.substring(0, yearTokenIndex);
			licenseHeaderAfterYearToken = licenseHeader.substring(yearTokenIndex + 5);
			licenseHeaderWithYearTokenReplaced = licenseHeader.replace(yearToken.get(), String.valueOf(YearMonth.now().getYear()));
			yearMatcherPattern = Pattern.compile("[0-9]{4}(" + Pattern.quote(yearSeparator) + "[0-9]{4})?");
		} else {
			licenseHeaderBeforeYearToken = null;
			licenseHeaderAfterYearToken = null;
			licenseHeaderWithYearTokenReplaced = null;
			yearMatcherPattern = null;
		}
	}

	/**
	 * Get the first place holder token being used in the
	 * license header for specifying the year
	 *
	 * @param licenseHeader String representation of the license header
	 * @return Matching value from YEAR_TOKENS or null if none exist
	 */
	private static Optional<String> getYearToken(String licenseHeader) {
		return YEAR_TOKENS.stream().filter(licenseHeader::contains).findFirst();
	}

	/** Reads the license file from the given file. */
	private LicenseHeaderStep(File licenseFile, Charset encoding, String delimiter, String yearSeparator) throws IOException {
		this(new String(Files.readAllBytes(licenseFile.toPath()), encoding), delimiter, yearSeparator);
	}

	/** Formats the given string. */
	public String format(String raw) {
		Matcher matcher = delimiterPattern.matcher(raw);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Unable to find delimiter regex " + delimiterPattern);
		} else {
			if (hasYearToken) {
				if (matchesLicenseWithYearToken(raw, matcher)) {
					// that means we have the license like `licenseHeaderBeforeYearToken 1990-2015 licenseHeaderAfterYearToken`
					return raw;
				} else {
					return licenseHeaderWithYearTokenReplaced + raw.substring(matcher.start());
				}
			} else if (matcher.start() == licenseHeader.length() && raw.startsWith(licenseHeader)) {
				// if no change is required, return the raw string without
				// creating any other new strings for maximum performance
				return raw;
			} else {
				// otherwise we'll have to add the header
				return licenseHeader + raw.substring(matcher.start());
			}
		}
	}

	private boolean matchesLicenseWithYearToken(String raw, Matcher matcher) {
		int startOfTheSecondPart = raw.indexOf(licenseHeaderAfterYearToken);
		return startOfTheSecondPart > licenseHeaderBeforeYearToken.length()
				&& (raw.startsWith(licenseHeaderBeforeYearToken) && startOfTheSecondPart + licenseHeaderAfterYearToken.length() == matcher.start())
				&& yearMatcherPattern.matcher(raw.substring(licenseHeaderBeforeYearToken.length(), startOfTheSecondPart)).matches();
	}
}
