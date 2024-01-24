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
package com.diffplug.spotless.java;

import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterStep;

public final class IdeaStep {

	private IdeaStep() {}

	public static FormatterStep create() {
		return create(true);
	}

	public static FormatterStep create(boolean withDefaults) {
		return create(withDefaults, null);
	}

	public static FormatterStep create(boolean withDefaults,
			@Nullable String binaryPath) {
		return create(withDefaults, binaryPath, null);
	}

	public static FormatterStep create(boolean withDefaults,
			@Nullable String binaryPath, @Nullable String configPath) {
		IdeaFormatterFunc formatterFunc = getFormatterFunc(withDefaults, binaryPath, configPath);
		// TODO: make it lazy
		return FormatterStep.createNeverUpToDate("IDEA", formatterFunc);
	}

	private static IdeaFormatterFunc getFormatterFunc(boolean withDefaults,
			@Nullable String binaryPath, @Nullable String configPath) {
		if (withDefaults) {
			return IdeaFormatterFunc
					.allowingDefaultsWithCustomBinary(binaryPath, configPath);
		}
		return IdeaFormatterFunc.noDefaultsWithCustomBinary(binaryPath, configPath);
	}

}
