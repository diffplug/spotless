/*
 * Copyright 2016-2025 DiffPlug
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

class EncodingErrorMsgTest {
	@Test
	void cp1252asUtf8() throws UnsupportedEncodingException {
		// empty case
		cp1252asUtf8("", null);
		// single char
		cp1252asUtf8("a", null);
		cp1252asUtf8("°", """
				Encoding error! Spotless uses UTF-8 by default.  At line 1 col 1:
				� <- UTF-8
				° <- windows-1252
				° <- ISO-8859-1
				ｰ <- Shift_JIS""");
		// multiline
		cp1252asUtf8("\n123\nabc\n", null);
		cp1252asUtf8("\n123\nabc°\nABC", """
				Encoding error! Spotless uses UTF-8 by default.  At line 3 col 4:
				abc�␤AB <- UTF-8
				abc°␤AB <- windows-1252
				abc°␤AB <- ISO-8859-1
				abcｰ␤AB <- Shift_JIS""");
	}

	private void cp1252asUtf8(String test, @Nullable String expectedMessage) throws UnsupportedEncodingException {
		byte[] cp1252 = test.getBytes("cp1252");
		String asUTF = new String(cp1252, UTF_8);
		String actualMessage = EncodingErrorMsg.msg(asUTF, cp1252, UTF_8);
		assertThat(actualMessage).isEqualTo(expectedMessage);
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
		utf8asCP1252("⍻", """
				Encoding error! You configured Spotless to use windows-1252.  At line 1 col 2:
				â�» <- windows-1252
				⍻ <- UTF-8
				â» <- ISO-8859-1
				竝ｻ <- Shift_JIS"""); // there are some codepoints where it doesn't
		// multiline
		utf8asCP1252("\n123\nabc\n", null);
		utf8asCP1252("\n123\nabc°\nABC", null);
		utf8asCP1252("\n123\nabc😂\nABC", null);
		utf8asCP1252("\n123\nabc⍻\nABC", """
				Encoding error! You configured Spotless to use windows-1252.  At line 3 col 5:
				bcâ�»␤A <- windows-1252
				bc⍻␤ABC <- UTF-8
				bcâ»␤A <- ISO-8859-1
				bc竝ｻ␤AB <- Shift_JIS""");
	}

	private void utf8asCP1252(String test, @Nullable String expectedMessage) throws UnsupportedEncodingException {
		byte[] utf8 = test.getBytes(UTF_8);
		String asCp1252 = new String(utf8, "cp1252");
		String actualMessage = EncodingErrorMsg.msg(asCp1252, utf8, Charset.forName("cp1252"));
		assertThat(actualMessage).isEqualTo(expectedMessage);
	}

	@Test
	void canUseUnrepresentableOnPurpose() throws UnsupportedEncodingException {
		String pathologic = new String(new char[]{EncodingErrorMsg.UNREPRESENTABLE});
		byte[] pathologicBytes = pathologic.getBytes(UTF_8);
		String pathologicMsg = EncodingErrorMsg.msg(pathologic, pathologicBytes, UTF_8);
		assertThat(pathologicMsg).isNull();
	}
}
