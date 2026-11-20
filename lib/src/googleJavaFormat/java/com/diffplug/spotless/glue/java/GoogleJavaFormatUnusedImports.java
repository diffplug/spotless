/*
 * Copyright 2026 DiffPlug
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

import javax.annotation.Nonnull;

import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.RemoveUnusedImports;
import com.sun.source.util.DocTrees;

final class GoogleJavaFormatUnusedImports {
	private GoogleJavaFormatUnusedImports() {}

	/**
	 * Calls {@link RemoveUnusedImports#removeUnusedImports(String)} with javac's class loader as
	 * the context class loader.
	 * <p>
	 * On JDK 23+, javac parses Markdown doc comments ({@code ///}) with a transformer from the
	 * {@code jdk.internal.md} module, which it looks up with {@link java.util.ServiceLoader} on the
	 * context class loader. Gradle and Maven run Spotless with a context class loader that cannot
	 * see that module, so javac silently fell back to treating Markdown doc comments as plain text,
	 * and imports used only by their links were removed
	 * (https://github.com/diffplug/spotless/issues/3161).
	 */
	@Nonnull
	static String removeUnusedImports(@Nonnull String input) throws FormatterException {
		Thread thread = Thread.currentThread();
		ClassLoader originalClassLoader = thread.getContextClassLoader();
		thread.setContextClassLoader(DocTrees.class.getClassLoader());
		try {
			return RemoveUnusedImports.removeUnusedImports(input);
		} finally {
			thread.setContextClassLoader(originalClassLoader);
		}
	}
}
