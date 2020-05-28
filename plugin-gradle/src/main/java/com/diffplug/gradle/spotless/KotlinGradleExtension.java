/*
 * Copyright 2016 DiffPlug
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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.diffplug.common.collect.ImmutableSortedMap;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.kotlin.KtLintStep;
import com.diffplug.spotless.kotlin.KtfmtStep;

public class KotlinGradleExtension extends FormatExtension {
	private static final String GRADLE_KOTLIN_DSL_FILE_EXTENSION = "*.gradle.kts";

	static final String NAME = "kotlinGradle";

	public KotlinGradleExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
	}

	/** Adds the specified version of [ktlint](https://github.com/pinterest/ktlint). */
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

		private FormatterStep createStep() {
			return KtLintStep.createForScript(version, GradleProvisioner.fromProject(getProject()), userData);
		}
	}

	/** Uses the [ktfmt](https://github.com/facebookincubator/ktfmt) jar to format source code. */
	public KtfmtConfig ktfmt() {
		return ktfmt(KtfmtStep.defaultVersion());
	}

	/**
	 * Uses the given version of [ktfmt](https://github.com/facebookincubator/ktfmt) to format source
	 * code.
	 */
	public KtfmtConfig ktfmt(String version) {
		Objects.requireNonNull(version);
		return new KtfmtConfig(version);
	}

	public class KtfmtConfig {
		final String version;

		KtfmtConfig(String version) {
			this.version = Objects.requireNonNull(version);
			addStep(createStep());
		}

		private FormatterStep createStep() {
			return KtfmtStep.create(version, GradleProvisioner.fromProject(getProject()));
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
