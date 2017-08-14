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

import java.util.Objects;

import com.diffplug.spotless.kotlin.KtLintStep;

public class KotlinGradleExtension extends FormatExtension {
	private static final String GRADLE_KOTLIN_DSL_FILE_EXTENSION = "*.gradle.kts";

	static final String NAME = "kotlinGradle";

	public KotlinGradleExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
	}

	/** Adds the specified version of [ktlint](https://github.com/shyiko/ktlint). */
	public void ktlint(String version) {
		Objects.requireNonNull(version, "version");
		addStep(KtLintStep.createForScript(version, GradleProvisioner.fromProject(getProject())));
	}

	public void ktlint() {
		ktlint(KtLintStep.defaultVersion());
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			target = parseTarget(GRADLE_KOTLIN_DSL_FILE_EXTENSION);
		}
		super.setupTask(task);
	}
}
