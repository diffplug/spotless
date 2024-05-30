/*
 * Copyright 2016-2024 DiffPlug
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

public class PerCharacterEscaperTest {
	@Test
	public void examples() {
		roundtrip("abcn123", "abcn123");
		roundtrip("abc/123", "abc//123");
		roundtrip("abc(123)", "abc/₍123/₎");
		roundtrip("abc\n123", "abc/n123");
		roundtrip("abc\nn123", "abc/nn123");
	}

	private void roundtrip(String unescaped, String escaped) {
		Assertions.assertEquals(escaped, Lint.safeParensAndNewlines.escape(unescaped));
		Assertions.assertEquals(unescaped, Lint.safeParensAndNewlines.unescape(escaped));
	}

	@Test
	public void performanceOptimizationSpecific() {
		PerCharacterEscaper escaper = PerCharacterEscaper.specifiedEscape("`a1b2c3d");
		// if nothing gets changed, it should return the exact same value
		String abc = "abc";
		Assertions.assertSame(abc, escaper.escape(abc));
		Assertions.assertSame(abc, escaper.unescape(abc));

		// otherwise it should have the normal behavior
		Assertions.assertEquals("`b", escaper.escape("1"));
		Assertions.assertEquals("`a", escaper.escape("`"));
		Assertions.assertEquals("abc`b`c`d`adef", escaper.escape("abc123`def"));

		// in both directions
		Assertions.assertEquals("1", escaper.unescape("`b"));
		Assertions.assertEquals("`", escaper.unescape("`a"));
		Assertions.assertEquals("abc123`def", escaper.unescape("abc`1`2`3``def"));
	}

	@Test
	public void cornerCasesSpecific() {
		PerCharacterEscaper escaper = PerCharacterEscaper.specifiedEscape("`a1b2c3d");
		// cornercase - escape character without follow-on will throw an error
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> escaper.unescape("`"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Escape character '`' can't be the last character in a string.");
		// escape character followed by non-escape character is fine
		Assertions.assertEquals("e", escaper.unescape("`e"));
	}
}
