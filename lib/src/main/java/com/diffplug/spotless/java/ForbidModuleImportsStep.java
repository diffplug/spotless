/*
 * Copyright 2025 DiffPlug
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

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.generic.ReplaceRegexStep;

/** Forbids any module import statements. */
public final class ForbidModuleImportsStep {

	/**
	 * Matches lines like 'import module java.base;' or 'import module java.sql;'.
	 */
	private static final String REGEX = "(?m)^import module[^;\\n]*;\\R?";
	private static final String NAME = "forbidModuleImports";
	private static final String ERROR = "Do not use module imports - replace with specific class imports as 'spotlessApply' cannot auto-fix this";

	private ForbidModuleImportsStep() {}

	public static FormatterStep create() {
		return ReplaceRegexStep.lint(NAME, REGEX, ERROR);
	}
}
