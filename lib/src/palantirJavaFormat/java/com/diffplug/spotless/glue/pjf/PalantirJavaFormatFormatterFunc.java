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
package com.diffplug.spotless.glue.pjf;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.ImportOrderer;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.palantir.javaformat.java.RemoveUnusedImports;

import com.diffplug.spotless.FormatterFunc;

public class PalantirJavaFormatFormatterFunc implements FormatterFunc {

	private final Formatter formatter;

	private final JavaFormatterOptions.Style formatterStyle;

	/**
	 * Creates a new formatter func that formats code via Palantir.
	 * @param style The style to use for formatting.
	 * @param formatJavadoc Whether to format Java docs. Requires at least Palantir 2.36.0 or later, otherwise the
	 * constructor will throw.
	 */
	public PalantirJavaFormatFormatterFunc(String style, boolean formatJavadoc) {
		this.formatterStyle = JavaFormatterOptions.Style.valueOf(style);
		JavaFormatterOptions.Builder builder = JavaFormatterOptions.builder();
		builder.style(formatterStyle);
		if (formatJavadoc) {
			applyFormatJavadoc(builder);
		}
		formatter = Formatter.createFormatter(builder.build());
	}

	@Override
	public String apply(String input) throws Exception {
		String source = input;
		source = ImportOrderer.reorderImports(source, formatterStyle);
		source = RemoveUnusedImports.removeUnusedImports(source);
		return formatter.formatSource(source);
	}

	@Override
	public String toString() {
		return "PalantirJavaFormatFormatterFunc{formatter=" + formatter + '}';
	}

	private static void applyFormatJavadoc(JavaFormatterOptions.Builder builder) {
		// The formatJavadoc option is available since Palantir 2.36.0
		// To support older versions for now, attempt to invoke the builder method via reflection.
		try {
			Method formatJavadoc = JavaFormatterOptions.Builder.class.getMethod("formatJavadoc", boolean.class);
			formatJavadoc.invoke(builder, true);
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			throw new IllegalStateException("Cannot enable formatJavadoc option, make sure you are using Palantir with version 2.36.0 or later", e);
		}
	}
}
