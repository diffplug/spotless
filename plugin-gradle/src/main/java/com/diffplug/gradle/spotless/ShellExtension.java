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
package com.diffplug.gradle.spotless;

import java.util.Objects;

import javax.inject.Inject;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.shell.ShfmtStep;

public class ShellExtension extends FormatExtension {
	private static final String SHELL_FILE_EXTENSION = "*.sh";

	static final String NAME = "shell";

	@Inject
	public ShellExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	/** If the user hasn't specified files, assume all shell files should be checked. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			target = parseTarget(SHELL_FILE_EXTENSION);
		}
		super.setupTask(task);
	}

	/** Adds the specified version of <a href="https://github.com/mvdan/sh">shfmt</a>. */
	public ShfmtExtension shfmt(String version) {
		Objects.requireNonNull(version);
		return new ShfmtExtension(version);
	}

	/** Adds the specified version of <a href="https://github.com/mvdan/sh">shfmt</a>. */
	public ShfmtExtension shfmt() {
		return shfmt(ShfmtStep.defaultVersion());
	}

	public class ShfmtExtension {
		ShfmtStep step;

		ShfmtExtension(String version) {
			this.step = ShfmtStep.withVersion(version);
			addStep(createStep());
		}

		public ShfmtExtension pathToExe(String pathToExe) {
			step = step.withPathToExe(pathToExe);
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return step.create();
		}
	}
}
