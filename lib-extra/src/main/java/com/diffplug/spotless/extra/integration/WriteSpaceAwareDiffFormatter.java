/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless.extra.integration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.util.IntList;
import org.eclipse.jgit.util.RawParseUtils;

/**
 * Formats the diff in Git-like style, however it makes whitespace visible for
 * edit-like diffs (when one fragment is replaced with another).
 */
class WriteSpaceAwareDiffFormatter {
	private static final int CONTEXT_LINES = 3;
	private static final String MIDDLE_DOT = "\u00b7";
	private static final String CR = "\u240d";
	private static final String LF = "\u240a";
	private static final String TAB = "\u21e5";
	private static final byte[] MIDDLE_DOT_UTF8 = MIDDLE_DOT.getBytes(StandardCharsets.UTF_8);
	private static final byte[] CR_UTF8 = CR.getBytes(StandardCharsets.UTF_8);
	private static final byte[] LF_UTF8 = LF.getBytes(StandardCharsets.UTF_8);
	private static final byte[] TAB_UTF8 = TAB.getBytes(StandardCharsets.UTF_8);
	private static final byte[] SPACE_SIMPLE = new byte[]{' '};
	private static final byte[] CR_SIMPLE = new byte[]{'\\', 'r'};
	private static final byte[] LF_SIMPLE = new byte[]{'\\', 'n'};
	private static final byte[] TAB_SIMPLE = new byte[]{'\\', 't'};

	private final ByteArrayOutputStream out;
	private final byte[] middleDot;
	private final byte[] cr;
	private final byte[] lf;
	private final byte[] tab;

	/**
	 * Creates the formatter.
	 * @param out output stream for the resulting diff. The diff would have \n line endings
	 * @param charset the charset that will be used when printing the results for the end user
	 */
	public WriteSpaceAwareDiffFormatter(ByteArrayOutputStream out, Charset charset) {
		this.out = out;
		CharsetEncoder charsetEncoder = charset.newEncoder();
		this.middleDot = replacementFor(charsetEncoder, MIDDLE_DOT, MIDDLE_DOT_UTF8, SPACE_SIMPLE);
		this.cr = replacementFor(charsetEncoder, CR, CR_UTF8, CR_SIMPLE);
		this.lf = replacementFor(charsetEncoder, LF, LF_UTF8, LF_SIMPLE);
		this.tab = replacementFor(charsetEncoder, TAB, TAB_UTF8, TAB_SIMPLE);
	}

	private static byte[] replacementFor(CharsetEncoder charsetEncoder, String value, byte[] fancy, byte[] simple) {
		return charsetEncoder.canEncode(value) ? fancy : simple;
	}

	/**
	 * Formats the diff.
	 * @param edits the list of edits to format
	 * @param a input text a, with \n line endings, with UTF-8 encoding
	 * @param b input text b, with \n line endings, with UTF-8 encoding
	 * @throws IOException if formatting fails
	 */
	public void format(EditList edits, RawText a, RawText b) throws IOException {
		IntList linesA = RawParseUtils.lineMap(a.getRawContent(), 0, a.getRawContent().length);
		IntList linesB = RawParseUtils.lineMap(b.getRawContent(), 0, b.getRawContent().length);
		boolean firstLine = true;
		for (int i = 0; i < edits.size(); i++) {
			Edit edit = edits.get(i);
			int lineA = Math.max(0, edit.getBeginA() - CONTEXT_LINES);
			int lineB = Math.max(0, edit.getBeginB() - CONTEXT_LINES);

			final int endIdx = findCombinedEnd(edits, i);
			final Edit endEdit = edits.get(endIdx);

			int endA = Math.min(a.size(), endEdit.getEndA() + CONTEXT_LINES);
			int endB = Math.min(b.size(), endEdit.getEndB() + CONTEXT_LINES);

			if (firstLine) {
				firstLine = false;
			} else {
				out.write('\n');
			}
			header(lineA, endA, lineB, endB);

			boolean showWhitespace = edit.getType() == Edit.Type.REPLACE;

			while (lineA < endA || lineB < endB) {
				if (lineA < edit.getBeginA()) {
					// Common part before the diff
					line(' ', a, lineA, linesA, false);
					lineA++;
					lineB++;
				} else if (lineA < edit.getEndA()) {
					line('-', a, lineA, linesA, showWhitespace);
					lineA++;
				} else if (lineB < edit.getEndB()) {
					line('+', b, lineB, linesB, showWhitespace);
					lineB++;
				} else {
					// Common part after the diff
					line(' ', a, lineA, linesA, false);
					lineA++;
					lineB++;
				}

				if (lineA == edit.getEndA() && lineB == edit.getEndB() && i < endIdx) {
					i++;
					edit = edits.get(i);
					showWhitespace = edit.getType() == Edit.Type.REPLACE;
				}
			}
		}
	}

	/**
	 * There might be multiple adjacent diffs, so we need to figure out the latest one in the group.
	 * @param edits list of edits
	 * @param i starting edit
	 * @return the index of the latest edit in the group
	 */
	private int findCombinedEnd(EditList edits, int i) {
		for (; i < edits.size() - 1; i++) {
			Edit current = edits.get(i);
			Edit next = edits.get(i + 1);
			if (current.getEndA() - next.getBeginA() > 2 * CONTEXT_LINES &&
					current.getEndB() - next.getBeginB() > 2 * CONTEXT_LINES) {
				break;
			}
		}
		return i;
	}

	private void header(int lineA, int endA, int lineB, int endB) {
		out.write('@');
		out.write('@');
		range('-', lineA + 1, endA - lineA);
		range('+', lineB + 1, endB - lineB);
		out.write(' ');
		out.write('@');
		out.write('@');
	}

	private void range(char prefix, int begin, int length) {
		out.write(' ');
		out.write(prefix);
		if (length == 0) {
			writeInt(begin - 1);
			out.write(',');
			out.write('0');
		} else {
			writeInt(begin);
			if (length > 1) {
				out.write(',');
				writeInt(length);
			}
		}
	}

	private void writeInt(int num) {
		String str = Integer.toString(num);
		for (int i = 0, len = str.length(); i < len; i++) {
			out.write(str.charAt(i));
		}
	}

	private void line(char prefix, RawText a, int lineA, IntList lines, boolean showWhitespace) throws IOException {
		out.write('\n');
		out.write(prefix);
		if (!showWhitespace) {
			a.writeLine(out, lineA);
			return;
		}
		byte[] bytes = a.getRawContent();
		for (int i = lines.get(lineA + 1), end = lines.get(lineA + 2); i < end; i++) {
			byte b = bytes[i];
			if (b == ' ') {
				out.write(middleDot);
			} else if (b == '\t') {
				out.write(tab);
			} else if (b == '\r') {
				out.write(cr);
			} else if (b == '\n') {
				out.write(lf);
			} else {
				out.write(b);
			}
		}
	}
}
