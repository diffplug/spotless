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
package com.diffplug.spotless.glue.java;

import static com.diffplug.spotless.glue.java.GoogleJavaFormatUtils.fixWindowsBug;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.ImportOrderer;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.google.googlejavaformat.java.RemoveUnusedImports;
import com.google.googlejavaformat.java.StringWrapper;

import com.diffplug.spotless.FormatterFunc;

public class GoogleJavaFormatFormatterFunc implements FormatterFunc {

	@Nonnull
	private final Formatter formatter;

	@Nonnull
	private final String version;
	@Nonnull
	private final JavaFormatterOptions.Style formatterStyle;
	private final boolean reflowStrings;

	public GoogleJavaFormatFormatterFunc(@Nonnull String version, @Nonnull String style, boolean reflowStrings) {
		this.version = Objects.requireNonNull(version);
		this.formatterStyle = JavaFormatterOptions.Style.valueOf(Objects.requireNonNull(style));
		this.reflowStrings = reflowStrings;

		this.formatter = new Formatter(JavaFormatterOptions.builder()
				.style(formatterStyle)
				.build());
	}

	@Override
	@Nonnull
	public String apply(@Nonnull String input) throws Exception {
		String formatted = formatter.formatSource(input);
		String removedUnused = RemoveUnusedImports.removeUnusedImports(formatted);
		String sortedImports = ImportOrderer.reorderImports(removedUnused, formatterStyle);
		String reflowedLongStrings = reflowLongStrings(sortedImports);
		return fixWindowsBug(reflowedLongStrings, version);
	}

	private String reflowLongStrings(String input) throws FormatterException {
		if (reflowStrings) {
			return StringWrapper.wrap(input, formatter);
		} else {
			return input;
		}
	}
}
