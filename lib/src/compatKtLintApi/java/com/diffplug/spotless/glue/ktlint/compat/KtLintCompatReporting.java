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
package com.diffplug.spotless.glue.ktlint.compat;

import java.util.ArrayList;

final class KtLintCompatReporting {

	private KtLintCompatReporting() {}

	static void addReport(ArrayList<String> errors, int line, int column, String ruleId, String detail) {
		StringBuilder sb = new StringBuilder("Error on line: ");
		sb.append(line).append(", column: ").append(column).append(System.lineSeparator()).append("rule: ").append(ruleId).append(System.lineSeparator()).append(detail);
		errors.add(sb.toString());
	}

	static String report(final ArrayList<String> errors) {
		StringBuilder output = new StringBuilder();
		output.append("There are ").append(errors.size()).append(" unfixed errors:").append(System.lineSeparator());
		for (final String error : errors) {
			output.append(error).append(System.lineSeparator());
		}
		throw new AssertionError(output);
	}
}
