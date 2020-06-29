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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.SerializableFileFilter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Prefixes a license header before the package statement. */
public final class LicenseHeaderStep implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final String NAME = "licenseHeader";
	private static final String DEFAULT_YEAR_DELIMITER = "-";
	private static final List<String> YEAR_TOKENS = Arrays.asList("$YEAR", "$today.year");

	private static final SerializableFileFilter UNSUPPORTED_JVM_FILES_FILTER = SerializableFileFilter.skipFilesNamed(
			"package-info.java", "package-info.groovy", "module-info.java");

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
				() -> new LicenseHeaderStep(new String(Files.readAllBytes(licenseHeaderFile.toPath()), encoding), delimiter, yearSeparator),
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

	private final Pattern delimiterPattern;
	private final String yearSepOrFull;
	private final @Nullable String yearToday;
	private final @Nullable String beforeYear;
	private final @Nullable String afterYear;
	private final boolean updateYearWithLatest;

	private LicenseHeaderStep(String licenseHeader, String delimiter, String yearSeparator) {
		this(licenseHeader, delimiter, yearSeparator, false);
	}

	/** The license that we'd like enforced. */
	public LicenseHeaderStep(String licenseHeader, String delimiter, String yearSeparator, boolean updateYearWithLatest) {
		if (delimiter.contains("\n")) {
			throw new IllegalArgumentException("The delimiter must not contain any newlines.");
		}
		// sanitize the input license
		licenseHeader = LineEnding.toUnix(licenseHeader);
		if (!licenseHeader.endsWith("\n")) {
			licenseHeader = licenseHeader + "\n";
		}
		this.delimiterPattern = Pattern.compile('^' + delimiter, Pattern.UNIX_LINES | Pattern.MULTILINE);

		Optional<String> yearToken = getYearToken(licenseHeader);
		if (yearToken.isPresent()) {
			yearToday = String.valueOf(YearMonth.now().getYear());
			int yearTokenIndex = licenseHeader.indexOf(yearToken.get());
			beforeYear = licenseHeader.substring(0, yearTokenIndex);
			afterYear = licenseHeader.substring(yearTokenIndex + yearToken.get().length());
			yearSepOrFull = yearSeparator;
			this.updateYearWithLatest = updateYearWithLatest;
		} else {
			yearToday = null;
			beforeYear = null;
			afterYear = null;
			this.yearSepOrFull = licenseHeader;
			this.updateYearWithLatest = false;
		}
	}

	private static final Pattern patternYearSingle = Pattern.compile("[0-9]{4}");

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

	/** Formats the given string. */
	public String format(String raw) {
		Matcher contentMatcher = delimiterPattern.matcher(raw);
		if (!contentMatcher.find()) {
			throw new IllegalArgumentException("Unable to find delimiter regex " + delimiterPattern);
		} else {
			if (yearToday == null) {
				// the no year case is easy
				if (contentMatcher.start() == yearSepOrFull.length() && raw.startsWith(yearSepOrFull)) {
					// if no change is required, return the raw string without
					// creating any other new strings for maximum performance
					return raw;
				} else {
					// otherwise we'll have to add the header
					return yearSepOrFull + raw.substring(contentMatcher.start());
				}
			} else {
				// the yes year case is a bit harder
				int beforeYearIdx = raw.indexOf(beforeYear);
				int afterYearIdx = raw.indexOf(afterYear, beforeYearIdx + beforeYear.length() + 1);

				if (beforeYearIdx >= 0 && afterYearIdx >= 0 && afterYearIdx + afterYear.length() <= contentMatcher.start()) {
					boolean noPadding = beforeYearIdx == 0 && afterYearIdx + afterYear.length() == contentMatcher.start(); // allows fastpath return raw
					String parsedYear = raw.substring(beforeYearIdx + beforeYear.length(), afterYearIdx);
					if (parsedYear.equals(yearToday)) {
						// it's good as is!
						return noPadding ? raw : beforeYear + yearToday + afterYear + raw.substring(contentMatcher.start());
					} else if (patternYearSingle.matcher(parsedYear).matches()) {
						if (updateYearWithLatest) {
							// expand from `2004` to `2004-2020`
							return beforeYear + parsedYear + yearSepOrFull + yearToday + afterYear + raw.substring(contentMatcher.start());
						} else {
							// it's already good as a single year
							return noPadding ? raw : beforeYear + parsedYear + afterYear + raw.substring(contentMatcher.start());
						}
					} else {
						Matcher yearMatcher = patternYearSingle.matcher(parsedYear);
						if (yearMatcher.find()) {
							String firstYear = yearMatcher.group();
							String newYear;
							String secondYear;
							if (updateYearWithLatest) {
								secondYear = firstYear.equals(yearToday) ? null : yearToday;
							} else if (yearMatcher.find(yearMatcher.end() + 1)) {
								secondYear = yearMatcher.group();
							} else {
								secondYear = null;
							}
							if (secondYear == null) {
								newYear = firstYear;
							} else {
								newYear = firstYear + yearSepOrFull + secondYear;
							}
							return noPadding && newYear.equals(parsedYear) ? raw : beforeYear + newYear + afterYear + raw.substring(contentMatcher.start());
						}
					}
				}
				// at worst, we just say that it was made today
				return beforeYear + yearToday + afterYear + raw.substring(contentMatcher.start());
			}
		}
	}

	public static final String spotlessSetLicenseHeaderYearsFromGitHistory = "spotlessSetLicenseHeaderYearsFromGitHistory";

	public static final String FLAG_SET_LICENSE_HEADER_YEARS_FROM_GIT_HISTORY() {
		return spotlessSetLicenseHeaderYearsFromGitHistory;
	}

	/** Sets copyright years on the given file by finding the oldest and most recent commits throughout git history. */
	public String setLicenseHeaderYearsFromGitHistory(String raw, File file) throws IOException {
		if (yearToday == null) {
			return raw;
		}
		Matcher contentMatcher = delimiterPattern.matcher(raw);
		if (!contentMatcher.find()) {
			throw new IllegalArgumentException("Unable to find delimiter regex " + delimiterPattern);
		}

		String oldYear;
		try {
			oldYear = parseYear("git log --follow --find-renames=40% --diff-filter=A", file);
		} catch (IllegalArgumentException e) {
			// Ideally, git log would always find the commit where it was added.
			// For some reason, that is sometimes not possible - in that case,
			// we'll settle for just the most recent, even if it was just a modification.
			oldYear = parseYear("git log --follow --find-renames=40% --reverse", file);
		}
		String newYear = parseYear("git log --max-count=1", file);
		String yearRange;
		if (oldYear.equals(newYear)) {
			yearRange = oldYear;
		} else {
			yearRange = oldYear + yearSepOrFull + newYear;
		}
		return beforeYear + yearRange + afterYear + raw.substring(contentMatcher.start());
	}

	private static String parseYear(String cmd, File file) throws IOException {
		String fullCmd = cmd + " " + file.getAbsolutePath();
		ProcessBuilder builder = new ProcessBuilder().directory(file.getParentFile());
		if (LineEnding.nativeIsWin()) {
			builder.command("cmd", "/c", fullCmd);
		} else {
			builder.command("bash", "-c", fullCmd);
		}
		Process process = builder.start();
		String output = drain(process.getInputStream());
		String error = drain(process.getErrorStream());
		if (!error.isEmpty()) {
			throw new IllegalArgumentException("Error for command '" + fullCmd + "':\n" + error);
		}
		Matcher matcher = FIND_YEAR.matcher(output);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			throw new IllegalArgumentException("Unable to parse date from command '" + fullCmd + "':\n" + output);
		}
	}

	private static final Pattern FIND_YEAR = Pattern.compile("Date:   .* ([0-9]{4}) ");

	@SuppressFBWarnings("DM_DEFAULT_ENCODING")
	private static String drain(InputStream stream) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int numRead;
		while ((numRead = stream.read(buf)) != -1) {
			output.write(buf, 0, numRead);
		}
		return new String(output.toByteArray());
	}
}
