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
package com.diffplug.spotless;

import java.util.Objects;

/**
 * Models a linted line or line range. Note that there is no concept of severity level - responsibility
 * for severity and confidence are pushed down to the configuration of the lint tool. If a lint makes it
 * to Spotless, then it is by definition.
 */
public final class Lint {
	private int lineStart, lineEnd; // 1-indexed
	private String code; // e.g. CN_IDIOM https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#cn-class-implements-cloneable-but-does-not-define-or-use-clone-method-cn-idiom
	private String msg;

	private Lint(int lineStart, int lineEnd, String lintCode, String lintMsg) {
		this.lineStart = lineStart;
		this.lineEnd = lineEnd;
		this.code = lintCode;
		this.msg = lintMsg;
	}

	public static Lint create(String code, String msg, int lineStart, int lineEnd) {
		if (lineEnd < lineStart) {
			throw new IllegalArgumentException("lineEnd must be >= lineStart: lineStart=" + lineStart + " lineEnd=" + lineEnd);
		}
		return new Lint(lineStart, lineEnd, code, msg);
	}

	public static Lint create(String code, String msg, int line) {
		return new Lint(line, line, code, msg);
	}

	public int getLineStart() {
		return lineStart;
	}

	public int getLineEnd() {
		return lineEnd;
	}

	public String getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

	@Override
	public String toString() {
		if (lineStart == lineEnd) {
			return lineStart + ": (" + code + ") " + msg;
		} else {
			return lineStart + "-" + lineEnd + ": (" + code + ") " + msg;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Lint lint = (Lint) o;
		return lineStart == lint.lineStart && lineEnd == lint.lineEnd && Objects.equals(code, lint.code) && Objects.equals(msg, lint.msg);
	}

	@Override
	public int hashCode() {
		return Objects.hash(lineStart, lineEnd, code, msg);
	}
}
