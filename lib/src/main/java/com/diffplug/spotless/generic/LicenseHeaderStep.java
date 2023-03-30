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
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.SerializableFileFilter;
import com.diffplug.spotless.ThrowingEx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Prefixes a license header before the package statement. */
public final class LicenseHeaderStep {
	public static final String DEFAULT_JAVA_HEADER_DELIMITER = "(package|import|public|class|module) ";
	private static final Logger LOGGER = LoggerFactory.getLogger(LicenseHeaderStep.class);

	public enum YearMode {
		PRESERVE, UPDATE_TO_TODAY, SET_FROM_GIT
	}

	public static LicenseHeaderStep headerDelimiter(String header, String delimiter) {
		return headerDelimiter(() -> header, delimiter);
	}

	public static LicenseHeaderStep headerDelimiter(ThrowingEx.Supplier<String> headerLazy, String delimiter) {
		return new LicenseHeaderStep(null, null, headerLazy, delimiter, DEFAULT_YEAR_DELIMITER, () -> YearMode.PRESERVE, null);
	}

	final String name;
	final @Nullable String contentPattern;
	final ThrowingEx.Supplier<String> headerLazy;
	final String delimiter;
	final String yearSeparator;
	final Supplier<YearMode> yearMode;
	final @Nullable String skipLinesMatching;

	private LicenseHeaderStep(@Nullable String name, @Nullable String contentPattern, ThrowingEx.Supplier<String> headerLazy, String delimiter, String yearSeparator, Supplier<YearMode> yearMode, @Nullable String skipLinesMatching) {
		this.name = sanitizeName(name);
		this.contentPattern = sanitizePattern(contentPattern);
		this.headerLazy = Objects.requireNonNull(headerLazy);
		this.delimiter = Objects.requireNonNull(delimiter);
		this.yearSeparator = Objects.requireNonNull(yearSeparator);
		this.yearMode = Objects.requireNonNull(yearMode);
		this.skipLinesMatching = sanitizePattern(skipLinesMatching);
	}

	public String getName() {
		return name;
	}

	public LicenseHeaderStep withName(String name) {
		return new LicenseHeaderStep(name, contentPattern, headerLazy, delimiter, yearSeparator, yearMode, skipLinesMatching);
	}

	public LicenseHeaderStep withContentPattern(String contentPattern) {
		return new LicenseHeaderStep(name, contentPattern, headerLazy, delimiter, yearSeparator, yearMode, skipLinesMatching);
	}

	public LicenseHeaderStep withHeaderString(String header) {
		return withHeaderLazy(() -> header);
	}

	public LicenseHeaderStep withHeaderLazy(ThrowingEx.Supplier<String> headerLazy) {
		return new LicenseHeaderStep(name, contentPattern, headerLazy, delimiter, yearSeparator, yearMode, skipLinesMatching);
	}

	public LicenseHeaderStep withDelimiter(String delimiter) {
		return new LicenseHeaderStep(name, contentPattern, headerLazy, delimiter, yearSeparator, yearMode, skipLinesMatching);
	}

	public LicenseHeaderStep withYearSeparator(String yearSeparator) {
		return new LicenseHeaderStep(name, contentPattern, headerLazy, delimiter, yearSeparator, yearMode, skipLinesMatching);
	}

	public LicenseHeaderStep withYearMode(YearMode yearMode) {
		return withYearModeLazy(() -> yearMode);
	}

	public LicenseHeaderStep withYearModeLazy(Supplier<YearMode> yearMode) {
		return new LicenseHeaderStep(name, contentPattern, headerLazy, delimiter, yearSeparator, yearMode, skipLinesMatching);
	}

	public LicenseHeaderStep withSkipLinesMatching(@Nullable String skipLinesMatching) {
		return new LicenseHeaderStep(name, contentPattern, headerLazy, delimiter, yearSeparator, yearMode, skipLinesMatching);
	}

