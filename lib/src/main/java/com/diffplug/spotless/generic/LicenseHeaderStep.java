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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;

/** Prefixes a license header before the package statement. */
public final class LicenseHeaderStep implements Serializable {
	private static final long serialVersionUID = 1L;

	// TODO: Make private and only allow access through a public static method?
	public static final String NAME = "LicenseHeader";

	private final String licenseHeader;
	private final Pattern delimiterPattern;

	/** Creates a FormatterStep which forces the start of each file to match a license header. */
	public static FormatterStep createFromHeader(String licenseHeader, String delimiter) {
		return FormatterStep.create(LicenseHeaderStep.NAME,
				new LicenseHeaderStep(licenseHeader, delimiter),
				step -> step::format);
	}

	/**
	 * Creates a FormatterStep which forces the start of each file to match the license header
	 * contained in the given file.
	 */
	public static FormatterStep createFromFile(File licenseHeaderFile, Charset encoding, String delimiter) {
		return FormatterStep.createLazy(LicenseHeaderStep.NAME,
				() -> new LicenseHeaderStep(licenseHeaderFile, encoding, delimiter),
				step -> step::format);
	}

	/** The license that we'd like enforced. */
	// TODO: Make package-private when LicenseHeaderStepTest is migrated to testlib
	public LicenseHeaderStep(String licenseHeader, String delimiter) {
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
	}

	/** Reads the license file from the given file. */
	// TODO: Make package-private when LicenseHeaderStepTest is migrated to testlib
	public LicenseHeaderStep(File licenseFile, Charset encoding, String delimiter) throws IOException {
		this(new String(Files.readAllBytes(licenseFile.toPath()), encoding), delimiter);
	}

	/** Formats the given string. */
	public String format(String raw) {
		Matcher matcher = delimiterPattern.matcher(raw);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Unable to find delimiter regex " + delimiterPattern);
		} else {
			if (matcher.start() == licenseHeader.length() && raw.startsWith(licenseHeader)) {
				// if no change is required, return the raw string without
				// creating any other new strings for maximum performance
				return raw;
			} else {
				// otherwise we'll have to add the header
				return licenseHeader + raw.substring(matcher.start());
			}
		}
	}
}
