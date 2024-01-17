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

import javax.inject.Inject;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.go.GofmtFormatStep;

public class GoExtension extends FormatExtension {
	public static final String NAME = "go";

	@Inject
	public GoExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	public GofmtConfig gofmt() {
		return new GofmtConfig(GofmtFormatStep.defaultVersion());
	}

	public GofmtConfig gofmt(String version) {
		return new GofmtConfig(version);
	}

	public class GofmtConfig {
		GofmtFormatStep stepCfg;

		public GofmtConfig(String version) {
			stepCfg = GofmtFormatStep.withVersion(version);
			addStep(createStep());
		}

		public GofmtConfig withGoExecutable(String pathToGo) {
			stepCfg = stepCfg.withGoExecutable(pathToGo);
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return stepCfg.create();
		}
	}
}
