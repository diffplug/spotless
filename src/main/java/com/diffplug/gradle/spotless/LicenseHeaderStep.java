package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.GradleException;

/** Prefixes a license header before the package statement. */
public class LicenseHeaderStep {
	public static final String NAME = "LicenseHeader";

	private final String license;
	private final Pattern delimiterPattern;

	/** The license that we'd like enforced. */
	public LicenseHeaderStep(String license, String delimiter) {
		if (delimiter.contains("\n")) {
			throw new GradleException("The delimiter must not contain any newlines.");
		}
		// sanitize the input license
		license = license.replace("\r", "");
		if (!license.endsWith("\n")) {
			license = license + "\n";
		}
		this.license = license;
		this.delimiterPattern = Pattern.compile('^' + delimiter, Pattern.UNIX_LINES | Pattern.MULTILINE);
	}

	/** Reads the license file from the given file. */
	public LicenseHeaderStep(File licenseFile, String delimiter) throws IOException {
		this(new String(Files.readAllBytes(licenseFile.toPath()), StandardCharsets.UTF_8), delimiter);
	}

	/** Formats the given string. */
	public String format(String raw) {
		Matcher matcher = delimiterPattern.matcher(raw);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Unable to find delimiter regex " + delimiterPattern);
		} else {
			if (matcher.start() == license.length() && raw.startsWith(license)) {
				// if no change is required, return the raw string without
				// creating any other new strings for maximum performance
				return raw;
			} else {
				// otherwise we'll have to add the header
				return license + raw.substring(matcher.start());
			}
		}
	}
}
