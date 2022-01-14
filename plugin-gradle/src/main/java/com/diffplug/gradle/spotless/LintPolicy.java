/*
 * Copyright 2022 DiffPlug
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
import java.util.Set;
import java.util.TreeSet;

import com.diffplug.spotless.Lint;
import com.diffplug.spotless.NoLambda;

public class LintPolicy extends NoLambda.EqualityBasedOnSerialization {
	private final Set<String> excludeSteps = new TreeSet<>();
	private final Set<String> excludePaths = new TreeSet<>();

	/** Adds a step name to exclude. */
	public void excludeStep(String stepName) {
		excludeSteps.add(Objects.requireNonNull(stepName));
	}

	/** Adds a relative path to exclude. */
	public void excludePath(String relativePath) {
		excludePaths.add(Objects.requireNonNull(relativePath));
	}

	public boolean runLintOn(String path) {
		return !excludePaths.contains(path);
	}

	public boolean includeLint(String path, Lint lint) {
		return runLintOn(path) && !excludeSteps.contains(lint.getCode());
	}
}
