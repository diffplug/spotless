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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class EncodingErrorMsgTest {
	@Test
	void cp1252asUtf8() throws UnsupportedEncodingException {
		// empty case
		cp1252asUtf8("", null);
		// single char
		cp1252asUtf8("a", null);
		cp1252asUtf8("°", "Encoding error! Spotless uses UTF-8 by default.  At line 1 col 1:\n" +
				"� <- UTF-8\n" +
				"° <- windows-1252\n" +
				"° <- ISO-8859-1\n" +
				"ｰ <- Shift_JIS");
		// multiline
		cp1252asUtf8("\n123\nabc\n", null);
		cp1252asUtf8("\n123\nabc°\nABC", "Encoding error! Spotless uses UTF-8 by default.  At line 3 col 4:\n" +
				"abc�␤AB <- UTF-8\n" +
				"abc°␤AB <- windows-1252\n" +
				"abc°␤AB <- ISO-8859-1\n" +
				"abcｰ␤AB <- Shift_JIS");
	}

	private void cp1252asUtf8(String test, @Nullable String expectedMessage) throws UnsupportedEncodingException {
		var cp1252 = test.getBytes("cp1252");
		var asUTF = new String(cp1252, StandardCharsets.UTF_8);
		String actualMessage = EncodingErrorMsg.msg(asUTF, cp1252, StandardCharsets.UTF_8);
		Assertions.assertThat(actualMessage).isEqualTo(expectedMessage);
	}

	@Test
	void utf8asCP1252() throws UnsupportedEncodingException {
		// unfortunately, if you treat UTF8 as Cp1252, it looks weird, but it usually roundtrips faithfully
		// which makes it hard to detect

		// empty case
		utf8asCP1252("", null);
		// single char
		utf8asCP1252("a", null);
		utf8asCP1252("°", null);
		// multibyte UTF-8 can hide too
		utf8asCP1252("😂", null);
		// but some will trigger problems we can detect
		utf8asCP1252("⍻", "Encoding error! You configured Spotless to use windows-1252.  At line 1 col 2:\n" +
				"â�» <- windows-1252\n" +
				"⍻ <- UTF-8\n" +
				"â» <- ISO-8859-1\n" +
				"竝ｻ <- Shift_JIS"); // there are some codepoints where it doesn't
		// multiline
		utf8asCP1252("\n123\nabc\n", null);
		utf8asCP1252("\n123\nabc°\nABC", null);
		utf8asCP1252("\n123\nabc😂\nABC", null);
		utf8asCP1252("\n123\nabc⍻\nABC", "Encoding error! You configured Spotless to use windows-1252.  At line 3 col 5:\n" +
				"bcâ�»␤A <- windows-1252\n" +
				"bc⍻␤ABC <- UTF-8\n" +
				"bcâ»␤A <- ISO-8859-1\n" +
				"bc竝ｻ␤AB <- Shift_JIS");
	}

	private void utf8asCP1252(String test, @Nullable String expectedMessage) throws UnsupportedEncodingException {
		var utf8 = test.getBytes(StandardCharsets.UTF_8);
		var asCp1252 = new String(utf8, "cp1252");
		String actualMessage = EncodingErrorMsg.msg(asCp1252, utf8, Charset.forName("cp1252"));
		Assertions.assertThat(actualMessage).isEqualTo(expectedMessage);
	}

	@Test
	void canUseUnrepresentableOnPurpose() throws UnsupportedEncodingException {
		var pathologic = new String(new char[]{EncodingErrorMsg.UNREPRESENTABLE});
		var pathologicBytes = pathologic.getBytes(StandardCharsets.UTF_8);
		String pathologicMsg = EncodingErrorMsg.msg(pathologic, pathologicBytes, StandardCharsets.UTF_8);
		Assertions.assertThat(pathologicMsg).isNull();
	}
}
