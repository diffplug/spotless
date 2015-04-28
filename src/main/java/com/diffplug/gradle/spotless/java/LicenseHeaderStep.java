package com.diffplug.gradle.spotless.java;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Prefixes a license header before the package statement. */
public class LicenseHeaderStep {
	public static final String NAME = "LicenseHeader";

	private final String license;

	/** The license that we'd like enforced. */
	public LicenseHeaderStep(String license) {
		this.license = license.replace("\r", "");
	}

	/** Reads the license file from the given file. */
	public LicenseHeaderStep(File licenseFile) throws IOException {
		this(new String(Files.readAllBytes(licenseFile.toPath()), StandardCharsets.UTF_8));
	}

	public String format(String raw) {
		if (raw.startsWith("package ")) {
			return license + "\n" + raw;
		} else {
			// find the package statement
			int packageIdx = raw.indexOf("\npackage ");
			if (packageIdx < 0) {
				throw new IllegalArgumentException("Unable to find 'package'");
			} else {
				// return header + body
				String body = raw.substring(packageIdx);
				return license + body;
			}
		}
	}
}
