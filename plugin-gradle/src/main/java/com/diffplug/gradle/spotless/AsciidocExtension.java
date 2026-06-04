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

import java.util.List;

import javax.inject.Inject;

import com.diffplug.spotless.asciidoc.AdocfmtConfig;
import com.diffplug.spotless.asciidoc.AdocfmtStep;

public class AsciidocExtension extends FormatExtension {
	static final String NAME = "asciidoc";

	@Inject
	public AsciidocExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	public AdocfmtFormatterConfig adocfmt() {
		return adocfmt(AdocfmtStep.defaultVersion());
	}

	public AdocfmtFormatterConfig adocfmt(String version) {
		return new AdocfmtFormatterConfig(version);
	}

	public class AdocfmtFormatterConfig {
		private final String version;
		private final AdocfmtConfig config = new AdocfmtConfig();

		AdocfmtFormatterConfig(String version) {
			this.version = version;
			addStep(AdocfmtStep.create(version, provisioner(), config));
		}

		public AdocfmtFormatterConfig normalizeSetextHeadings(boolean normalizeSetextHeadings) {
			config.normalizeSetextHeadings = normalizeSetextHeadings;
			replaceStep(AdocfmtStep.create(version, provisioner(), config));
			return this;
		}

		public AdocfmtFormatterConfig collapseConsecutiveBlankLines(boolean collapseConsecutiveBlankLines) {
			config.collapseConsecutiveBlankLines = collapseConsecutiveBlankLines;
			replaceStep(AdocfmtStep.create(version, provisioner(), config));
			return this;
		}

		public AdocfmtFormatterConfig oneSentencePerLine(boolean oneSentencePerLine) {
			config.oneSentencePerLine = oneSentencePerLine;
			replaceStep(AdocfmtStep.create(version, provisioner(), config));
			return this;
		}

		public AdocfmtFormatterConfig normalizeBlockDelimiters(boolean normalizeBlockDelimiters) {
			config.normalizeBlockDelimiters = normalizeBlockDelimiters;
			replaceStep(AdocfmtStep.create(version, provisioner(), config));
			return this;
		}

		public AdocfmtFormatterConfig removeTrailingHeaderEqualsSign(boolean removeTrailingHeaderEqualsSign) {
			config.removeTrailingHeaderEqualsSign = removeTrailingHeaderEqualsSign;
			replaceStep(AdocfmtStep.create(version, provisioner(), config));
			return this;
		}

		public AdocfmtFormatterConfig titleCase(boolean titleCase) {
			config.titleCase = titleCase;
			replaceStep(AdocfmtStep.create(version, provisioner(), config));
			return this;
		}

		public AdocfmtFormatterConfig removeTrailingWhitespace(boolean removeTrailingWhitespace) {
			config.removeTrailingWhitespace = removeTrailingWhitespace;
			replaceStep(AdocfmtStep.create(version, provisioner(), config));
			return this;
		}

		public AdocfmtFormatterConfig normalizeListBullets(boolean normalizeListBullets) {
			config.normalizeListBullets = normalizeListBullets;
			replaceStep(AdocfmtStep.create(version, provisioner(), config));
			return this;
		}

		public AdocfmtFormatterConfig normalizeOrderedListMarkers(boolean normalizeOrderedListMarkers) {
			config.normalizeOrderedListMarkers = normalizeOrderedListMarkers;
			replaceStep(AdocfmtStep.create(version, provisioner(), config));
			return this;
		}

		public AdocfmtFormatterConfig ensureHeadingBlankLines(boolean ensureHeadingBlankLines) {
			config.ensureHeadingBlankLines = ensureHeadingBlankLines;
			replaceStep(AdocfmtStep.create(version, provisioner(), config));
			return this;
		}

		public AdocfmtFormatterConfig ensureSourceDelimiters(boolean ensureSourceDelimiters) {
			config.ensureSourceDelimiters = ensureSourceDelimiters;
			replaceStep(AdocfmtStep.create(version, provisioner(), config));
			return this;
		}
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			target = parseTarget(List.of("**/*.adoc", "**/*.asciidoc"));
		}
		super.setupTask(task);
	}
}
