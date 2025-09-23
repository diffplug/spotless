/*
 * Copyright 2022-2025 DiffPlug
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Models a linted line or line range. Note that there is no concept of severity level - responsibility
 * for severity and confidence are pushed down to the configuration of the lint tool. If a lint makes it
 * to Spotless, then it is by definition.
 */
public final class Lint implements Serializable {
	public static Lint atUndefinedLine(String ruleId, String detail) {
		return new Lint(LINE_UNDEFINED, ruleId, detail);
	}

	public static Lint atLine(int line, String ruleId, String detail) {
		return new Lint(line, ruleId, detail);
	}

	public static Lint atLineRange(int lineStart, int lineEnd, String shortCode, String detail) {
		return new Lint(lineStart, lineEnd, shortCode, detail);
	}

	private static final long serialVersionUID = 1L;

	private int lineStart, lineEnd; // 1-indexed, inclusive
	private String shortCode; // e.g. CN_IDIOM https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#cn-class-implements-cloneable-but-does-not-define-or-use-clone-method-cn-idiom
	private String detail;

	private Lint(int lineStart, int lineEnd, String shortCode, String detail) {
		if (lineEnd < lineStart) {
			throw new IllegalArgumentException("lineEnd must be >= lineStart: lineStart=" + lineStart + " lineEnd=" + lineEnd);
		}
		this.lineStart = lineStart;
		this.lineEnd = lineEnd;
		this.shortCode = LineEnding.toUnix(shortCode);
		this.detail = LineEnding.toUnix(detail);
	}

	private Lint(int line, String shortCode, String detail) {
		this(line, line, shortCode, detail);
	}

	public int getLineStart() {
		return lineStart;
	}

	public int getLineEnd() {
		return lineEnd;
	}

	public String getShortCode() {
		return shortCode;
	}

	public String getDetail() {
		return detail;
	}

	/** Any exception which implements this interface will have its lints extracted and reported cleanly to the user. */
	public interface Has {
		List<Lint> getLints();
	}

	/** An exception for shortcutting execution to report a lint to the user. */
	static class ShortcutException extends RuntimeException implements Has {
		public ShortcutException(Lint... lints) {
			this(Arrays.asList(lints));
		}

		private final List<Lint> lints;

		ShortcutException(Collection<Lint> lints) {
			super(lints.iterator().next().toString());
			this.lints = List.copyOf(lints);
		}

		@Override
		public List<Lint> getLints() {
			return lints;
		}
	}

	/** Returns an exception which will wrap all of the given lints using {@link Has} */
	public static RuntimeException shortcut(Collection<Lint> lints) {
		return new ShortcutException(lints);
	}

	/** Returns an exception which will wrap this lint using {@link Has} */
	public RuntimeException shortcut() {
		return new ShortcutException(this);
	}

	@Override
	public String toString() {
		if (lineStart == lineEnd) {
			if (lineStart == LINE_UNDEFINED) {
				return "LINE_UNDEFINED: (" + shortCode + ") " + detail;
			} else {
				return "L" + lineStart + ": (" + shortCode + ") " + detail;
			}
		} else {
			return "L" + lineStart + "-" + lineEnd + ": (" + shortCode + ") " + detail;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Lint lint = (Lint) o;
		return lineStart == lint.lineStart && lineEnd == lint.lineEnd && Objects.equals(shortCode, lint.shortCode) && Objects.equals(detail, lint.detail);
	}

	@Override
	public int hashCode() {
		return Objects.hash(lineStart, lineEnd, shortCode, detail);
	}

	/** Attempts to parse a line number from the given exception. */
	static Lint createFromThrowable(FormatterStep step, Throwable e) {
		Throwable current = e;
		while (current != null) {
			String message = current.getMessage();
			int lineNumber = lineNumberFor(message);
			if (lineNumber != -1) {
				return new Lint(lineNumber, step.getName(), msgFrom(message));
			}
			current = current.getCause();
		}
		String exceptionName = e.getClass().getName();
		String detail = ThrowingEx.stacktrace(e);
		if (detail.startsWith(exceptionName + ": ")) {
			detail = detail.substring(exceptionName.length() + 2);
		}
		Matcher matcher = Pattern.compile("line (\\d+)").matcher(detail);
		int line = LINE_UNDEFINED;
		if (matcher.find()) {
			line = Integer.parseInt(matcher.group(1));
		}
		return Lint.atLine(line, exceptionName, detail);
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

	public void addWarningMessageTo(StringBuilder buffer, String stepName, boolean oneLine) {
		if (lineStart == Lint.LINE_UNDEFINED) {
			buffer.append("LINE_UNDEFINED");
		} else {
			buffer.append("L");
			buffer.append(lineStart);
			if (lineEnd != lineStart) {
				buffer.append("-").append(lineEnd);
			}
		}
		buffer.append(" ");
		buffer.append(stepName).append("(").append(shortCode).append(") ");

		int firstNewline = detail.indexOf('\n');
		if (firstNewline == -1) {
			buffer.append(detail);
		} else if (oneLine) {
			buffer.append(detail, 0, firstNewline);
			buffer.append(" (...)");
		} else {
			buffer.append(detail);
		}
	}
}
