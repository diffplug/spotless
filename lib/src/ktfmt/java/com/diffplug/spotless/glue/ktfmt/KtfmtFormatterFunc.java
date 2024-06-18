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
package com.diffplug.spotless.glue.ktfmt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.facebook.ktfmt.format.Formatter;
import com.facebook.ktfmt.format.FormattingOptions;

import com.diffplug.spotless.FormatterFunc;

public final class KtfmtFormatterFunc implements FormatterFunc {

	@Nonnull
	private final KtfmtStyle style;

	@Nullable
	private final KtfmtFormattingOptions ktfmtFormattingOptions;

	public KtfmtFormatterFunc() {
		this(KtfmtStyle.META, null);
	}

	public KtfmtFormatterFunc(@Nonnull KtfmtStyle style) {
		this(style, null);
	}

	public KtfmtFormatterFunc(@Nullable KtfmtFormattingOptions ktfmtFormattingOptions) {
		this(KtfmtStyle.META, ktfmtFormattingOptions);
	}

	public KtfmtFormatterFunc(@Nonnull KtfmtStyle style, @Nullable KtfmtFormattingOptions ktfmtFormattingOptions) {
		this.style = style;
		this.ktfmtFormattingOptions = ktfmtFormattingOptions;
	}

	@Nonnull
	@Override
	public String apply(@Nonnull String input) throws Exception {
		return Formatter.format(createFormattingOptions(), input);
	}

	private FormattingOptions createFormattingOptions() throws Exception {
		FormattingOptions formattingOptions;
		switch (style) {
		case META:
			formattingOptions = Formatter.META_FORMAT;
			break;
		case GOOGLE:
			formattingOptions = Formatter.GOOGLE_FORMAT;
			break;
		case KOTLIN_LANG:
			formattingOptions = Formatter.KOTLINLANG_FORMAT;
			break;
		default:
			throw new IllegalStateException("Unknown formatting option " + style);
		}

		if (ktfmtFormattingOptions != null) {
			formattingOptions = formattingOptions.copy(
					ktfmtFormattingOptions.getMaxWidth().orElse(formattingOptions.getMaxWidth()),
					ktfmtFormattingOptions.getBlockIndent().orElse(formattingOptions.getBlockIndent()),
					ktfmtFormattingOptions.getContinuationIndent().orElse(formattingOptions.getContinuationIndent()),
					ktfmtFormattingOptions.getManageTrailingCommas().orElse(formattingOptions.getManageTrailingCommas()),
					ktfmtFormattingOptions.getRemoveUnusedImports().orElse(formattingOptions.getRemoveUnusedImports()),
					formattingOptions.getDebuggingPrintOpsAfterFormatting());
		}

		return formattingOptions;
	}
}
