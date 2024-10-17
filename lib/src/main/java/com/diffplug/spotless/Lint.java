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
package com.diffplug.spotless;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Models a linted line or line range. Note that there is no concept of severity level - responsibility
 * for severity and confidence are pushed down to the configuration of the lint tool. If a lint makes it
 * to Spotless, then it is by definition.
 */
public final class Lint implements Serializable {
	/** Returns a runtime exception which, if thrown, will create a lint at an undefined line. */
	public static ShortcutException atUndefinedLine(String code, String msg) {
		return new ShortcutException(Lint.create(code, msg, LINE_UNDEFINED));
	}

	/** Returns a runtime exception which, if thrown, will lint a specific line. */
	public static ShortcutException atLine(int line, String code, String msg) {
		return new ShortcutException(Lint.create(code, msg, line));
	}

	/** Returns a runtime exception which, if thrown, will lint a specific line range. */
	public static ShortcutException atLineRange(int lineStart, int lineEnd, String code, String msg) {
		return new ShortcutException(Lint.create(code, msg, lineStart, lineEnd));
	}

	/** Any exception which implements this interface will have its lints extracted and reported cleanly to the user. */
	public interface Has {
		List<Lint> getLints();
	}

	/** An exception for shortcutting execution to report a lint to the user. */
	public static class ShortcutException extends RuntimeException implements Has {
		public ShortcutException(Lint... lints) {
			this(Arrays.asList(lints));
		}

		private final List<Lint> lints;

		public ShortcutException(Collection<Lint> lints) {
			this.lints = List.copyOf(lints);
		}

		@Override
		public List<Lint> getLints() {
			return lints;
		}
	}

	private static final long serialVersionUID = 1L;

	private int lineStart, lineEnd; // 1-indexed, inclusive
	private String code; // e.g. CN_IDIOM https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#cn-class-implements-cloneable-but-does-not-define-or-use-clone-method-cn-idiom
	private String msg;

	private Lint(int lineStart, int lineEnd, String lintCode, String lintMsg) {
		this.lineStart = lineStart;
		this.lineEnd = lineEnd;
		this.code = LineEnding.toUnix(lintCode);
		this.msg = LineEnding.toUnix(lintMsg);
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
			if (lineStart == LINE_UNDEFINED) {
				return "LINE_UNDEFINED: (" + code + ") " + msg;
			} else {
				return lineStart + ": (" + code + ") " + msg;
			}
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

	/** Attempts to parse a line number from the given exception. */
	static Lint createFromThrowable(FormatterStep step, String content, Throwable e) {
		Throwable current = e;
		while (current != null) {
			String message = current.getMessage();
			int lineNumber = lineNumberFor(message);
			if (lineNumber != -1) {
				return Lint.create(step.getName(), msgFrom(message), lineNumber);
			}
			current = current.getCause();
		}
		int numNewlines = (int) content.codePoints().filter(c -> c == '\n').count();
		return Lint.create(step.getName(), ThrowingEx.stacktrace(e), 1, 1 + numNewlines);
	}

	private static int lineNumberFor(String message) {
		if (message == null) {
			return -1;
		}
		int firstColon = message.indexOf(':');
		if (firstColon == -1) {
			return -1;
		}
		String candidateNum = message.substring(0, firstColon);
		try {
			return Integer.parseInt(candidateNum);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static String msgFrom(String message) {
		for (int i = 0; i < message.length(); ++i) {
			if (Character.isLetter(message.charAt(i))) {
				return message.substring(i);
			}
		}
		return "";
	}

	public static final int LINE_UNDEFINED = -1;
}
