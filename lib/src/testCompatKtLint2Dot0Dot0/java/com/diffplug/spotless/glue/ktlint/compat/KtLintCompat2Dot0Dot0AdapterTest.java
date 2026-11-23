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
package com.diffplug.spotless.glue.ktlint.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class KtLintCompat2Dot0Dot0AdapterTest {
	@Test
	public void testDefaults(@TempDir Path path) throws IOException {
		KtLintCompat2Dot0Dot0Adapter adapter = new KtLintCompat2Dot0Dot0Adapter();
		var content = loadAndWriteText(path, "EmptyClassBody.kt");
		final Path filePath = Path.of(path.toString(), "EmptyClassBody.kt");

		Map<String, Object> editorConfigOverrideMap = new HashMap<>();

		String formatted = adapter.format(content, filePath, null, editorConfigOverrideMap);
		assertEquals("class EmptyClassBody\n", formatted);
	}

	@Test
	public void testEditorConfigCanDisable(@TempDir Path path) throws IOException {
		KtLintCompat2Dot0Dot0Adapter adapter = new KtLintCompat2Dot0Dot0Adapter();
		var content = loadAndWriteText(path, "FailsNoSemicolons.kt");
		final Path filePath = Path.of(path.toString(), "FailsNoSemicolons.kt");

		Map<String, Object> editorConfigOverrideMap = new HashMap<>();
		editorConfigOverrideMap.put("indent_style", "tab");
		// ktlint 2.x renamed the 'no-semi' rule to 'semicolon'
		editorConfigOverrideMap.put("ktlint_standard_semicolon", "disabled");

		String formatted = adapter.format(content, filePath, null, editorConfigOverrideMap);
		assertEquals("class FailsNoSemicolons {\n\tval i = 0;\n}\n", formatted);
	}

	@Test
	public void testUnfixableViolationIsReportedWithLineAndRuleId(@TempDir Path path) throws IOException {
		KtLintCompat2Dot0Dot0Adapter adapter = new KtLintCompat2Dot0Dot0Adapter();
		var content = loadAndWriteText(path, "FailsWildcardImport.kt");
		final Path filePath = Path.of(path.toString(), "FailsWildcardImport.kt");

		Map<String, Object> editorConfigOverrideMap = new HashMap<>();

		KtLintCompatReporting.KtlintSpotlessException e = assertThrows(KtLintCompatReporting.KtlintSpotlessException.class,
				() -> adapter.format(content, filePath, null, editorConfigOverrideMap));
		assertEquals(3, e.line);
		assertEquals("standard:no-wildcard-imports", e.ruleId);
		assertEquals("Wildcard import", e.detail);
	}

	private static String loadAndWriteText(Path path, String name) throws IOException {
		try (InputStream is = KtLintCompat2Dot0Dot0AdapterTest.class.getResourceAsStream("/" + name)) {
			Files.copy(is, path.resolve(name));
		}
		return Files.readString(path.resolve(name));
	}
}
