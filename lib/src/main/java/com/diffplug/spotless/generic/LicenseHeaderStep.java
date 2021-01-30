/*
 * Copyright 2016-2021 DiffPlug
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
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.SerializableFileFilter;
import com.diffplug.spotless.ThrowingEx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Prefixes a license header before the package statement. */
public final class LicenseHeaderStep {
	public enum YearMode {
		PRESERVE, UPDATE_TO_TODAY, SET_FROM_GIT
	}

	public static LicenseHeaderStep headerDelimiter(String header, String delimiter) {
		return headerDelimiter(() -> header, delimiter);
	}

	public static LicenseHeaderStep headerDelimiter(ThrowingEx.Supplier<String> headerLazy, String delimiter) {
		return new LicenseHeaderStep(headerLazy, delimiter, DEFAULT_YEAR_DELIMITER, () -> YearMode.PRESERVE);
	}

	final ThrowingEx.Supplier<String> headerLazy;
	final String delimiter;
	final String yearSeparator;
	final Supplier<YearMode> yearMode;

	private LicenseHeaderStep(ThrowingEx.Supplier<String> headerLazy, String delimiter, String yearSeparator, Supplier<YearMode> yearMode) {
		this.headerLazy = Objects.requireNonNull(headerLazy);
		this.delimiter = Objects.requireNonNull(delimiter);
		this.yearSeparator = Objects.requireNonNull(yearSeparator);
		this.yearMode = Objects.requireNonNull(yearMode);
	}

	public LicenseHeaderStep withHeaderString(String header) {
		return withHeaderLazy(() -> header);
	}

	public LicenseHeaderStep withHeaderLazy(ThrowingEx.Supplier<String> headerLazy) {
		return new LicenseHeaderStep(headerLazy, delimiter, yearSeparator, yearMode);
	}

	public LicenseHeaderStep withDelimiter(String delimiter) {
		return new LicenseHeaderStep(headerLazy, delimiter, yearSeparator, yearMode);
	}

	public LicenseHeaderStep withYearSeparator(String yearSeparator) {
		return new LicenseHeaderStep(headerLazy, delimiter, yearSeparator, yearMode);
	}

	public LicenseHeaderStep withYearMode(YearMode yearMode) {
		return withYearModeLazy(() -> yearMode);
	}

	public LicenseHeaderStep withYearModeLazy(Supplier<YearMode> yearMode) {
		return new LicenseHeaderStep(headerLazy, delimiter, yearSeparator, yearMode);
	}

	public FormatterStep build() {
		if (yearMode.get() == YearMode.SET_FROM_GIT) {
			return FormatterStep.createNeverUpToDateLazy(LicenseHeaderStep.name(), () -> {
				boolean updateYear = false; // doesn't matter
				Runtime runtime = new Runtime(headerLazy.get(), delimiter, yearSeparator, updateYear);
				return FormatterFunc.needsFile(runtime::setLicenseHeaderYearsFromGitHistory);
			});
		} else {
			return FormatterStep.createLazy(LicenseHeaderStep.name(), () -> {
				// by default, we should update the year if the user is using ratchetFrom
				boolean updateYear;
				switch (yearMode.get()) {
				case PRESERVE:
					updateYear = false;
					break;
				case UPDATE_TO_TODAY:
					updateYear = true;
					break;
				case SET_FROM_GIT:
				default:
					throw new IllegalStateException(yearMode.toString());
				}
				return new Runtime(headerLazy.get(), delimiter, yearSeparator, updateYear);
			}, step -> step::format);
		}
	}

	private static final String NAME = "licenseHeader";
	private static final String DEFAULT_YEAR_DELIMITER = "-";
	private static final List<String> YEAR_TOKENS = Arrays.asList("$YEAR", "$today.year");

	private static final SerializableFileFilter UNSUPPORTED_JVM_FILES_FILTER = SerializableFileFilter.skipFilesNamed(
			"package-info.java", "package-info.groovy", "module-info.java");

	public static String name() {
		return NAME;
	}

	public static String defaultYearDelimiter() {
		return DEFAULT_YEAR_DELIMITER;
	}

	public static SerializableFileFilter unsupportedJvmFilesFilter() {
		return UNSUPPORTED_JVM_FILES_FILTER;
	}

	public static final String spotlessSetLicenseHeaderYearsFromGitHistory = "spotlessSetLicenseHeaderYearsFromGitHistory";

	public static final String FLAG_SET_LICENSE_HEADER_YEARS_FROM_GIT_HISTORY() {
		return spotlessSetLicenseHeaderYearsFromGitHistory;
	}

	private static class Runtime implements Serializable {
		private static final long serialVersionUID = 1475199492829130965L;

		private final Pattern delimiterPattern;
		private final String yearSepOrFull;
		private final @Nullable String yearToday;
		private final @Nullable String beforeYear;
		private final @Nullable String afterYear;
		private final boolean updateYearWithLatest;

