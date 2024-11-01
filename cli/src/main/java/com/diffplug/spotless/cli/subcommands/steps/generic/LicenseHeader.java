/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.cli.subcommands.steps.generic;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import javax.annotation.Nonnull;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.antlr4.Antlr4Defaults;
import com.diffplug.spotless.cpp.CppDefaults;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.kotlin.KotlinConstants;
import com.diffplug.spotless.protobuf.ProtobufConstants;

import picocli.CommandLine;

@CommandLine.Command(name = "license-header", description = "Runs license header")
public class LicenseHeader extends SpotlessFormatterStepSubCommand {

	@CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
	LicenseHeaderSourceOption licenseHeaderSourceOption;

	@CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
	LicenseHeaderDelimiterOption licenseHeaderDelimiterOption;

	static class LicenseHeaderSourceOption {
		@CommandLine.Option(names = {"--header", "-H"}, required = true)
		String header;
		@CommandLine.Option(names = {"--header-file", "-f"}, required = true)
		File headerFile;
	}

	static class LicenseHeaderDelimiterOption {

		@CommandLine.Option(names = {"--delimiter", "-d"}, required = true)
		String delimiter;

		@CommandLine.Option(names = {"--delimiter-for", "-D"}, required = true)
		DefaultDelimiterType defaultDelimiterType;
	}

	enum DefaultDelimiterType {
		JAVA(LicenseHeaderStep.DEFAULT_JAVA_HEADER_DELIMITER), CPP(CppDefaults.DELIMITER_EXPR), ANTLR4(Antlr4Defaults.licenseHeaderDelimiter()), GROOVY(LicenseHeaderStep.DEFAULT_JAVA_HEADER_DELIMITER), PROTOBUF(ProtobufConstants.LICENSE_HEADER_DELIMITER), KOTLIN(KotlinConstants.LICENSE_HEADER_DELIMITER);

		private final String delimiterExpression;

		DefaultDelimiterType(String delimiterExpression) {
			this.delimiterExpression = delimiterExpression;
		}
	}

	@Nonnull
	@Override
	public List<FormatterStep> prepareFormatterSteps() {
		FormatterStep licenseHeaderStep = LicenseHeaderStep.headerDelimiter(headerSource(), delimiter())
				// TODO add more config options
				.build();
		return List.of(licenseHeaderStep);
	}

	private ThrowingEx.Supplier<String> headerSource() {
		if (licenseHeaderSourceOption.header != null) {
			return () -> licenseHeaderSourceOption.header;
		} else {
			return () -> ThrowingEx.get(() -> Files.readString(licenseHeaderSourceOption.headerFile.toPath()));
		}
	}

	private String delimiter() {
		if (licenseHeaderDelimiterOption == null) {
			return DefaultDelimiterType.JAVA.delimiterExpression;
		}
		if (licenseHeaderDelimiterOption.delimiter != null) {
			return licenseHeaderDelimiterOption.delimiter;
		} else {
			return licenseHeaderDelimiterOption.defaultDelimiterType.delimiterExpression;
		}
	}
}
