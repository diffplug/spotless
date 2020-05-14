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
import java.nio.charset.CharsetEncoder;
import java.util.Locale;

import javax.annotation.Nullable;

class EncodingErrorMsg {
	static @Nullable String msg(String chars, byte[] bytes, Charset encoding) {
		CharsetEncoder encoder = encoding.newEncoder();
		CharBuffer charBuf = CharBuffer.allocate(2);
		ByteBuffer byteBuf = ByteBuffer.allocate(4);
		int cIdx = 0;
		int bIdx = 0;
		int line = 1;
		int col = 0;
		while (cIdx < chars.length()) {
			charBuf.rewind();
			byteBuf.rewind();
			++col;
			char c = chars.charAt(cIdx++);
			charBuf.put(c);
			if (c == '\n') {
				++line;
				col = 0;
			} else if (!Character.isBmpCodePoint(c)) {
				// since we just decoded String, we can assume that all of its UTF-16 are correctly paired
				charBuf.put(chars.charAt(cIdx++));
			}
			charBuf.limit(charBuf.position());
			charBuf.position(0);
			boolean endOfInput = true;
			encoder.encode(charBuf, byteBuf, endOfInput);
			for (int i = 0; i < byteBuf.position(); ++i) {
				if (byteBuf.get(i) != bytes[bIdx + i]) {
					byteBuf.limit(byteBuf.position());
					byteBuf.position(0);
					charBuf.position(0);
					return "Line " + line + " col " + col + ": the codepoint '" + charBuf.toString() + "' (U+" + Integer.toHexString(charBuf.toString().codePointAt(0)).toUpperCase(Locale.ROOT) +
							") encoded via " + encoding +
							" to 0x" + hex(byteBuf) + " but was decoded from 0x" + hex(ByteBuffer.wrap(bytes, bIdx, 1));
				}
			}
			bIdx += byteBuf.position();
		}
		return null;
	}

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	private static String hex(ByteBuffer bytes) {
		char[] hexChars = new char[bytes.remaining() * 2];
		for (int j = 0; j < bytes.limit(); j++) {
			int v = bytes.get() & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}
}
