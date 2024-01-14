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
package com.diffplug.spotless.glue.diktat.compat;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

interface DiktatReporting {
	static <T> void reportIfRequired(
			List<T> errors,
			ToIntFunction<T> lineGetter,
			ToIntFunction<T> columnGetter,
			Function<T, String> detailGetter) {
		if (!errors.isEmpty()) {
			StringBuilder error = new StringBuilder();
			error.append("There are ").append(errors.size()).append(" unfixed errors:");
			for (T er : errors) {
				error.append(System.lineSeparator())
						.append("Error on line: ").append(lineGetter.applyAsInt(er))
						.append(", column: ").append(columnGetter.applyAsInt(er))
						.append(" cannot be fixed automatically")
						.append(System.lineSeparator())
						.append(detailGetter.apply(er));
			}
			throw new AssertionError(error);
		}
	}
}
