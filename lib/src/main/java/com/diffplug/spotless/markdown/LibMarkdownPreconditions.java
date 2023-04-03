/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.markdown;

import java.util.Map;
import java.util.Objects;

final class LibMarkdownPreconditions {
	// prevent direct instantiation
	private LibMarkdownPreconditions() {}

	static <K, V> Map<K, V> requireKeysAndValuesNonNull(Map<K, V> map) {
		Objects.requireNonNull(map);
		map.forEach((key, value) -> {
			var errorMessage = key + "=" + value;
			Objects.requireNonNull(key, errorMessage);
			Objects.requireNonNull(value, errorMessage);
		});
		return map;
	}
}
