/*
 * Copyright 2026 DiffPlug
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

import javax.inject.Inject;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.toml.VersionCatalogStep;

public class TomlExtension extends FormatExtension {
	static final String NAME = "toml";

	@Inject
	public TomlExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			throw noDefaultTargetException();
		}
		super.setupTask(task);
	}

	public VersionCatalogConfig versionCatalog() {
		return new VersionCatalogConfig();
	}

	public class VersionCatalogConfig {
		private boolean stripQuotedKeys;
		private int maxLineLength = 120;

		public VersionCatalogConfig() {
			this.stripQuotedKeys = false;
			addStep(createStep());
		}

		public void stripQuotedKeys(boolean stripQuotedKeys) {
			this.stripQuotedKeys = stripQuotedKeys;
			replaceStep(createStep());
		}

		public void maxLineLength(int maxLineLength) {
			this.maxLineLength = maxLineLength;
			replaceStep(createStep());
		}

		private FormatterStep createStep() {
			return VersionCatalogStep.create(stripQuotedKeys, maxLineLength);
		}
	}
}
