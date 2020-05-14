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
import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class EncodingErrorMsgTest {
	@Test
	public void cp1252asUtf8() throws UnsupportedEncodingException {
		// dummy case
		cp1252asUtf8("", null);
		cp1252asUtf8("a", null);
		cp1252asUtf8("\u00B0",
				"Line 1 col 1: the codepoint '�' (U+FFFD) encoded via UTF-8 to 0xEFBFBD but was decoded from 0xB0");
		cp1252asUtf8("\n123\nabc\n", null);
		//		cp1252asUtf8("\n123\nabc" + "\u00B0" + "\nABC",
		//				"Line 3 col 4: the codepoint '�' (U+FFFD) encoded via UTF-8 to 0xEFBFBD but was decoded from 0xB0");
	}

	private void cp1252asUtf8(String test, @Nullable String expectedMessage) throws UnsupportedEncodingException {
		byte[] cp1252 = test.getBytes("cp1252");
		String asUTF = new String(cp1252, StandardCharsets.UTF_8);
		String actualMessage = EncodingErrorMsg.msg(asUTF, cp1252, StandardCharsets.UTF_8);
		Assertions.assertThat(actualMessage).isEqualTo(expectedMessage);
	}
}
