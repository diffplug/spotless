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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class KtLintCompat0Dot49Dot0AdapterTest {
	@Test
	public void testDefaults(@TempDir Path path) throws IOException {
		KtLintCompat0Dot49Dot0Adapter ktLintCompat0Dot49Dot0Adapter = new KtLintCompat0Dot49Dot0Adapter();
		String text = loadAndWriteText(path, "EmptyClassBody.kt");
		final Path filePath = Paths.get(path.toString(), "EmptyClassBody.kt");

		Map<String, Object> editorConfigOverrideMap = new HashMap<>();

		String formatted = ktLintCompat0Dot49Dot0Adapter.format(text, filePath, null, editorConfigOverrideMap);
		assertEquals("class EmptyClassBody\n", formatted);
	}

	@Test
	public void testEditorConfigCanDisable(@TempDir Path path) throws IOException {
		KtLintCompat0Dot49Dot0Adapter ktLintCompat0Dot49Dot0Adapter = new KtLintCompat0Dot49Dot0Adapter();
		String text = loadAndWriteText(path, "FailsNoSemicolons.kt");
		final Path filePath = Paths.get(path.toString(), "FailsNoSemicolons.kt");

		Map<String, Object> editorConfigOverrideMap = new HashMap<>();
		editorConfigOverrideMap.put("indent_style", "tab");
		editorConfigOverrideMap.put("ktlint_standard_no-semi", "disabled");

		String formatted = ktLintCompat0Dot49Dot0Adapter.format(text, filePath, null, editorConfigOverrideMap);
		assertEquals("class FailsNoSemicolons {\n\tval i = 0;\n}\n", formatted);
	}

	private static String loadAndWriteText(Path path, String name) throws IOException {
		try (InputStream is = KtLintCompat0Dot49Dot0AdapterTest.class.getResourceAsStream("/" + name)) {
			Files.copy(is, path.resolve(name));
		}
		return new String(Files.readAllBytes(path.resolve(name)), StandardCharsets.UTF_8);
	}

}
