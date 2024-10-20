/*
 * Copyright 2022-2024 DiffPlug
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

public final class KtLintCompatReporting {

	private KtLintCompatReporting() {}

	static void report(int line, int column, String ruleId, String detail) {
		throw new KtlintSpotlessException(line, ruleId, detail);
	}

	public static class KtlintSpotlessException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public final int line;
		public final String ruleId;
		public final String detail;

		KtlintSpotlessException(int line, String ruleId, String detail) {
			this.line = line;
			this.ruleId = ruleId;
			this.detail = detail;
		}
	}
}
