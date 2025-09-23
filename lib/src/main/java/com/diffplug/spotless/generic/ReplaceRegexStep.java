/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless.generic;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Lint;

public final class ReplaceRegexStep {
	// prevent direct instantiation
	private ReplaceRegexStep() {}

	public static FormatterStep create(String name, String regex, String replacement) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(regex, "regex");
		Objects.requireNonNull(replacement, "replacement");
		return FormatterStep.createLazy(name,
				() -> new State(Pattern.compile(regex, Pattern.UNIX_LINES | Pattern.MULTILINE), replacement),
				State::toFormatter);
	}

	public static FormatterStep lint(String name, String regex, String lintDetail) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(regex, "regex");
		Objects.requireNonNull(lintDetail, "lintDetail");
		return FormatterStep.createLazy(name,
				() -> new LintState(Pattern.compile(regex, Pattern.UNIX_LINES | Pattern.MULTILINE), name, lintDetail),
				LintState::toLinter);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final Pattern regex;
		private final String replacement;

		State(Pattern regex, String replacement) {
			this.regex = regex;
			this.replacement = replacement;
		}

		FormatterFunc toFormatter() {
			return raw -> regex.matcher(raw).replaceAll(replacement);
		}
	}

	private static final class LintState implements Serializable {
		private static final long serialVersionUID = 1L;

		private final Pattern regex;
		private final String ruleId;
		private final String lintDetail;

		LintState(Pattern regex, String ruleId, String lintDetail) {
			this.regex = regex;
			this.ruleId = ruleId;
			this.lintDetail = lintDetail;
		}

		FormatterFunc toLinter() {
			return new FormatterFunc() {
				@Override
				public String apply(String raw) {
					return raw;
				}

				@Override
				public List<Lint> lint(String raw, File file) {
					List<Lint> lints = new ArrayList<>();
					var matcher = regex.matcher(raw);
					while (matcher.find()) {
						int line = 1 + (int) raw.codePoints().limit(matcher.start()).filter(c -> c == '\n').count();
						String errorCode = matcher.group(0).trim();
						int firstNewline = errorCode.indexOf("\n");
						if (firstNewline != -1) {
							errorCode = errorCode.substring(0, firstNewline);
						}
						lints.add(Lint.atLine(line, errorCode, lintDetail));
					}
					return lints;
				}
			};
		}
	}
}
