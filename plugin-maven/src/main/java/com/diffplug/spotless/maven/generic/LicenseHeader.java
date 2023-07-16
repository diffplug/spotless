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
package com.diffplug.spotless.maven.generic;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.generic.LicenseHeaderStep.YearMode;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class LicenseHeader implements FormatterStepFactory {

	@Parameter
	private String file;

	@Parameter
	private String content;

	@Parameter
	private String delimiter;

	@Parameter
	private String skipLinesMatching;

	@Override
	public final FormatterStep newFormatterStep(FormatterStepConfig config) {
		String delimiterString = delimiter != null ? delimiter : config.getLicenseHeaderDelimiter();
		if (delimiterString == null) {
			throw new IllegalArgumentException("You need to specify 'delimiter'.");
		}
		if (file != null ^ content != null) {
			YearMode yearMode;
			if ("true".equals(config.spotlessSetLicenseHeaderYearsFromGitHistory().orElse(""))) {
				yearMode = YearMode.SET_FROM_GIT;
			} else {
				boolean updateYear = config.getRatchetFrom().isPresent();
				yearMode = updateYear ? YearMode.UPDATE_TO_TODAY : YearMode.PRESERVE;
			}
			return LicenseHeaderStep.headerDelimiter(() -> readFileOrContent(config), delimiterString)
					.withYearMode(yearMode)
					.withSkipLinesMatching(skipLinesMatching)
					.build()
					.filterByFile(LicenseHeaderStep.unsupportedJvmFilesFilter());
		} else {
			throw new IllegalArgumentException("Must specify exactly one of 'file' or 'content'.");
		}
	}

	private String readFileOrContent(FormatterStepConfig config) throws IOException {
		if (content != null) {
			return content;
		} else {
			byte[] raw = Files.readAllBytes(config.getFileLocator().locateFile(file).toPath());
			return new String(raw, config.getEncoding());
		}
	}
}
