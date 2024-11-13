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
package com.diffplug.spotless.cli.steps;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import javax.annotation.Nonnull;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.antlr4.Antlr4Defaults;
import com.diffplug.spotless.cli.core.SpotlessActionContext;
import com.diffplug.spotless.cli.core.TargetFileTypeInferer;
import com.diffplug.spotless.cpp.CppDefaults;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.kotlin.KotlinConstants;
import com.diffplug.spotless.protobuf.ProtobufConstants;

import picocli.CommandLine;

@CommandLine.Command(name = "license-header", description = "Runs license header")
public class LicenseHeader extends SpotlessFormatterStep {

	@CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
	LicenseHeaderSourceOption licenseHeaderSourceOption;

	@CommandLine.Option(names = {"--delimiter", "-d"}, required = false, description = "The delimiter to use for the license header. If not provided, the default delimiter for the file type will be used (if available, otherwise java is assumed).")
	String delimiter;

	static class LicenseHeaderSourceOption {
		@CommandLine.Option(names = {"--header", "-H"}, required = true, description = "The license header content to apply. May contain @|YELLOW $YEAR|@ as placeholder.")
		String header;
		@CommandLine.Option(names = {"--header-file", "-f"}, required = true, description = "The license header content in a file to apply.%n  May contain @|YELLOW $YEAR|@ as placeholder.")
		File headerFile;
	}

	// TODO add more config options

	@Nonnull
	@Override
	public List<FormatterStep> prepareFormatterSteps(SpotlessActionContext context) {
		FormatterStep licenseHeaderStep = LicenseHeaderStep.headerDelimiter(headerSource(context), delimiter(context.targetFileType()))
				// TODO add more config options
				.build();
		return List.of(licenseHeaderStep);
	}

	private ThrowingEx.Supplier<String> headerSource(SpotlessActionContext context) {
		if (licenseHeaderSourceOption.header != null) {
			return () -> licenseHeaderSourceOption.header;
		} else {
			return () -> ThrowingEx.get(() -> Files.readString(context.resolveFile(licenseHeaderSourceOption.headerFile).toPath()));
		}
	}

	private String delimiter(TargetFileTypeInferer.TargetFileType inferredFileType) {
		if (delimiter != null) {
			return delimiter;
		} else {
			return inferredDelimiterType(inferredFileType);
		}
	}

	private String inferredDelimiterType(TargetFileTypeInferer.TargetFileType inferredFileType) {
		switch (inferredFileType.fileType()) {
		case JAVA:
			// fall through
		case GROOVY:
			return LicenseHeaderStep.DEFAULT_JAVA_HEADER_DELIMITER;
		case CPP:
			return CppDefaults.DELIMITER_EXPR;
		case ANTLR4:
			return Antlr4Defaults.licenseHeaderDelimiter();
		case PROTOBUF:
			return ProtobufConstants.LICENSE_HEADER_DELIMITER;
		case KOTLIN:
			return KotlinConstants.LICENSE_HEADER_DELIMITER;
		default:
			return LicenseHeaderStep.DEFAULT_JAVA_HEADER_DELIMITER;
		}
	}
}
