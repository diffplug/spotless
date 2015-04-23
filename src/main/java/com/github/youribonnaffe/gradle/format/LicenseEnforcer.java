package com.github.youribonnaffe.gradle.format;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class LicenseEnforcer extends FormatterStep {
	public static FormatterStep load(String licenseHeader, File licenseHeaderFile) throws IOException {
		if (licenseHeader == null && licenseHeaderFile == null) {
			return FormatterStep.NO_OP;
		} else if (licenseHeader != null && licenseHeaderFile != null) {
			throw new IllegalArgumentException("You can only specify one of licenseHeader and licenseHeaderFile, not both.");
		} else if (licenseHeader != null) {
			return new LicenseEnforcer(licenseHeader);
		} else if (licenseHeaderFile != null) {
			return new LicenseEnforcer(new String(Files.readAllBytes(licenseHeaderFile.toPath()), StandardCharsets.UTF_8));
		} else {
			throw new IllegalArgumentException("Something crazy happened.");
		}
	}

	private final String license;

	/** The license that we'd like enforced. */
	private LicenseEnforcer(String license) {
		this.license = license.replace("\r", "");
	}

	public String format(String content) {
		if (content.startsWith("package ")) {
			return license + "\n" + content;
		} else {
			// find the package statement
			int packageIdx = content.indexOf("\npackage ");
			if (packageIdx < 0) {
				throw new IllegalArgumentException("Unable to find 'package'");
			} else {
				// return header + body
				String body = content.substring(packageIdx);
				return license + body;
			}
		}
	}
}