	public FormatterStep build() {
		FormatterStep formatterStep = null;

		if (yearMode.get() == YearMode.SET_FROM_GIT) {
			formatterStep = FormatterStep.createNeverUpToDateLazy(name, () -> {
				var updateYear = false; // doesn't matter
				var runtime = new Runtime(headerLazy.get(), delimiter, yearSeparator, updateYear, skipLinesMatching);
				return FormatterFunc.needsFile(runtime::setLicenseHeaderYearsFromGitHistory);
			});
		} else {
			formatterStep = FormatterStep.createLazy(name, () -> {
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
				return new Runtime(headerLazy.get(), delimiter, yearSeparator, updateYear, skipLinesMatching);
			}, step -> FormatterFunc.needsFile(step::format));
		}

		if (contentPattern == null) {
			return formatterStep;
		}

		return formatterStep.filterByContentPattern(contentPattern);
	}

	private String sanitizeName(@Nullable String name) {
		if (name == null) {
			return DEFAULT_NAME_PREFIX;
		}

		name = name.trim();

		if (Objects.equals(DEFAULT_NAME_PREFIX, name) || name.startsWith(DEFAULT_NAME_PREFIX)) {
			return name;
		}

		return DEFAULT_NAME_PREFIX + "-" + name;
	}

	@Nullable
	private String sanitizePattern(@Nullable String pattern) {
		if (pattern == null) {
			return pattern;
		}

		pattern = pattern.trim();

		if (pattern.isEmpty()) {
			return null;
		}

		return pattern;
	}

	private static final String DEFAULT_NAME_PREFIX = LicenseHeaderStep.class.getName();
	private static final String DEFAULT_YEAR_DELIMITER = "-";
	private static final List<String> YEAR_TOKENS = Arrays.asList("$YEAR", "$today.year");

	private static final SerializableFileFilter UNSUPPORTED_JVM_FILES_FILTER = SerializableFileFilter.skipFilesNamed(
			"package-info.java", "package-info.groovy", "module-info.java");

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
		private final @Nullable Pattern skipLinesMatching;
		private final String yearSepOrFull;
		private final @Nullable String yearToday;
		private final @Nullable String beforeYear;
		private final @Nullable String afterYear;
		private final boolean updateYearWithLatest;
		private final boolean licenseHeaderWithRange;
		private final boolean hasFileToken;

		private static final Pattern FILENAME_PATTERN = Pattern.compile("\\$FILE");

