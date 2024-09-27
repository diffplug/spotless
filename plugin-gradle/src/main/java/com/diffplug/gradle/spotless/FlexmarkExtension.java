/*
 * Copyright 2023 DiffPlug
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

import java.util.Objects;

import javax.inject.Inject;

import com.vladsch.flexmark.util.data.MutableDataSet;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.markdown.FlexmarkStep;

public class FlexmarkExtension extends FormatExtension {
	static final String NAME = "flexmark";

	@Inject
	public FlexmarkExtension(final SpotlessExtension spotless) {
		super(spotless);
	}

	public FlexmarkFormatterConfig flexmark() {
		return flexmark(FlexmarkStep.defaultVersion());
	}

	public FlexmarkFormatterConfig flexmark(final String version) {
		return new FlexmarkFormatterConfig(version);
	}

	public FlexmarkFormatterConfig flexmark(
		final String version,
		final MutableDataSet options
	) {
		return new FlexmarkFormatterConfig(version, options);
	}

	@Override
	protected void setupTask(final SpotlessTask task) {
		// defaults to all markdown files
		if (this.target == null) {
			throw noDefaultTargetException();
		}
		super.setupTask(task);
	}

	public class FlexmarkFormatterConfig {

		private final String version;
		private final MutableDataSet options;

		FlexmarkFormatterConfig(final String version) {
			this.version = Objects.requireNonNull(version);
			this.options = new MutableDataSet();
			addStep(createStep());
		}

		FlexmarkFormatterConfig(
			final String version,
			final MutableDataSet options
		) {
			this.version = Objects.requireNonNull(version);
			this.options = options;
			addStep(createStep());
		}

		private FormatterStep createStep() {
			return FlexmarkStep.create(
				this.version,
				provisioner(),
				this.options
			);
		}
	}

}
