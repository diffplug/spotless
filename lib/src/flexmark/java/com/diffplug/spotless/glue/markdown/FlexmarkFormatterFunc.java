/*
 * Copyright 2021 DiffPlug
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
package com.diffplug.spotless.glue.markdown;

import com.vladsch.flexmark.formatter.Formatter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.parser.PegdownExtensions;
import com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

import com.diffplug.spotless.FormatterFunc;

/**
 * The formatter function for <a href="https://github.com/vsch/flexmark-java">flexmark-java</a>.
 */
public class FlexmarkFormatterFunc implements FormatterFunc {

	/**
	 * The emulation profile is used by both the parser and the formatter and generally determines the markdown flavor.
	 * COMMONMARK is the default defined by flexmark-java.
	 */
	private static final String DEFAULT_EMULATION_PROFILE = "COMMONMARK";

	private final Parser parser;
	private final Formatter formatter;

	public FlexmarkFormatterFunc(final MutableDataHolder customFormatterOptions) {
		// flexmark-java has a separate parser and renderer (formatter)
		// this is build from the example in https://github.com/vsch/flexmark-java/wiki/Markdown-Formatter

		// The emulation profile generally determines the markdown flavor. We use the same one for both the parser and
		// the formatter, to make sure this formatter func is idempotent.
		final ParserEmulationProfile emulationProfile = ParserEmulationProfile.valueOf(DEFAULT_EMULATION_PROFILE);

		final MutableDataHolder parserOptions = createParserOptions(emulationProfile);
		final MutableDataHolder formatterOptions = createFormatterOptions(parserOptions, emulationProfile);

		parser = Parser.builder(parserOptions).build();
		formatter = Formatter.builder(MutableDataSet.merge(formatterOptions, customFormatterOptions)).build();
	}

	/**
	 * Creates the parser options.
	 * See: https://github.com/vsch/flexmark-java/wiki/Markdown-Formatter#options
	 *
	 * @param emulationProfile the emulation profile (or flavor of markdown) the parser should use
	 * @return the created parser options
	 */
	private static MutableDataHolder createParserOptions(ParserEmulationProfile emulationProfile) {
		final MutableDataHolder parserOptions = PegdownOptionsAdapter.flexmarkOptions(PegdownExtensions.ALL).toMutable();
		parserOptions.set(Parser.PARSER_EMULATION_PROFILE, emulationProfile);
		return parserOptions;
	}

	/**
	 * Creates the formatter options, copies the parser extensions and changes defaults that make sense for a formatter.
	 * See: https://github.com/vsch/flexmark-java/wiki/Markdown-Formatter#options
	 *
	 * @param parserOptions the options used for the parser
	 * @param emulationProfile the emulation profile (or flavor of markdown) the formatter should use
	 * @return the created formatter options
	 */
	private static MutableDataHolder createFormatterOptions(MutableDataHolder parserOptions, ParserEmulationProfile emulationProfile) {
		final MutableDataHolder formatterOptions = new MutableDataSet();
		formatterOptions.set(Parser.EXTENSIONS, Parser.EXTENSIONS.get(parserOptions));
		formatterOptions.set(Formatter.FORMATTER_EMULATION_PROFILE, emulationProfile);
		return formatterOptions;
	}

	@Override
	public String apply(String input) {
		final Document parsedMarkdown = parser.parse(input);
		return formatter.render(parsedMarkdown);
	}
}
