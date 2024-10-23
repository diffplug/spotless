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
package com.diffplug.spotless;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LintPolicy {
	static void legacyBehavior(Formatter formatter, File file, ValuePerStep<Throwable> exceptionPerStep) {
		for (int i = 0; i < formatter.getSteps().size(); ++i) {
			Throwable exception = exceptionPerStep.get(i);
			if (exception != null && exception != LintState.formatStepCausedNoChange()) {
				logger.error("Step '{}' found problem in '{}':\n{}", formatter.getSteps().get(i), file.getName(), exception.getMessage(), exception);
				throw ThrowingEx.asRuntimeRethrowError(exception);
			}
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(Formatter.class);
}
