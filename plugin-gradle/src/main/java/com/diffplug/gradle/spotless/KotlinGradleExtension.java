/*
 * Copyright 2016-2021 DiffPlug
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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import com.diffplug.common.collect.ImmutableSortedMap;
import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.kotlin.DiktatStep;
import com.diffplug.spotless.kotlin.KtLintStep;
import com.diffplug.spotless.kotlin.KtfmtStep;
import com.diffplug.spotless.kotlin.KtfmtStep.Style;

public class KotlinGradleExtension extends FormatExtension {
	private static final String GRADLE_KOTLIN_DSL_FILE_EXTENSION = "*.gradle.kts";

	static final String NAME = "kotlinGradle";

	@Inject
	public KotlinGradleExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	/** Adds the specified version of <a href="https://github.com/pinterest/ktlint">ktlint</a>. */
	public KotlinFormatExtension ktlint(String version) {
		Objects.requireNonNull(version, "version");
		return new KotlinFormatExtension(version, Collections.emptyMap());
	}

	public KotlinFormatExtension ktlint() {
		return ktlint(KtLintStep.defaultVersion());
	}

	public class KotlinFormatExtension {

		private final String version;
		private Map<String, String> userData;
		private String editorConfig;

		KotlinFormatExtension(String version, Map<String, String> config) {
			this.version = version;
			this.userData = config;
			addStep(createStep());
		}

		public void userData(Map<String, String> userData) {
			// Copy the map to a sorted map because up-to-date checking is based on binary-equals of the serialized
			// representation.
			this.userData = ImmutableSortedMap.copyOf(userData);
			replaceStep(createStep());
		}

		public void editorConfig(String editorConfig) {
			this.editorConfig = editorConfig;
			replaceStep(createStep());
		}

		private FormatterStep createStep() {
			return KtLintStep.createForScript(version, provisioner(), userData, editorConfig);
		}
	}

	/** Uses the <a href="https://github.com/facebookincubator/ktfmt">ktfmt</a> jar to format source code. */
	public KtfmtConfig ktfmt() {
		return ktfmt(KtfmtStep.defaultVersion());
	}

	/**
	 * Uses the given version of <a href="https://github.com/facebookincubator/ktfmt">ktfmt</a> to format source
	 * code.
	 */
	public KtfmtConfig ktfmt(String version) {
		Objects.requireNonNull(version);
		return new KtfmtConfig(version);
	}

	public class KtfmtConfig {
		final String version;
		Style style;

		KtfmtConfig(String version) {
			this.version = Objects.requireNonNull(version);
			this.style = Style.DEFAULT;
			addStep(createStep());
		}

		public void style(Style style) {
			this.style = style;
			replaceStep(createStep());
		}

		public void dropboxStyle() {
			style(Style.DROPBOX);
		}

		public void googleStyle() {
			style(Style.GOOGLE);
		}

		public void kotlinlangStyle() {
			style(Style.KOTLINLANG);
		}

		private FormatterStep createStep() {
			return KtfmtStep.create(version, provisioner(), style);
		}
	}

	/** Adds the specified version of <a href="https://github.com/cqfn/diKTat">diktat</a>. */
	public DiktatFormatExtension diktat(String version) {
		Objects.requireNonNull(version, "version");
		return new DiktatFormatExtension(version);
	}

	public DiktatFormatExtension diktat() {
		return diktat(DiktatStep.defaultVersionDiktat());
	}

	public class DiktatFormatExtension {

		private final String version;
		private FileSignature config;

		DiktatFormatExtension(String version) {
			this.version = version;
			addStep(createStep());
		}

		public DiktatFormatExtension configFile(Object file) throws IOException {
			// Specify the path to the configuration file
			if (file == null) {
				this.config = null;
			} else {
				this.config = FileSignature.signAsList(getProject().file(file));
			}
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return DiktatStep.createForScript(version, provisioner(), config);
		}
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			target = parseTarget(GRADLE_KOTLIN_DSL_FILE_EXTENSION);
		}
		super.setupTask(task);
	}
}
