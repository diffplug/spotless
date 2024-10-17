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
package com.diffplug.spotless.glue.ktlint.compat;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

public interface KtLintCompatAdapter {

	String format(
			String content,
			Path path,
			Path editorConfigPath,
			Map<String, Object> editorConfigOverrideMap) throws NoSuchFieldException, IllegalAccessException;

	static void setCodeContent(Object code, String content) {
		AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
			try {
				Field contentField = code.getClass().getDeclaredField("content");
				contentField.setAccessible(true);
				contentField.set(code, content);
			} catch (NoSuchFieldException | IllegalAccessException e) {
				// Handle exceptions as needed
				throw new RuntimeException("Failed to set content field", e);
			}
			return null;
		});
	}
}
