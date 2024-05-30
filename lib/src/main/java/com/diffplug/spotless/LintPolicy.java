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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LintPolicy {
	private static final Logger logger = LoggerFactory.getLogger(Formatter.class);

	static void error(Throwable e, FormatterStep step, String relativePath) {
		logger.error("Step '{}' found problem in '{}':\n{}", step.getName(), relativePath, e.getMessage(), e);
	}

	static void warning(Throwable e, FormatterStep step, String relativePath) {
		logger.warn("Unable to apply step '{}' to '{}'", step.getName(), relativePath, e);
	}
}
