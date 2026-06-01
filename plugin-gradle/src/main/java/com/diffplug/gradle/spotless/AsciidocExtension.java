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
import com.diffplug.spotless.asciidoc.AsciidocFormatterConfig;
import com.diffplug.spotless.asciidoc.AsciidocFormatterStep;

public class AsciidocExtension extends FormatExtension {
	static final String NAME = "asciidoc";

	@Inject
	public AsciidocExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			throw noDefaultTargetException();
		}
		super.setupTask(task);
	}

	public AsciidocConfig asciidoc() {
		return new AsciidocConfig();
	}

	public class AsciidocConfig {
		private final AsciidocFormatterConfig config = new AsciidocFormatterConfig();

		AsciidocConfig() {
			addStep(createStep());
		}

		public AsciidocConfig normalizeSetextHeadings(boolean value) {
			config.setNormalizeSetextHeadings(value);
			replaceStep(createStep());
			return this;
		}

		public AsciidocConfig collapseConsecutiveBlankLines(boolean value) {
			config.setCollapseConsecutiveBlankLines(value);
			replaceStep(createStep());
			return this;
		}

		public AsciidocConfig oneSentencePerLine(boolean value) {
			config.setOneSentencePerLine(value);
			replaceStep(createStep());
			return this;
		}

		public AsciidocConfig normalizeBlockDelimiters(boolean value) {
			config.setNormalizeBlockDelimiters(value);
			replaceStep(createStep());
			return this;
		}

		public AsciidocConfig removeTrailingHeaderEqualsSign(boolean value) {
			config.setRemoveTrailingHeaderEqualsSign(value);
			replaceStep(createStep());
			return this;
		}

		public AsciidocConfig titleCase(boolean value) {
			config.setTitleCase(value);
			replaceStep(createStep());
			return this;
		}

		public AsciidocConfig removeTrailingWhitespace(boolean value) {
			config.setRemoveTrailingWhitespace(value);
			replaceStep(createStep());
			return this;
		}

		public AsciidocConfig normalizeListBullets(boolean value) {
			config.setNormalizeListBullets(value);
			replaceStep(createStep());
			return this;
		}

		public AsciidocConfig normalizeOrderedListMarkers(boolean value) {
			config.setNormalizeOrderedListMarkers(value);
			replaceStep(createStep());
			return this;
		}

		public AsciidocConfig ensureHeadingBlankLines(boolean value) {
			config.setEnsureHeadingBlankLines(value);
			replaceStep(createStep());
			return this;
		}

		public AsciidocConfig ensureSourceDelimiters(boolean value) {
			config.setEnsureSourceDelimiters(value);
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return AsciidocFormatterStep.create(config);
		}
	}
}
