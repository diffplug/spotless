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

import javax.inject.Inject;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.gherkin.GherkinSimpleStep;

public class GherkinExtension extends FormatExtension {
	private static final int DEFAULT_INDENTATION = 4;
	static final String NAME = "gherkin";

	@Inject
	public GherkinExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			throw noDefaultTargetException();
		}
		super.setupTask(task);
	}

	public SimpleConfig simple() {
		return new SimpleConfig(DEFAULT_INDENTATION);
	}

	public class SimpleConfig {
		private int indent;

		public SimpleConfig(int indent) {
			this.indent = indent;
			addStep(createStep());
		}

		public void indentWithSpaces(int indent) {
			this.indent = indent;
			replaceStep(createStep());
		}

		private FormatterStep createStep() {
			return GherkinSimpleStep.create(indent, provisioner());
		}
	}

}
