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
package com.diffplug.spotless.toml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.LintState;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;

class VersionCatalogStepTest {
	@Test
	void behavior() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.testResource("toml/versionCatalogDirty.toml", "toml/versionCatalogClean.toml");
	}

	@Test
	void idempotent() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.testResourceUnaffected("toml/versionCatalogClean.toml");
	}

	@Test
	void spacingAroundEquals() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"[versions]\nfoo=\"1.0\"\n",
				"[versions]\nfoo = \"1.0\"\n");
	}

	@Test
	void inlineTableSpacing() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"[libraries]\nfoo = {module=\"org:foo\",version.ref=\"bar\"}\n",
				"[libraries]\nfoo = { module = \"org:foo\", version.ref = \"bar\" }\n");
	}

	@Test
	void inlineArraySpacing() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"[bundles]\nfoo = [\"a\",\"b\"]\n",
				"[bundles]\nfoo = [ \"a\", \"b\" ]\n");
	}

	@Test
	void sortEntries() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"[versions]\nzoo = \"1.0\"\nalpha = \"2.0\"\n",
				"[versions]\nalpha = \"2.0\"\nzoo = \"1.0\"\n");
	}

	@Test
	void sortTables() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"[plugins]\na = \"1\"\n\n[versions]\nb = \"2\"\n",
				"[versions]\nb = \"2\"\n\n[plugins]\na = \"1\"\n");
	}

	@Test
	void commentsPreserved() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"[versions]\n# Z library\nzoo = \"1.0\"\n# A library\nalpha = \"2.0\"\n",
				"[versions]\n# A library\nalpha = \"2.0\"\n# Z library\nzoo = \"1.0\"\n");
	}

	@Test
	void inlineCommentsPreserved() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"[versions]\nfoo =\"1.0\" # latest stable\n",
				"[versions]\nfoo = \"1.0\" # latest stable\n");
	}

	@Test
	void bracketsInInlineCommentsDoNotConsumeFollowingEntries() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		for (String comment : new String[]{"[planned update", "]", "{", "}", "[\"unfinished quote"}) {
			harness.test(
					"[versions]\nzoo = \"1.0\" # " + comment + "\nalpha = \"2.0\"\n",
					"[versions]\nalpha = \"2.0\"\nzoo = \"1.0\" # " + comment + "\n");
		}
	}

	@Test
	void hashesAndBracketsInsideStringsAreNotComments() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		for (String value : new String[]{"\"1.0#[\"", "'1.0#['", "\"1.0\\\"#[\""}) {
			harness.testUnaffected("[versions]\nfoo = " + value + "\n");
		}
	}

	@Test
	void escapedBackslashBeforeClosingQuoteDoesNotHideComment() throws Exception {
		StepHarness.forStep(VersionCatalogStep.create()).test(
				"[versions]\nzoo = \"1.0\\\\\" # [planned update\nalpha = \"2.0\"\n",
				"[versions]\nalpha = \"2.0\"\nzoo = \"1.0\\\\\" # [planned update\n");
	}

	@Test
	void literalStringCommasInArraysArePreserved() throws Exception {
		StepHarness.forStep(VersionCatalogStep.create()).test(
				"[bundles]\nfoo=['a,b','c']\n",
				"[bundles]\nfoo = [ 'a,b', 'c' ]\n");
	}

	@Test
	void literalStringCommasInInlineTablesArePreserved() throws Exception {
		StepHarness.forStep(VersionCatalogStep.create()).test(
				"[libraries]\nfoo={module='a,b',version=\"1.0\"}\n",
				"[libraries]\nfoo = { module = 'a,b', version = \"1.0\" }\n");
	}

	@Test
	void escapedBackslashBeforeArrayDelimiterDoesNotChangeStrings() throws Exception {
		StepHarness.forStep(VersionCatalogStep.create()).test(
				"[bundles]\nfoo=[\"a\\\\\",\"b,c\"]\n",
				"[bundles]\nfoo = [ \"a\\\\\", \"b,c\" ]\n");
	}

	@Test
	void escapedBackslashBeforeTableDelimiterDoesNotChangeStrings() throws Exception {
		StepHarness.forStep(VersionCatalogStep.create()).test(
				"[libraries]\nfoo={module=\"a\\\\\",version=\"b,c\"}\n",
				"[libraries]\nfoo = { module = \"a\\\\\", version = \"b,c\" }\n");
	}

	@Test
	void singleLineStringsCannotConsumeFollowingEntries() {
		for (String quote : new String[]{"\"", "'"}) {
			StepHarness.forStep(VersionCatalogStep.create())
					.expectLintsOf("[versions]\nfoo = " + quote + "1.0\nbar = " + quote + "2.0\n")
					.toBe("L2 versionCatalog(unterminatedEntry) Unterminated version catalog entry in [versions]");
		}
	}

	@Test
	void escapedNewlineCannotContinueSingleLineString() {
		StepHarness.forStep(VersionCatalogStep.create())
				.expectLintsOf("[versions]\nfoo = \"1.0\\\nbar = \"2.0\n")
				.toBe("L2 versionCatalog(unterminatedEntry) Unterminated version catalog entry in [versions]");
	}

	@Test
	void tripleQuotedStringCommasInArraysArePreserved() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		for (String delimiter : new String[]{"\"\"\"", "'''"}) {
			harness.test(
					"[bundles]\nfoo=[" + delimiter + "a,b" + delimiter + ",'c']\n",
					"[bundles]\nfoo = [ " + delimiter + "a,b" + delimiter + ", 'c' ]\n");
		}
	}

	@Test
	void multilineArrayCommentsKeepTheirLineBoundaries() throws Exception {
		StepHarness.forStep(VersionCatalogStep.create()).test(
				"[bundles]\nzoo = [\"c\"]\n\"alpha\" = [\n  \"a\", # [keep this note\n  # } another note\n  \"b\"\n]\n",
				"[bundles]\n\"alpha\" = [\n  \"a\", # [keep this note\n  # } another note\n  \"b\"\n]\nzoo = [ \"c\" ]\n");
	}

	@Test
	void multilineArrayWithoutCommentsStillJoins() throws Exception {
		StepHarness.forStep(VersionCatalogStep.create()).test(
				"[bundles]\nfoo = [\n  \"a\",\n  \"b\"\n]\n",
				"[bundles]\nfoo = [ \"a\", \"b\" ]\n");
	}

	@Test
	void preservedMultilineEntriesStillStripQuotedKeys() throws Exception {
		StepHarness.forStep(VersionCatalogStep.create(true)).test(
				"[bundles]\n\"foo\" = [\n  \"a#b\" # keep this note\n]\n",
				"[bundles]\nfoo = [\n  \"a#b\" # keep this note\n]\n");
	}

	@Test
	void quotedCommentAndMultilineMarkersDoNotPreventJoiningArrays() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		for (String value : new String[]{"\"a#b\"", "'a#b'", "\"'''\"", "'\"\"\"'"}) {
			harness.test(
					"[bundles]\nfoo = [\n  " + value + "\n]\n",
					"[bundles]\nfoo = [ " + value + " ]\n");
		}
	}

	@Test
	void multilineBasicStringCanContinueAfterEscapedNewline() throws Exception {
		StepHarness.forStep(VersionCatalogStep.create()).testUnaffected(
				"[versions]\nfoo = \"\"\"1.0\\\n  continued\"\"\"\n");
	}

	@Test
	void multilineStringsKeepHashesBracketsAndWhitespace() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		for (String delimiter : new String[]{"\"\"\"", "'''"}) {
			harness.testUnaffected("[versions]\nfoo = " + delimiter + "1.0  \n# [literal text\n" + delimiter + "\n");
		}
	}

	@Test
	void unfinishedEntryDoesNotReturnPartialCatalog() throws Exception {
		String input = "# Catalog\n\n[bundles]\nalpha=[\"a\"]\nzoo = [\n  \"b\"\n";
		try (Formatter formatter = Formatter.builder()
				.steps(List.of(VersionCatalogStep.create()))
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.encoding(StandardCharsets.UTF_8)
				.build()) {
			StepHarness.forFormatter(formatter).expectLintsOf(input)
					.toBe("L5 versionCatalog(unterminatedEntry) Unterminated version catalog entry in [bundles]");
			LintState state = LintState.of(formatter, new File("libs.versions.toml"), input.getBytes(StandardCharsets.UTF_8));
			assertThat(state.isHasLints()).isTrue();
			assertThat(state.getDirtyState().isClean()).isTrue();
		}
	}

	@Test
	void inlineCommentOnInlineTable() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"[libraries]\nfoo = {module=\"org:foo\",version.ref=\"bar\"} # important\n",
				"[libraries]\nfoo = { module = \"org:foo\", version.ref = \"bar\" } # important\n");
	}

	@Test
	void preamblePreserved() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"# Catalog header\n\n# Generated\n\n[versions]\nfoo = \"1.0\"\n",
				"# Catalog header\n\n# Generated\n\n[versions]\nfoo = \"1.0\"\n");
	}

	@Test
	void quotedKeySortsByLogicalName() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"[versions]\n\"zoo\" = \"1.0\"\nalpha = \"2.0\"\n",
				"[versions]\nalpha = \"2.0\"\n\"zoo\" = \"1.0\"\n");
	}

	@Test
	void stripQuotedKeysDisabledByDefault() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"[versions]\n\"foo\" = \"1.0\"\n",
				"[versions]\n\"foo\" = \"1.0\"\n");
	}

	@Test
	void stripQuotedKeysEnabled() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create(true));
		harness.test(
				"[versions]\n\"foo\" = \"1.0\"\n",
				"[versions]\nfoo = \"1.0\"\n");
	}

	@Test
	void stripQuotedKeysPreservesNonBareKeys() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create(true));
		harness.test(
				"[versions]\n\"foo.bar\" = \"1.0\"\n",
				"[versions]\n\"foo.bar\" = \"1.0\"\n");
	}

	@Test
	void multiLineInlineTableJoined() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"[libraries]\nfoo = {\n  module = \"org:foo\",\n  version.ref = \"bar\"\n}\n",
				"[libraries]\nfoo = { module = \"org:foo\", version.ref = \"bar\" }\n");
	}

	@Test
	void multiLineInlineTableWithTrailingComma() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.test(
				"[libraries]\nfoo = {\n  module = \"org:foo\",\n  version.ref = \"bar\",\n}\n",
				"[libraries]\nfoo = { module = \"org:foo\", version.ref = \"bar\" }\n");
	}

	@Test
	void longLineStaysSingleLine() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create());
		harness.testUnaffected(
				"[libraries]\nfoo = { module = \"org.example:foo-bar-baz-qux\", version.ref = \"fooBarBazQux\" }\n");
	}

	@Test
	void equality() throws Exception {
		new SerializableEqualityTester() {
			boolean stripQuotedKeys;

			@Override
			protected void setupTest(API api) {
				api.areDifferentThan();
				stripQuotedKeys = true;
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return VersionCatalogStep.create(stripQuotedKeys);
			}
		}.testEquals();
	}
}
