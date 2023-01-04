/*
 * Copyright 2023 DiffPlug
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class KtLintCompat0Dot48Dot1AdapterTest {
	@Test
	public void testDefaults(@TempDir Path path) throws IOException {
		KtLintCompat0Dot48Dot1Adapter KtLintCompat0Dot48Dot1Adapter = new KtLintCompat0Dot48Dot1Adapter();
		try (InputStream is = KtLintCompat0Dot48Dot1AdapterTest.class.getResourceAsStream("/empty_class_body.kt")) {
			Files.copy(is, path.resolve("empty_class_body.kt"));
		}
		String text = new String(Files.readAllBytes(path.resolve("empty_class_body.kt")));

		Map<String, String> userData = new HashMap<>();

		Map<String, Object> editorConfigOverrideMap = new HashMap<>();

		String formatted = KtLintCompat0Dot48Dot1Adapter.format(text, "empty_class_body.kt", false, false, userData, editorConfigOverrideMap);
		assertEquals("class empty_class_body\n", formatted);
	}

	@Test
	public void testEditorConfigCanDisable(@TempDir Path path) throws IOException {
		KtLintCompat0Dot48Dot1Adapter KtLintCompat0Dot48Dot1Adapter = new KtLintCompat0Dot48Dot1Adapter();
		try (InputStream is = KtLintCompat0Dot48Dot1AdapterTest.class.getResourceAsStream("/fails_no_semicolons.kt")) {
			Files.copy(is, path.resolve("fails_no_semicolons.kt"));
		}
		String text = new String(Files.readAllBytes(path.resolve("fails_no_semicolons.kt")));

		Map<String, String> userData = new HashMap<>();

		Map<String, Object> editorConfigOverrideMap = new HashMap<>();
		editorConfigOverrideMap.put("indent_style", "tab");
		editorConfigOverrideMap.put("ktlint_standard_no-semi", "disabled");

		String formatted = KtLintCompat0Dot48Dot1Adapter.format(text, "fails_no_semicolons.kt", false, false, userData, editorConfigOverrideMap);
		assertEquals("class fails_no_semicolons {\n\tval i = 0;\n}\n", formatted);
	}
}
