/*
 * Copyright 2026 DiffPlug
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
package com.diffplug.spotless.groovy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.StepHarness;

class RemoveSemicolonsStepTest {
	private final StepHarness harness = StepHarness.forStep(RemoveSemicolonsStep.create());

	private static String lines(String... lines) {
		return String.join("\n", lines);
	}

	@Test
	void removesTrailingSemicolons() {
		harness.testResource("groovy/removeSemicolons/GroovyCodeWithSemicolons.test",
				"groovy/removeSemicolons/GroovyCodeWithSemicolonsFormatted.test");
	}

	@Test
	void keepsSemicolonsBetweenStatementsAndBeforeTrailingComments() {
		harness.testUnaffected(lines(
				"def a = 1; def b = 2",
				"println(a); // trailing comment",
				"for (;;) { break }"));
	}

	/**
	 * <a href="https://github.com/diffplug/spotless/issues/2780">`removeSemicolons()` should not be applied to multiline strings in groovy</a>
	 */
	@Nested
	class Issue2780 {
		@Test
		void multilineStrings() {
			harness.testResource("groovy/removeSemicolons/Issue2780/MultilineString.test",
					"groovy/removeSemicolons/Issue2780/MultilineStringFormatted.test");
		}

		@Test
		void comments() {
			harness.testResource("groovy/removeSemicolons/Issue2780/Comments.test",
					"groovy/removeSemicolons/Issue2780/CommentsFormatted.test");
		}

		@Test
		void dollarSlashyString() {
			harness.test(
					lines(
							"def js = $/",
							"  function (doc, meta) {",
							"    emit(meta.id, null);",
							"  }",
							"/$;",
							"println(js);"),
					lines(
							"def js = $/",
							"  function (doc, meta) {",
							"    emit(meta.id, null);",
							"  }",
							"/$",
							"println(js)"));
		}

		@Test
		void dollarSlashyStringWithEscapes() {
			// `$/` is an escaped slash and `$$` an escaped dollar, neither terminates the string
			harness.test(
					lines(
							"def s = $/",
							"  a $/$ b;",
							"  $$ c;",
							"/$;"),
					lines(
							"def s = $/",
							"  a $/$ b;",
							"  $$ c;",
							"/$"));
		}

		@Test
		void escapedTripleDoubleQuotes() {
			harness.test(
					lines(
							"def s = \"\"\"",
							"  say \\\"\"\"hi;",
							"  emit(x);",
							"\"\"\";"),
					lines(
							"def s = \"\"\"",
							"  say \\\"\"\"hi;",
							"  emit(x);",
							"\"\"\""));
		}

		@Test
		void escapedTripleSingleQuotes() {
			harness.test(
					lines(
							"def s = '''",
							"  it\\'''s;",
							"  emit(x);",
							"''';"),
					lines(
							"def s = '''",
							"  it\\'''s;",
							"  emit(x);",
							"'''"));
		}

		@Test
		void multiLineForHeader() {
			harness.test(
					lines(
							"for (int i = 0;",
							"     i < 10;",
							"     i++) {",
							"  println(i);",
							"}"),
					lines(
							"for (int i = 0;",
							"     i < 10;",
							"     i++) {",
							"  println(i)",
							"}"));
		}

		@Test
		void closuresInsideParenthesesAreStillCleaned() {
			harness.test(
					lines(
							"example.andThen(val -> {",
							"  return val + 2;",
							"});",
							"list.each({ x ->",
							"  println(x);",
							"})"),
					lines(
							"example.andThen(val -> {",
							"  return val + 2",
							"})",
							"list.each({ x ->",
							"  println(x)",
							"})"));
		}

		@Test
		void multiLineForHeaderWithClosureBody() {
			harness.test(
					lines(
							"for (int i = 0;",
							"     i < 10;",
							"     i++) {",
							"  run({",
							"    println(i);",
							"  });",
							"}"),
					lines(
							"for (int i = 0;",
							"     i < 10;",
							"     i++) {",
							"  run({",
							"    println(i)",
							"  })",
							"}"));
		}

		@Test
		void onlyTheForKeywordProtectsParentheses() {
			harness.test(
					lines(
							"platfor(a;",
							"  b);"),
					lines(
							"platfor(a",
							"  b)"));
		}

		@Test
		void bracketsInsideStringsAndCommentsAreIgnored() {
			harness.test(
					lines(
							"def a = \"(\";",
							"def b = ')';",
							"// (",
							"/* ) */",
							"println(a + b);"),
					lines(
							"def a = \"(\"",
							"def b = ')'",
							"// (",
							"/* ) */",
							"println(a + b)"));
		}
	}
}
