package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.gradle.api.GradleException;

/** Prefixes a license header before the package statement. */
public class LicenseHeaderStep {
	public static final String NAME = "LicenseHeader";

	private final String license;
	private final String delimiter;
	private final String newlineThenDelimiter;

	/** The license that we'd like enforced. */
	public LicenseHeaderStep(String license, String delimiter) {
		if (delimiter.contains("\n")) {
			throw new GradleException("The delimiter must not contain any newlines.");
		}
		this.license = license.replace("\r", "");
		this.delimiter = delimiter;
		this.newlineThenDelimiter = "\n" + delimiter;
	}

	/** Reads the license file from the given file. */
	public LicenseHeaderStep(File licenseFile, String delimiter) throws IOException {
		this(new String(Files.readAllBytes(licenseFile.toPath()), StandardCharsets.UTF_8), delimiter);
	}

	public String format(String raw) {
		if (raw.startsWith(delimiter)) {
			return license + "\n" + raw;
		} else {
			// find the package statement
			int packageIdx = raw.indexOf(newlineThenDelimiter);
			if (packageIdx < 0) {
				throw new IllegalArgumentException("Unable to find " + delimiter);
			} else {
				// return header + body
				String body = raw.substring(packageIdx);
				return license + body;
			}
		}
	}
}
