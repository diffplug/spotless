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
import java.nio.charset.CoderResult;
import java.util.Locale;

import javax.annotation.Nullable;

class EncodingErrorMsg {
	private static final char UNREPRESENTABLE = '�';

	static @Nullable String msg(String chars, byte[] bytes, Charset encoding) {
		int unrepresentable = chars.indexOf(UNREPRESENTABLE);
		if (unrepresentable == -1) {
			return null;
		}

		CharsetEncoder encoder = encoding.newEncoder();
		CharBuffer charBuf = CharBuffer.allocate(2);
		ByteBuffer byteBuf = ByteBuffer.allocate(4);
		int cIdx = 0;
		int bIdx = 0;
		int line = 1;
		int col = 0;
		while (cIdx < chars.length()) {
			charBuf.clear();
			byteBuf.clear();
			++col;
			char c = chars.charAt(cIdx++);
			charBuf.put(c);
			int codePoint;
			if (Character.isBmpCodePoint(c)) {
				codePoint = c;
				if (c == '\n') {
					++line;
					col = 0;
				} else if (c < ' ' && c != '\t' && c != '\r') {
					return lineColCodepointMsg(line, col, codePoint, encoding, ByteBuffer.wrap(bytes, bIdx, 1), "is a control character");
				}
			} else {
				// since we just decoded String, we can assume that all of its UTF-16 are correctly paired
				char c2 = chars.charAt(cIdx++);
				codePoint = Character.toCodePoint(c, c2);
				charBuf.put(c2);
			}
			charBuf.flip();
			boolean endOfInput = true;
			CoderResult result = encoder.encode(charBuf, byteBuf, endOfInput);
			if (result.isUnmappable()) {
				return lineColMsg(line, col, "unmappable character for " + encoding);
			} else if (result.isMalformed()) {
				return lineColMsg(line, col, "malformed character for " + encoding);
			}
			for (int i = 0; i < byteBuf.position(); ++i) {
				if (byteBuf.get(i) != bytes[bIdx + i]) {
					byteBuf.flip();
					return lineColCodepointMsg(line, col, codePoint, encoding, ByteBuffer.wrap(bytes, bIdx, i + 1),
							"should have been 0x" + hex(byteBuf));
				}
			}
			bIdx += byteBuf.position();
		}
		throw new IllegalArgumentException();
	}

	private static String lineColMsg(int line, int col, String msg) {
		return "Line " + line + " col " + col + ": " + msg;
	}

	private static String lineColCodepointMsg(int line, int col, int codepoint, Charset charset, ByteBuffer decodedFrom, String msg) {
		return lineColMsg(line, col, "the codepoint '" + new String(Character.toChars(codepoint)) + "' (U+" + Integer.toHexString(codepoint).toUpperCase(Locale.ROOT) + ") " +
				"decoded via " + charset + " from 0x" + hex(decodedFrom) + " " +
				msg);
	}

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	private static String hex(ByteBuffer bytes) {
		int len = bytes.limit() - bytes.position();
		char[] hexChars = new char[2 * len];
		for (int j = 0; j < len; j++) {
			int v = bytes.get() & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}
}
