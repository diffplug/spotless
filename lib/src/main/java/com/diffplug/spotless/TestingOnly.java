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

import java.util.Locale;

/** These FormatterStep are meant to be used for testing only. */
public class TestingOnly {
	public static FormatterStep lowercase() {
		return FormatterStep.create("lowercase", "lowercaseStateUnused", unused -> TestingOnly::lowercase);
	}

	private static String lowercase(String raw) {
		return raw.toLowerCase(Locale.ROOT);
	}

	public static FormatterStep diverge() {
		return FormatterStep.create("diverge", "divergeStateUnused", unused -> TestingOnly::diverge);
	}

	private static String diverge(String raw) {
		return raw + " ";
	}
}
