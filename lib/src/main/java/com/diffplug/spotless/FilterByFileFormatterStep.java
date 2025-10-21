/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

final class FilterByFileFormatterStep extends DelegateFormatterStep {
	private final SerializableFileFilter filter;

	FilterByFileFormatterStep(FormatterStep delegateStep, SerializableFileFilter filter) {
		super(delegateStep);
		this.filter = requireNonNull(filter);
	}

	@Override
	public @Nullable String format(String raw, File file) throws Exception {
		requireNonNull(raw, "raw");
		requireNonNull(file, "file");
		if (filter.accept(file)) {
			return delegateStep.format(raw, file);
		} else {
			return raw;
		}
	}

	@Override
	public List<Lint> lint(String content, File file) throws Exception {
		requireNonNull(content, "content");
		requireNonNull(file, "file");
		if (filter.accept(file)) {
			return delegateStep.lint(content, file);
		} else {
			return List.of();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FilterByFileFormatterStep that = (FilterByFileFormatterStep) o;
		return Objects.equals(delegateStep, that.delegateStep)
				&& Objects.equals(filter, that.filter);
	}

	@Override
	public int hashCode() {
		return Objects.hash(delegateStep, filter);
	}

	@Serial
	private static final long serialVersionUID = 1L;
}
