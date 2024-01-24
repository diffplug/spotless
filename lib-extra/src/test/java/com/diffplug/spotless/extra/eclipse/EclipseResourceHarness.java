/*
 * Copyright 2016-2024 DiffPlug
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
package com.diffplug.spotless.extra.eclipse;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.Arrays;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;

/**
 * Provides a common formatter test for all Spotless Eclipse Formatter steps
 * provided by lib-extra.
 * <p>
 * The external Spotless Eclipse Formatter Step implementations are responsible
 * to test the functionality of the Spotless adaptations for the Eclipse formatter.
 * This explicitly includes a check of various forms of valid and invalid input
 * to cover all relevant execution paths during the code formatting and internal
 * exception handling.
 * </p>
 * <p>
 * The lib-extra users, like plugin-gradle and plugin-maven are responsible
 * to test the correct provision of user settings to the generic
 * Spotless Eclipse Formatter steps provided by lib-extra.
 * </p>
 */
public class EclipseResourceHarness extends ResourceHarness {
	private final EclipseBasedStepBuilder stepBuilder;

	/**
	 * Create harness to be used for several versions of the formatter step
	 * @param builder Eclipse Formatter step builder
	 */
	public EclipseResourceHarness(EclipseBasedStepBuilder builder) {
		this.stepBuilder = builder;
	}

	protected StepHarnessWithFile harnessFor(String formatterVersion, File... settingsFiles) throws Exception {
		stepBuilder.setVersion(formatterVersion);
		stepBuilder.setPreferences(Arrays.asList(settingsFiles));
		FormatterStep step = stepBuilder.build();
		return StepHarnessWithFile.forStep(this, step);
	}
}
