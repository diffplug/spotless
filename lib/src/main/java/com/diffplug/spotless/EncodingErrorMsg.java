/*
 * Copyright 2016-2023 DiffPlug
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
		var unrepresentable = chars.indexOf(UNREPRESENTABLE);
		if (unrepresentable == -1) {
			return null;
		}

		// sometimes the '�' is really in a file, such as for *this* file
		// so we have to handle that corner case
		var byteBuf = ByteBuffer.wrap(bytes);
		var charBuf = CharBuffer.allocate(chars.length());
		var result = charset.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT)
				.decode(byteBuf, charBuf, true);
		if (!result.isError()) {
			return null;
		} else {
			// there really is an encoding error, so we'll send a message
			return new EncodingErrorMsg(chars, byteBuf, charset, unrepresentable).message.toString();
		}
	}

	private final ByteBuffer byteBuf;
	private final CharBuffer charBuf;
	private final int unrepresentable;
	private final StringBuilder message;

	private EncodingErrorMsg(String chars, ByteBuffer byteBuf, Charset charset, int unrepresentable) {
		this.byteBuf = byteBuf;
		this.unrepresentable = unrepresentable;
		// make a new, smaller charBuf better suited to our request
		charBuf = CharBuffer.allocate(Math.min(unrepresentable + 2 * CONTEXT, chars.length()));

		message = new StringBuilder("Encoding error! ");
		if (charset.equals(StandardCharsets.UTF_8)) {
			message.append("Spotless uses UTF-8 by default.");
		} else {
			message.append("You configured Spotless to use " + charset.name() + ".");
		}

		var line = 1;
		var col = 1;
		for (var i = 0; i < unrepresentable; ++i) {
			var c = chars.charAt(i);
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
		appendExample(iterator.next(), true);
		while (iterator.hasNext()) {
			appendExample(iterator.next(), false);
		}
	}

	private static void addIfAvailable(Collection<Charset> charsets, String name) {
		try {
			charsets.add(Charset.forName(name));
		} catch (UnsupportedCharsetException e) {
			// no worries
		}
	}

	private void appendExample(Charset charset, boolean must) {
		byteBuf.clear();
		charBuf.clear();

		var decoder = charset.newDecoder();
		if (!must) {
			// bail early if we can
			var r = decoder
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

		var start = Math.max(unrepresentable - CONTEXT, 0);
		var end = Math.min(charBuf.limit(), unrepresentable + CONTEXT + 1);
		message.append('\n');
		message.append(charBuf.subSequence(start, end).toString()
				.replace('\n', '␤')
				.replace('\r', '␍')
				.replace('\t', '⇥'));
		message.append(" <- ");
		message.append(charset.name());
	}
}
