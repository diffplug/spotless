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

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
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
	void longLineSplitsInlineTable() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create(false, 40));
		harness.test(
				"[libraries]\nfoo = { module = \"org.example:foo-bar\", version.ref = \"fooBar\" }\n",
				"[libraries]\nfoo = {\n  module = \"org.example:foo-bar\",\n  version.ref = \"fooBar\",\n}\n");
	}

	@Test
	void shortLineStaysSingleLine() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create(false, 200));
		harness.test(
				"[libraries]\nfoo = {module=\"org:foo\",version.ref=\"bar\"}\n",
				"[libraries]\nfoo = { module = \"org:foo\", version.ref = \"bar\" }\n");
	}

	@Test
	void splitLineIdempotent() throws Exception {
		StepHarness harness = StepHarness.forStep(VersionCatalogStep.create(false, 40));
		harness.testUnaffected(
				"[libraries]\nfoo = {\n  module = \"org.example:foo-bar\",\n  version.ref = \"fooBar\",\n}\n");
	}

	@Test
	void equality() throws Exception {
		new SerializableEqualityTester() {
			boolean stripQuotedKeys;
			int maxLineLength = 120;

			@Override
			protected void setupTest(API api) {
				api.areDifferentThan();
				stripQuotedKeys = true;
				api.areDifferentThan();
				maxLineLength = 80;
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return VersionCatalogStep.create(stripQuotedKeys, maxLineLength);
			}
		}.testEquals();
	}
}
