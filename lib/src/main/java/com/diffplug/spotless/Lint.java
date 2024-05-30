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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

	/** Guaranteed to have no newlines, but also guarantees to preserve all newlines and parenthesis in code and msg. */
	String asOneLine() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(Integer.toString(lineStart));
		if (lineStart != lineEnd) {
			buffer.append('-');
			buffer.append(Integer.toString(lineEnd));
		}
		buffer.append(OPEN);
		buffer.append(safeParensAndNewlines.escape(code));
		buffer.append(CLOSE);
		buffer.append(safeParensAndNewlines.escape(msg));
		return buffer.toString();
	}

	private static final String OPEN = ": (";
	private static final String CLOSE = ") ";

	static Lint fromOneLine(String content) {
		int codeOpen = content.indexOf(OPEN);
		int codeClose = content.indexOf(CLOSE, codeOpen);

		int lineStart, lineEnd;
		String lineNumber = content.substring(0, codeOpen);
		int idxDash = lineNumber.indexOf('-');
		if (idxDash == -1) {
			lineStart = Integer.parseInt(lineNumber);
			lineEnd = lineStart;
		} else {
			lineStart = Integer.parseInt(lineNumber.substring(0, idxDash));
			lineEnd = Integer.parseInt(lineNumber.substring(idxDash + 1));
		}

		String code = safeParensAndNewlines.unescape(content.substring(codeOpen + OPEN.length(), codeClose));
		String msg = safeParensAndNewlines.unescape(content.substring(codeClose + CLOSE.length()));
		return Lint.create(code, msg, lineStart, lineEnd);
	}

	/** Call .escape to get a string which is guaranteed to have no parenthesis or newlines, and you can call unescape to get the original back. */
	static final PerCharacterEscaper safeParensAndNewlines = PerCharacterEscaper.specifiedEscape("\\\\\nn(₍)₎");

	/** Converts a list of lints to a String, format is not guaranteed to be consistent from version to version of Spotless. */
	public static String toString(List<Lint> lints) {
		StringBuilder builder = new StringBuilder();
		for (Lint lint : lints) {
			builder.append(lint.asOneLine());
			builder.append('\n');
		}
		return builder.toString();
	}

	/** Converts a list of lints to a String, format is not guaranteed to be consistent from version to version of Spotless. */
	public static List<Lint> fromString(String content) {
		List<Lint> lints = new ArrayList<>();
		String[] lines = content.split("\n");
		for (String line : lines) {
			line = line.trim();
			if (!line.isEmpty()) {
				lints.add(fromOneLine(line));
			}
		}
		return lints;
	}

	public static List<Lint> fromFile(File file) throws IOException {
		byte[] content = Files.readAllBytes(file.toPath());
		return fromString(new String(content, StandardCharsets.UTF_8));
	}

	public static void toFile(List<Lint> lints, File file) throws IOException {
		Path path = file.toPath();
		Path parent = path.getParent();
		if (parent == null) {
			throw new IllegalArgumentException("file has no parent dir");
		}
		Files.createDirectories(parent);
		byte[] content = toString(lints).getBytes(StandardCharsets.UTF_8);
		Files.write(path, content);
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
}
