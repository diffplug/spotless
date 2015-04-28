package com.diffplug.gradle.spotless.java;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import org.gradle.api.GradleException;

import com.diffplug.gradle.spotless.FormatterStep;

/** Prefixes a license header before the package statement. */
public class LicenseHeaderStep implements FormatterStep {
	public static Optional<LicenseHeaderStep> load(String licenseHeader, File licenseHeaderFile) throws IOException {
		if (licenseHeader != null && licenseHeaderFile != null) {
			throw new GradleException("You can only specify one of licenseHeader and licenseHeaderFile, not both.");
		} else if (licenseHeader == null && licenseHeaderFile == null) {
			return Optional.empty();
		} else if (licenseHeader != null) {
			return Optional.of(new LicenseHeaderStep(licenseHeader));
		} else if (licenseHeaderFile != null) {
			return Optional.of(new LicenseHeaderStep(new String(Files.readAllBytes(licenseHeaderFile.toPath()), StandardCharsets.UTF_8)));
		} else {
			throw new IllegalArgumentException("Something crazy happened.");
		}
	}

	private final String license;

	/** The license that we'd like enforced. */
	private LicenseHeaderStep(String license) {
		this.license = license.replace("\r", "");
	}

	@Override
	public String getName() {
		return "LicenseHeader";
	}

	@Override
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
