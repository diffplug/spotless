/*
 * Copyright 2020 DiffPlug
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
import com.diffplug.spotless.python.BlackStep;

public class PythonExtension extends FormatExtension {
	static final String NAME = "python";

	@Inject
	public PythonExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	public BlackConfig black() {
		return black(BlackStep.defaultVersion());
	}

	public BlackConfig black(String version) {
		return new BlackConfig(version);
	}

	public class BlackConfig {
		BlackStep stepCfg;

		BlackConfig(String version) {
			this.stepCfg = BlackStep.withVersion(version);
			addStep(createStep());
		}

		public BlackConfig pathToExe(String pathToBlack) {
			stepCfg = stepCfg.withPathToExe(pathToBlack);
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return stepCfg.create();
		}
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		// defaults to all markdown files
		if (target == null) {
			throw noDefaultTargetException();
		}
		super.setupTask(task);
	}
}
