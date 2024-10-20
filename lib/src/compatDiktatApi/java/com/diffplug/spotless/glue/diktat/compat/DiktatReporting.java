/*
 * Copyright 2023-2024 DiffPlug
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public interface DiktatReporting {
	class Lint implements Serializable {
		private static final long serialVersionUID = 1L;
		public final int line;
		public final String ruleId;
		public final String detail;

		Lint(int line, String ruleId, String detail) {
			this.line = line;
			this.ruleId = ruleId;
			this.detail = detail;
		}
	}

	class LintException extends RuntimeException {
		public final List<Lint> lints;

		LintException(List<Lint> lints) {
			this.lints = lints;
		}
	}

	static <T> void reportIfRequired(
			List<T> errors,
			ToIntFunction<T> lineGetter,
			Function<T, String> codeGetter,
			Function<T, String> detailGetter) {
		if (!errors.isEmpty()) {
			var lints = new ArrayList<Lint>();
			for (T er : errors) {
				lints.add(new Lint(lineGetter.applyAsInt(er), codeGetter.apply(er), detailGetter.apply(er)));
			}
			throw new LintException(lints);
		}
	}
}
