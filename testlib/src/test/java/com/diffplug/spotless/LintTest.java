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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LintTest {
	@Test
	public void examples() {
		roundtrip(Lint.create("code", "msg", 5));
		roundtrip(Lint.create("code", "msg", 5, 7));
		roundtrip(Lint.create("(code)", "msg\nwith\nnewlines", 5, 7));
	}

	private void roundtrip(Lint lint) {
		Lint roundTripped = Lint.fromOneLine(lint.asOneLine());
		Assertions.assertEquals(lint.asOneLine(), roundTripped.asOneLine());
	}

	@Test
	public void perCharacterEscaper() {
		roundtrip("abcn123", "abcn123");
		roundtrip("abc\\123", "abc\\\\123");
		roundtrip("abc(123)", "abc\\₍123\\₎");
		roundtrip("abc\n123", "abc\\n123");
		roundtrip("abc\nn123", "abc\\nn123");
	}

	private void roundtrip(String unescaped, String escaped) {
		Assertions.assertEquals(escaped, Lint.safeParensAndNewlines.escape(unescaped));
		Assertions.assertEquals(unescaped, Lint.safeParensAndNewlines.unescape(escaped));
	}
}
