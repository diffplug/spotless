/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless;

import java.util.Set;
import java.util.TreeSet;

/**
 * A policy for handling exceptions in the format.  Any exceptions will
 * halt the build except for a specifically excluded path or step.
 */
public class FormatExceptionPolicyStrict extends NoLambda.EqualityBasedOnSerialization implements FormatExceptionPolicy {
	private static final long serialVersionUID = 1L;

	private final Set<String> excludeSteps = new TreeSet<>();
	private final Set<String> excludePaths = new TreeSet<>();

	/** Adds a step name to exclude. */
	public void excludeStep(String stepName) {
		excludeSteps.add(stepName);
	}

	/** Adds a realtive pathx to exclude. */
	public void excludePath(String relativePath) {
		excludePaths.add(relativePath);
	}

	@Override
	public void handleError(Throwable e, FormatterStep step, String relativePath) {
		if (excludeSteps.contains(step.getName())) {
			FormatExceptionPolicyLegacy.warning(e, step, relativePath);
		} else {
			if (excludePaths.contains(relativePath)) {
				FormatExceptionPolicyLegacy.warning(e, step, relativePath);
			} else {
				FormatExceptionPolicyLegacy.error(e, step, relativePath);
				throw ThrowingEx.asRuntimeRethrowError(e);
			}
		}
	}
}
