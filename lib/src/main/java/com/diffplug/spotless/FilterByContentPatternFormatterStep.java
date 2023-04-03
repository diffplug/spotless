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
package com.diffplug.spotless;

import java.io.File;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

final class FilterByContentPatternFormatterStep extends DelegateFormatterStep {
	final Pattern contentPattern;

	FilterByContentPatternFormatterStep(FormatterStep delegateStep, String contentPattern) {
		super(delegateStep);
		this.contentPattern = Pattern.compile(Objects.requireNonNull(contentPattern));
	}

	@Override
	public @Nullable String format(String raw, File file) throws Exception {
		Objects.requireNonNull(raw, "raw");
		Objects.requireNonNull(file, "file");
		var matcher = contentPattern.matcher(raw);
		if (matcher.find()) {
			return delegateStep.format(raw, file);
		} else {
			return raw;
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
		var that = (FilterByContentPatternFormatterStep) o;
		return Objects.equals(delegateStep, that.delegateStep) &&
				Objects.equals(contentPattern.pattern(), that.contentPattern.pattern());
	}

	@Override
	public int hashCode() {
		return Objects.hash(delegateStep, contentPattern.pattern());
	}

	private static final long serialVersionUID = 1L;
}
