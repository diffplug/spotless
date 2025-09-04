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
import com.diffplug.spotless.generic.LintRegexStep;

/** Removes any wildcard import statements. */
public final class RemoveWildcardImportsStep {

	/**
	 * Matches lines like 'import foo.*;' or 'import static foo.*;'.
	 */
	private static final String REGEX = "(?m)^import\\s+(?:static\\s+)?[^;\\n]*\\*;\\R?";
	private static final String NAME = "removeWildcardImports";
	private static final String ERROR = "Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this";

	private RemoveWildcardImportsStep() {}

	public static FormatterStep create() {
		return LintRegexStep.lint(NAME, REGEX, ERROR);
	}
}
