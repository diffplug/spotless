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
package com.diffplug.spotless.java;

import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;

class ForbidWildcardImportsStepTest {
	@Test
	void detectsPlainAndStaticWildcards() {
		FormatterStep step = ForbidWildcardImportsStep.create();
		StepHarness.forStep(step)
				.expectLintsOf(StringPrinter.buildStringFromLines(
						"import java.util.*;",
						"import static java.util.Collections.*;",
						"import java.util.List;"))
				.toBe("L1 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this",
						"L2 forbidWildcardImports(import static java.util.Collections.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
	}

	@Test
	void detectsIndentedWildcardImport() {
		FormatterStep step = ForbidWildcardImportsStep.create();
		StepHarness.forStep(step)
				.expectLintsOf(StringPrinter.buildStringFromLines(
						"  import java.util.*;",
						"\timport static java.util.Collections.*;"))
				.toBe("L1 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this",
						"L2 forbidWildcardImports(import static java.util.Collections.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
	}

	@Test
	void ignoresTextBlocks() {
		String source = StringPrinter.buildStringFromLines(
				"import java.util.List;",
				"class Example {",
				"    String source = \"\"\"",
				"        import java.util.*;",
				"        import static java.util.Collections.*;",
				"        \"\"\";",
				"}");
		StepHarness.forStep(ForbidWildcardImportsStep.create())
				.testUnaffected(source)
				.expectLintsOf(source).toBe("(none)");
	}

	@Test
	void ignoresEscapedQuotesAndCommentMarkersInsideTextBlocks() {
		String source = StringPrinter.buildStringFromLines(
				"class Example {",
				"    String source = \"\"\"",
				"        \" and \"\" and \\\"\"\" do not close the text block",
				"        import java.util.*;",
				"        /* // ' are text, not comments or character literals",
				"        import static java.util.Collections.*;",
				"        \\\\",
				"        \"\"\";",
				"    String other = \"\"\"",
				"        import java.io.*;",
				"        \"\"\";",
				"}");
		StepHarness.forStep(ForbidWildcardImportsStep.create())
				.testUnaffected(source)
				.expectLintsOf(source).toBe("(none)");
	}

	@Test
	void ignoresComments() {
		String source = StringPrinter.buildStringFromLines(
				"// \"\"\" is not a text block",
				"/* \"\"\" ' //",
				"import java.util.*;",
				"import static java.util.Collections.*;",
				"*/",
				"/**",
				"import java.io.*;",
				"*/",
				"// import java.net.*;",
				"import java.util.List;");
		StepHarness.forStep(ForbidWildcardImportsStep.create())
				.testUnaffected(source)
				.expectLintsOf(source).toBe("(none)");
	}

	@Test
	void preservesRealImportDiagnosticsAfterComments() {
		String source = StringPrinter.buildStringFromLines(
				"/* \"\"\" " + "\ud83d\ude00".repeat(80),
				"import ignored.*;",
				"*/",
				"  import java.util.*; // keep the original diagnostic",
				"// \"\"\"",
				"\timport static java.util.Collections.*;");
		StepHarness.forStep(ForbidWildcardImportsStep.create())
				.testUnaffected(source)
				.expectLintsOf(source)
				.toBe("L4 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this",
						"L6 forbidWildcardImports(import static java.util.Collections.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
	}

	@Test
	void quotesInOrdinaryLiteralsDoNotStartTextBlocksOrComments() {
		String source = StringPrinter.buildStringFromLines(
				"@Example(text = \"\\\"\\\"\\\"\", quote = '\"', apostrophe = '\\'', backslash = '\\\\', block = \"/*\", line = \"//\", escaped = \"\\\\\")",
				"package example;",
				"import java.util.*;");
		StepHarness.forStep(ForbidWildcardImportsStep.create())
				.testUnaffected(source)
				.expectLintsOf(source)
				.toBe("L3 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
	}

	@Test
	void detectsRealImportAfterTextBlockWithEscapedBackslash() {
		String source = StringPrinter.buildStringFromLines(
				"@Example(\"\"\"",
				"    import ignored.*;",
				"    " + "\\\\" + "\"\"\")",
				"package example;",
				"import java.util.*;");
		StepHarness.forStep(ForbidWildcardImportsStep.create())
				.testUnaffected(source)
				.expectLintsOf(source)
				.toBe("L5 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
	}

	@Test
	void detectsImportWithoutFinalNewline() {
		String source = "import java.util.*;";
		StepHarness.forStep(ForbidWildcardImportsStep.create())
				.testUnaffected(source)
				.expectLintsOf(source)
				.toBe("L1 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
	}

	@Test
	void ignoresUnterminatedCommentsAndTextBlocks() {
		StepHarness harness = StepHarness.forStep(ForbidWildcardImportsStep.create());
		for (String opening : new String[]{"/*", "String source = \"\"\""}) {
			String source = opening + "\nimport java.util.*;\n";
			harness.testUnaffected(source).expectLintsOf(source).toBe("(none)");
		}
	}

	@Test
	void ignoresLargeTextBlock() {
		String source = "class Example { String source = \"\"\"\n" + "text ".repeat(20_000)
				+ "\nimport java.util.*;\n\"\"\"; }\n";
		StepHarness.forStep(ForbidWildcardImportsStep.create())
				.testUnaffected(source)
				.expectLintsOf(source).toBe("(none)");
	}

	@Test
	void unicodeEscapesInStringsDoNotHideRealImports() {
		StepHarness harness = StepHarness.forStep(ForbidWildcardImportsStep.create());
		for (String literal : new String[]{
				"\"\\u005c\"\"",
				"\"\\uuuu005c\"\"",
				"\"\\\\u0022\"",
				"\"\\\\\\u006e\"",
				"\"\\u005c\\u005c\"",
				"\"\\u005c\\u0022\""}) {
			String source = StringPrinter.buildStringFromLines(
					"@Example(" + literal + ")",
					"package example;",
					"import java.util.*;");
			harness.testUnaffected(source).expectLintsOf(source)
					.toBe("L3 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
		}
	}

	@Test
	void unicodeEscapesCanDelimitComments() {
		String source = StringPrinter.buildStringFromLines(
				"\\u002f\\u002a",
				"import ignored.*;",
				"\\u002a/",
				"import java.util.*;");
		StepHarness.forStep(ForbidWildcardImportsStep.create())
				.testUnaffected(source)
				.expectLintsOf(source)
				.toBe("L4 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
	}

	@Test
	void unicodeEscapesCanDelimitTextBlocks() {
		String delimiter = "\\u0022".repeat(3);
		String source = StringPrinter.buildStringFromLines(
				"@Example(" + delimiter,
				"    import ignored.*;",
				delimiter + ")",
				"package example;",
				"import java.util.*;");
		StepHarness.forStep(ForbidWildcardImportsStep.create())
				.testUnaffected(source)
				.expectLintsOf(source)
				.toBe("L5 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
	}

	@Test
	void unicodeLineTerminatorsEndLineComments() {
		StepHarness harness = StepHarness.forStep(ForbidWildcardImportsStep.create());
		for (String newline : new String[]{"\\u000a", "\\u000d", "\\u000d\\u000a"}) {
			String source = StringPrinter.buildStringFromLines(
					"// " + newline + "@Example(\"\"\"",
					"    import ignored.*;",
					"\"\"\")",
					"package example;",
					"import java.util.*;");
			harness.testUnaffected(source).expectLintsOf(source)
					.toBe("L5 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
		}
	}

	@Test
	void unicodeTranslationIsNotRecursive() {
		String source = StringPrinter.buildStringFromLines(
				"// \\u005cu000a \"\"\"",
				"import java.util.*;");
		StepHarness.forStep(ForbidWildcardImportsStep.create())
				.testUnaffected(source)
				.expectLintsOf(source)
				.toBe("L2 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
	}

	@Test
	void unicodeTranslationPreservesRawDiagnostics() {
		String source = StringPrinter.buildStringFromLines(
				"// \\u000a // another comment",
				"import java.\\u0075til.*;");
		StepHarness.forStep(ForbidWildcardImportsStep.create())
				.testUnaffected(source)
				.expectLintsOf(source)
				.toBe("L2 forbidWildcardImports(import java.\\u0075til.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
	}

	@Test
	void malformedUnicodeEscapesDoNotThrow() {
		StepHarness harness = StepHarness.forStep(ForbidWildcardImportsStep.create());
		for (String escape : new String[]{"\\u00g0", "\\uu00", "\\u００２２"}) {
			String source = "// " + escape + "\nimport java.util.*;\n";
			harness.testUnaffected(source).expectLintsOf(source)
					.toBe("L2 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
		}
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			@Override
			protected void setupTest(API api) {
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return ForbidWildcardImportsStep.create();
			}
		}.testEquals();
	}
}
