/*
 * Copyright 2021 DiffPlug
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
import com.diffplug.spotless.terraform.TerraformFmtStep;

public class TerraformExtension extends FormatExtension {
	static final String NAME = "terraform";

	@Inject
	public TerraformExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	public TerraformFmtConfig fmt() {
		return fmt(TerraformFmtStep.defaultVersion());
	}

	public TerraformFmtConfig fmt(String version) {
		return new TerraformFmtConfig(version);
	}

	public class TerraformFmtConfig {
		TerraformFmtStep stepCfg;

		TerraformFmtConfig(String version) {
			this.stepCfg = TerraformFmtStep.withVersion(version);
			addStep(createStep());
		}

		public TerraformFmtConfig pathToExe(String pathToTerraformCli) {
			stepCfg = stepCfg.withPathToExe(pathToTerraformCli);
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return stepCfg.create();
		}
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			throw noDefaultTargetException();
		}
		super.setupTask(task);
	}
}