		/** The license that we'd like enforced. */
		private Runtime(String licenseHeader, String delimiter, String yearSeparator, boolean updateYearWithLatest) {
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
		private String format(String raw) {
			Matcher contentMatcher = findContentDelimiter(raw);

			if (yearToday == null) {
				return formatFixedLicenseHeader(raw, contentMatcher);
			}

			SortedSet<String> years = new TreeSet<>();

			extractYearsFromHeader(years::add, raw, contentMatcher);

			if (updateYearWithLatest || years.isEmpty()) {
				years.add(yearToday);
			}

			return formatYearRangeLicenseHeader(raw, contentMatcher, years);
		}

		/**
		 * Find the delimiter marking the end of the header and start of the content.
		 * @param raw the text of the whole file.
		 * @return a {@link Matcher} instance at the location in the file where the delimiter is.
		 * @throws IllegalArgumentException if the delimiter cannot be found.
		 */
		private Matcher findContentDelimiter(String raw) {
			Matcher contentMatcher = delimiterPattern.matcher(raw);
			if (!contentMatcher.find()) {
				throw new IllegalArgumentException("Unable to find delimiter regex " + delimiterPattern);
			}
			return contentMatcher;
		}

		/**
		 * Format the file with the full fixed content license header.
		 * @param raw            the text of the whole file.
		 * @param contentMatcher as returned by {@link #findContentDelimiter(String)}.
		 * @return the file with the full fixed content license header.
		 */
		private String formatFixedLicenseHeader(String raw, Matcher contentMatcher) {
			// the no year case is easy
			if (contentMatcher.start() == yearSepOrFull.length() && raw.startsWith(yearSepOrFull)) {
				// if no change is required, return the raw string without
				// creating any other new strings for maximum performance
				return raw;
			} else {
				// otherwise we'll have to add the header
				return yearSepOrFull + raw.substring(contentMatcher.start());
			}
		}

		/**
		 * Format the file with a date range license header.
		 * @param raw            the text of the whole file.
		 * @param contentMatcher as returned by {@link #findContentDelimiter(String)}.
		 * @param years 		 the range of years to use.
		 * @return the file with the full fixed content license header.
		 */
		private String formatYearRangeLicenseHeader(String raw, Matcher contentMatcher, SortedSet<String> years) {
			String header;
			if (years.size() == 1) {
				header = beforeYear + years.first() + afterYear;
			} else {
				header = beforeYear + years.first() + yearSepOrFull + years.last() + afterYear;
			}

			if (header.length() == contentMatcher.start() && raw.startsWith(header)) {
				// fast path where we don't need to make any changes at all
				return raw;
			} else {
				return header + raw.substring(contentMatcher.start());
			}
		}

		/**
		 * Extract years from the existing license header.
		 * @param years          a consumer to accept extracted years
		 * @param raw            the text of the whole file.
		 * @param contentMatcher as returned by {@link #findContentDelimiter(String)}.
		 */
		private void extractYearsFromHeader(Consumer<String> years, String raw, Matcher contentMatcher) {
			// the yes year case is a bit harder
			int beforeYearIdx = raw.indexOf(beforeYear);
			int afterYearIdx = raw.indexOf(afterYear, beforeYearIdx + beforeYear.length() + 1);

			if (beforeYearIdx >= 0 && afterYearIdx >= 0 && afterYearIdx + afterYear.length() <= contentMatcher.start()) {
				// and also ends with exactly the right header, so it's easy to parse the existing year
				String existingYear = raw.substring(beforeYearIdx + beforeYear.length(), afterYearIdx);
				extractYearsFromText(years, existingYear);
			} else {
				String existingHeader = raw.substring(0, contentMatcher.start());
				extractYearsFromText(years, existingHeader);
			}
		}

		private static final Pattern YYYY = Pattern.compile("[0-9]{4}");

		/**
		 * Extract years from the given string using the YYYY pattern.
		 * @param years a consumer to accept extracted years
		 * @param text  the text to search within.
		 */
		private void extractYearsFromText(Consumer<String> years, String text) {
			if (text.equals(yearToday)) {
				// fast path if the text is an exact match
				years.accept(yearToday);
				return;
			}

			Matcher yearMatcher = YYYY.matcher(text);
			if (yearMatcher.find()) {
				// extract the first year of the range
				years.accept(yearMatcher.group());

				// look for an possible second year in the text
				final int nextStart = yearMatcher.end() + 1;
				if (nextStart < text.length() && yearMatcher.find(nextStart)) {
					years.accept(yearMatcher.group());
				}
			}
		}

		/** Sets copyright years on the given file by finding the oldest and most recent commits throughout git history. */
		private String setLicenseHeaderYearsFromGitHistory(String raw, File file) throws IOException {
			Matcher contentMatcher = findContentDelimiter(raw);
			if (yearToday == null) {
				return formatFixedLicenseHeader(raw, contentMatcher);
			}

			SortedSet<String> years = new TreeSet<>();
			extractYearsFromGitHistory(years::add, file);

			return formatYearRangeLicenseHeader(raw, contentMatcher, years);
		}

		/**
		 * Extract years from the git history by finding the oldest and most recent commits throughout git history.
		 * @param file  the file to search git for.
		 * @param years a consumer to accept extracted years
		 */
		private void extractYearsFromGitHistory(Consumer<String> years, File file) throws IOException {
			try {
				years.accept(parseYear("git log --follow --find-renames=40% --diff-filter=A", file));
			} catch (IllegalArgumentException e) {
				// Ideally, git log would always find the commit where it was added.
				// For some reason, that is sometimes not possible - in that case,
				// we'll settle for just the most recent, even if it was just a modification.
				years.accept(parseYear("git log --follow --find-renames=40% --reverse", file));
			}
			years.accept(parseYear("git log --max-count=1", file));
		}

		private static String parseYear(String cmd, File file) throws IOException {
			String fullCmd = cmd + " " + file.getAbsolutePath();
			ProcessBuilder builder = new ProcessBuilder().directory(file.getParentFile());
			if (FileSignature.machineIsWin()) {
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
}
