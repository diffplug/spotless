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
package com.diffplug.spotless;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import javax.annotation.Nullable;

class EncodingErrorMsg {
	static final char UNREPRESENTABLE = '�';
	private static int CONTEXT = 3;

	static @Nullable String msg(String chars, byte[] bytes, Charset charset) {
		int unrepresentable = chars.indexOf(UNREPRESENTABLE);
		if (unrepresentable == -1) {
			return null;
		}

		// sometimes the '�' is really in a file, such as for *this* file
		// so we have to handle that pathologic case
		ByteBuffer byteBuf = ByteBuffer.wrap(bytes);
		CharBuffer charBuf = CharBuffer.allocate(chars.length());
		CoderResult result = charset.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT)
				.decode(byteBuf, charBuf, true);
		if (!result.isError()) {
			return null;
		}

		// make a new, smaller charBuf better suited to our request
		charBuf = CharBuffer.allocate(Math.min(unrepresentable + 2 * CONTEXT, chars.length()));

		StringBuilder message = new StringBuilder("Encoding error! ");
		if (charset.equals(StandardCharsets.UTF_8)) {
			message.append("Spotless uses UTF-8 by default.");
		} else {
			message.append("You configured Spotless to use " + charset.name() + ".");
		}

		int line = 1;
		int col = 1;
		for (int i = 0; i < unrepresentable; ++i) {
			char c = chars.charAt(i);
			if (c == '\n') {
				++line;
				col = 1;
			} else if (c != '\r') {
				++col;
			}
		}
		message.append("  At line " + line + " col " + col + ":");

		// https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html
		LinkedHashSet<Charset> encodings = new LinkedHashSet<>();
		encodings.add(charset); // the encoding we are using
		encodings.add(StandardCharsets.UTF_8);  // followed by likely encodings
		addIfAvailable(encodings, "windows-1252");
		encodings.add(StandardCharsets.ISO_8859_1);
		addIfAvailable(encodings, "Shift_JIS");
		addIfAvailable(encodings, "Big5");
		addIfAvailable(encodings, "Big5-HKSCS");
		addIfAvailable(encodings, "GBK");
		addIfAvailable(encodings, "GB2312");
		addIfAvailable(encodings, "GB18030");
		Iterator<Charset> iterator = encodings.iterator();

		appendExample(message, iterator.next(), byteBuf, charBuf, unrepresentable, true);
		while (iterator.hasNext()) {
			appendExample(message, iterator.next(), byteBuf, charBuf, unrepresentable, false);
		}
		return message.toString();
	}

	private static void addIfAvailable(Collection<Charset> charsets, String name) {
		try {
			charsets.add(Charset.forName(name));
		} catch (UnsupportedCharsetException e) {
			// no worries
		}
	}

	private static void appendExample(StringBuilder message, Charset charset, ByteBuffer byteBuf, CharBuffer charBuf, int startPoint, boolean must) {
		byteBuf.clear();
		charBuf.clear();

		CharsetDecoder decoder = charset.newDecoder();
		if (!must) {
			// bail early if we can
			CoderResult r = decoder
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(byteBuf, charBuf, true);
			if (r.isError()) {
				return;
			}
		} else {
			decoder
					.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE)
					.decode(byteBuf, charBuf, true);
		}
		charBuf.flip();

		int start = Math.max(startPoint - CONTEXT, 0);
		int end = Math.min(charBuf.limit(), startPoint + CONTEXT + 1);
		message.append('\n');
		message.append(charBuf.subSequence(start, end).toString()
				.replace('\n', '␤')
				.replace('\r', '␍')
				.replace('\t', '⇥'));
		message.append(" <- ");
		message.append(charset.name());
	}
}