		/** The license that we'd like enforced. */
		private Runtime(String licenseHeader, String delimiter, String yearSeparator, boolean updateYearWithLatest, @Nullable String skipLinesMatching) {
			if (delimiter.contains("\n")) {
				throw new IllegalArgumentException("The delimiter must not contain any newlines.");
			}
			// sanitize the input license
			licenseHeader = LineEnding.toUnix(licenseHeader);
			if (!licenseHeader.isEmpty() && !licenseHeader.endsWith("\n")) {
				licenseHeader = licenseHeader + "\n";
			}
			this.delimiterPattern = Pattern.compile('^' + delimiter, Pattern.UNIX_LINES | Pattern.MULTILINE);
			this.skipLinesMatching = skipLinesMatching == null ? null : Pattern.compile(skipLinesMatching);
			this.hasFileToken = FILENAME_PATTERN.matcher(licenseHeader).find();

			Optional<String> yearToken = getYearToken(licenseHeader);
			if (yearToken.isPresent()) {
				this.yearToday = String.valueOf(YearMonth.now().getYear());
				var yearTokenIndex = licenseHeader.indexOf(yearToken.get());
				this.beforeYear = licenseHeader.substring(0, yearTokenIndex);
				this.afterYear = licenseHeader.substring(yearTokenIndex + yearToken.get().length());
				this.yearSepOrFull = yearSeparator;
				this.updateYearWithLatest = updateYearWithLatest;

				var hasHeaderWithRange = false;
				var yearPlusSep = 4 + yearSeparator.length();
				if (beforeYear.endsWith(yearSeparator) && yearTokenIndex > yearPlusSep) {
					// year from in range
					var yearFrom = licenseHeader.substring(yearTokenIndex - yearPlusSep, yearTokenIndex).substring(0, 4);
					hasHeaderWithRange = YYYY.matcher(yearFrom).matches();
				}
				this.licenseHeaderWithRange = hasHeaderWithRange;
			} else {
				this.yearToday = null;
				this.beforeYear = null;
				this.afterYear = null;
				this.yearSepOrFull = licenseHeader;
				this.updateYearWithLatest = false;
				this.licenseHeaderWithRange = false;
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
		private String format(String raw, File file) {
			if (skipLinesMatching == null) {
				return addOrUpdateLicenseHeader(raw, file);
			} else {
				var lines = raw.split("\n");
				var skippedLinesBuilder = new StringBuilder();
				var remainingLinesBuilder = new StringBuilder();
				var lastMatched = true;
				for (String line : lines) {
					if (lastMatched) {
						var matcher = skipLinesMatching.matcher(line);
						if (matcher.find()) {
							skippedLinesBuilder.append(line).append('\n');
						} else {
							remainingLinesBuilder.append(line).append('\n');
							lastMatched = false;
						}
					} else {
						remainingLinesBuilder.append(line).append('\n');
					}
				}
				return skippedLinesBuilder + addOrUpdateLicenseHeader(remainingLinesBuilder.toString(), file);
			}
		}

		private String addOrUpdateLicenseHeader(String raw, File file) {
			raw = replaceYear(raw);
			raw = replaceFileName(raw, file);
			return raw;
		}

		private String replaceYear(String raw) {
			var contentMatcher = delimiterPattern.matcher(raw);
			if (!contentMatcher.find()) {
				throw new IllegalArgumentException("Unable to find delimiter regex " + delimiterPattern);
			} else {
				var content = raw.substring(contentMatcher.start());
				if (yearToday == null) {
					// the no year case is easy
					if (contentMatcher.start() == yearSepOrFull.length() && raw.startsWith(yearSepOrFull)) {
						// if no change is required, return the raw string without
						// creating any other new strings for maximum performance
						return raw;
					} else {
						// otherwise we'll have to add the header
						return yearSepOrFull + content;
					}
				} else {
					// the yes year case is a bit harder
					var beforeYearIdx = raw.indexOf(beforeYear);
					var afterYearIdx = raw.indexOf(afterYear, beforeYearIdx + beforeYear.length() + 1);

					if (beforeYearIdx >= 0 && afterYearIdx >= 0 && afterYearIdx + afterYear.length() <= contentMatcher.start()) {
						// and also ends with exactly the right header, so it's easy to parse the existing year
						var existingYear = raw.substring(beforeYearIdx + beforeYear.length(), afterYearIdx);
						var newYear = calculateYearExact(existingYear);
						if (existingYear.equals(newYear)) {
							// fastpath where we don't need to make any changes at all
							var noPadding = beforeYearIdx == 0 && afterYearIdx + afterYear.length() == contentMatcher.start(); // allows fastpath return raw
							if (noPadding) {
								return raw;
							}
						}
						return beforeYear + newYear + afterYear + content;
					} else {
						var newYear = calculateYearBySearching(raw.substring(0, contentMatcher.start()));
						// at worst, we just say that it was made today
						return beforeYear + newYear + afterYear + content;
					}
				}
			}
		}

		private static final Pattern YYYY = Pattern.compile("[0-9]{4}");

		/** Calculates the year to inject. */
		private String calculateYearExact(String parsedYear) {
			if (parsedYear.equals(yearToday)) {
				return parsedYear;
			} else if (YYYY.matcher(parsedYear).matches()) {
				if (updateYearWithLatest) {
					if (licenseHeaderWithRange) {
						return yearToday;
					} else {
						return parsedYear + yearSepOrFull + yearToday;
					}
				} else {
					// it's already good as a single year
					return parsedYear;
				}
			} else {
				return calculateYearBySearching(parsedYear);
			}
		}

		/** Searches the given string for YYYY, and uses that to determine the year range. */
		private String calculateYearBySearching(String content) {
			var yearMatcher = YYYY.matcher(content);
			if (yearMatcher.find()) {
				var firstYear = yearMatcher.group();

				String secondYear = null;
				if (updateYearWithLatest) {
					secondYear = firstYear.equals(yearToday) ? null : yearToday;
				} else {
					var contentWithSecondYear = content.substring(yearMatcher.end() + 1);
					var endOfLine = contentWithSecondYear.indexOf('\n');
					if (endOfLine != -1) {
						contentWithSecondYear = contentWithSecondYear.substring(0, endOfLine);
					}
					var secondYearMatcher = YYYY.matcher(contentWithSecondYear);
					if (secondYearMatcher.find()) {
						secondYear = secondYearMatcher.group();
					}
				}

				if (secondYear == null) {
					return firstYear;
				} else {
					if (licenseHeaderWithRange) {
						return secondYear;
					} else {
						return firstYear + yearSepOrFull + secondYear;
					}
				}
			} else {
				LOGGER.warn("Can't parse copyright year '{}', defaulting to {}", content, yearToday);
				// couldn't recognize the year format
				return yearToday;
			}
		}

		/** Sets copyright years on the given file by finding the oldest and most recent commits throughout git history. */
		private String setLicenseHeaderYearsFromGitHistory(String raw, File file) throws IOException {
			if (yearToday == null) {
				return raw;
			}
			var contentMatcher = delimiterPattern.matcher(raw);
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
			var newYear = parseYear("git log --max-count=1", file);
			String yearRange;
			if (oldYear.equals(newYear)) {
				yearRange = oldYear;
			} else {
				yearRange = oldYear + yearSepOrFull + newYear;
			}
			return beforeYear + yearRange + afterYear + raw.substring(contentMatcher.start());
		}

		private String replaceFileName(String raw, File file) {
			if (!hasFileToken) {
				return raw;
			}
			var contentMatcher = delimiterPattern.matcher(raw);
			if (!contentMatcher.find()) {
				throw new IllegalArgumentException("Unable to find delimiter regex " + delimiterPattern);
			}
			var header = raw.substring(0, contentMatcher.start());
			var content = raw.substring(contentMatcher.start());
			return FILENAME_PATTERN.matcher(header).replaceAll(file.getName()) + content;
		}

		private static String parseYear(String cmd, File file) throws IOException {
			var fullCmd = cmd + " -- " + file.getAbsolutePath();
			var builder = new ProcessBuilder().directory(file.getParentFile());
			if (FileSignature.machineIsWin()) {
				builder.command("cmd", "/c", fullCmd);
			} else {
				builder.command("bash", "-c", fullCmd);
			}
			var process = builder.start();
			var output = drain(process.getInputStream());
			var error = drain(process.getErrorStream());
			if (!error.isEmpty()) {
				throw new IllegalArgumentException("Error for command '" + fullCmd + "':\n" + error);
			}
			var matcher = FIND_YEAR.matcher(output);
			if (matcher.find()) {
				return matcher.group(1);
			} else {
				throw new IllegalArgumentException("Unable to parse date from command '" + fullCmd + "':\n" + output);
			}
		}

		private static final Pattern FIND_YEAR = Pattern.compile("Date:   .* ([0-9]{4}) ");

		@SuppressFBWarnings("DM_DEFAULT_ENCODING")
		private static String drain(InputStream stream) throws IOException {
			var output = new ByteArrayOutputStream();
			var buf = new byte[1024];
			int numRead;
			while ((numRead = stream.read(buf)) != -1) {
				output.write(buf, 0, numRead);
			}
			return new String(output.toByteArray());
		}
	}
}
