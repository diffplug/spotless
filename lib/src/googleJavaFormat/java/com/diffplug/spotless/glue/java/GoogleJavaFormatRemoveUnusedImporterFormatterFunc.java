/*
 * Copyright 2023-2025 DiffPlug
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

import static java.util.Objects.requireNonNull;

import javax.annotation.Nonnull;

import com.google.googlejavaformat.java.RemoveUnusedImports;

import com.diffplug.spotless.FormatterFunc;

public class GoogleJavaFormatRemoveUnusedImporterFormatterFunc implements FormatterFunc {

	@Nonnull
	private final String version;

	public GoogleJavaFormatRemoveUnusedImporterFormatterFunc(@Nonnull String version) {
		this.version = requireNonNull(version);
	}

	@Override
	@Nonnull
	public String apply(@Nonnull String input) throws Exception {
		return RemoveUnusedImports.removeUnusedImports(input);
	}
}
