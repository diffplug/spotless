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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class EncodingErrorMsgTest {
	@Test
	public void cp1252asUtf8() throws UnsupportedEncodingException {
		// empty case
		cp1252asUtf8("", null);
		// single char
		cp1252asUtf8("a", null);
		cp1252asUtf8("\u00B0",
				"Line 1 col 1: the codepoint '�' (U+FFFD) decoded via UTF-8 from 0xB0 should have been 0xEFBFBD");
		// multiline
		cp1252asUtf8("\n123\nabc\n", null);
		cp1252asUtf8("\n123\nabc\u00B0\nABC",
				"Line 3 col 4: the codepoint '�' (U+FFFD) decoded via UTF-8 from 0xB0 should have been 0xEFBFBD");
	}

	private void cp1252asUtf8(String test, @Nullable String expectedMessage) throws UnsupportedEncodingException {
		byte[] cp1252 = test.getBytes("cp1252");
		String asUTF = new String(cp1252, StandardCharsets.UTF_8);
		String actualMessage = EncodingErrorMsg.msg(asUTF, cp1252, StandardCharsets.UTF_8);
		Assertions.assertThat(actualMessage).isEqualTo(expectedMessage);
	}

	@Test
	public void utf8asCP1252() throws UnsupportedEncodingException {
		// unfortunately, if you treat UTF8 as Cp1252, it looks weird, but it usually roundtrips faithfully
		// which makes it hard to detect

		// empty case
		utf8asCP1252("", null);
		// single char
		utf8asCP1252("a", null);
		utf8asCP1252("\u00B0", null);
		// multibyte UTF-8 can hide too
		utf8asCP1252("\u1F602", null);
		// but some will trigger problems we can detect
		utf8asCP1252("\u237B", "Line 1 col 2: unmappable character for windows-1252"); // there are some codepoints where it doesn't
		// multiline
		utf8asCP1252("\n123\nabc\n", null);
		utf8asCP1252("\n123\nabc\u00B0\nABC", null);
		utf8asCP1252("\n123\nabc\u1F602\nABC", null);
		utf8asCP1252("\n123\nabc\u237B\nABC", "Line 3 col 5: unmappable character for windows-1252");
	}

	private void utf8asCP1252(String test, @Nullable String expectedMessage) throws UnsupportedEncodingException {
		byte[] utf8 = test.getBytes(StandardCharsets.UTF_8);
		String asCp1252 = new String(utf8, "cp1252");
		String actualMessage = EncodingErrorMsg.msg(asCp1252, utf8, Charset.forName("cp1252"));
		Assertions.assertThat(actualMessage).isEqualTo(expectedMessage);
	}
}
