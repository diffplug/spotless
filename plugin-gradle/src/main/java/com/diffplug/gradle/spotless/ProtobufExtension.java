/*
 * Copyright 2022 DiffPlug
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
package com.diffplug.gradle.spotless;

import static com.diffplug.spotless.protobuf.ProtobufConstants.LICENSE_HEADER_DELIMITER;

import java.util.Objects;

import javax.inject.Inject;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.protobuf.BufStep;

public class ProtobufExtension extends FormatExtension implements HasBuiltinDelimiterForLicense {
	static final String NAME = "protobuf";

	@Inject
	public ProtobufExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	@Override
	public LicenseHeaderConfig licenseHeader(String licenseHeader) {
		return licenseHeader(licenseHeader, LICENSE_HEADER_DELIMITER);
	}

	@Override
	public LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile) {
		return licenseHeaderFile(licenseHeaderFile, LICENSE_HEADER_DELIMITER);
	}

	/** Adds the specified version of <a href="https://buf.build/">buf</a>. */
	public BufFormatExtension buf(String version) {
		Objects.requireNonNull(version);
		return new BufFormatExtension(version);
	}

	public BufFormatExtension buf() {
		return buf(BufStep.defaultVersion());
	}

	public class BufFormatExtension {
		private final String version;

		BufFormatExtension(String version) {
			this.version = version;
			addStep(createStep());
		}

		private FormatterStep createStep() {
			return BufStep.withVersion(version).create();
		}
	}

	/** If the user hasn't specified files, assume all protobuf files should be checked. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			target = parseTarget("**/*.proto");
		}
		super.setupTask(task);
	}
}
