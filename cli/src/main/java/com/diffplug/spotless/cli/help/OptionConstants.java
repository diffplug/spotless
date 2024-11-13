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
package com.diffplug.spotless.cli.help;

public final class OptionConstants {

	public static final String NEW_LINE = "%n  ";

	public static final String VALID_VALUES_SUFFIX = NEW_LINE + "One of: ${COMPLETION-CANDIDATES}";

	public static final String DEFAULT_VALUE_SUFFIX = NEW_LINE + "(default: ${DEFAULT-VALUE})";

	public static final String VALID_AND_DEFAULT_VALUES_SUFFIX = VALID_VALUES_SUFFIX + DEFAULT_VALUE_SUFFIX;

	private OptionConstants() {
		// no instance
	}
}
