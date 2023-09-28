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

import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.ImportOrderer;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import com.google.googlejavaformat.java.RemoveUnusedImports;
import com.google.googlejavaformat.java.StringWrapper;

import com.diffplug.spotless.FormatterFunc;

// Used via reflection by the Gradle plugin.
@SuppressWarnings("unused")
public class GoogleJavaFormatFormatterFunc implements FormatterFunc {

	@Nonnull
	private final Formatter formatter;

	@Nonnull
	private final String version;

	@Nonnull
	private final Style formatterStyle;

	private final boolean reflowStrings;

	private final boolean reorderImports;

	public GoogleJavaFormatFormatterFunc(@Nonnull String version, @Nonnull String style, boolean reflowStrings, boolean reorderImports, boolean formatJavadoc) {
		this.version = Objects.requireNonNull(version);
		this.formatterStyle = Style.valueOf(Objects.requireNonNull(style));
		this.reflowStrings = reflowStrings;
		this.reorderImports = reorderImports;

		JavaFormatterOptions.Builder builder = JavaFormatterOptions.builder().style(formatterStyle);
		if (!formatJavadoc) {
			builder = builder.formatJavadoc(false);
		}
		this.formatter = new Formatter(builder.build());
	}

	@Override
	@Nonnull
	public String apply(@Nonnull String input) throws Exception {
		String formatted = formatter.formatSource(input);
		String removedUnused = RemoveUnusedImports.removeUnusedImports(formatted);
		String sortedImports = ImportOrderer.reorderImports(removedUnused, reorderImports ? formatterStyle : Style.GOOGLE);
		return reflowLongStrings(sortedImports);
	}

	private String reflowLongStrings(String input) throws FormatterException {
		if (reflowStrings) {
			return StringWrapper.wrap(input, formatter);
		} else {
			return input;
		}
	}
}
