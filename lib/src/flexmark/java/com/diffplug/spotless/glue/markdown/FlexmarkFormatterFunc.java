/*
 * Copyright 2021-2025 DiffPlug
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vladsch.flexmark.formatter.Formatter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.parser.PegdownExtensions;
import com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.markdown.FlexmarkConfig;

/**
 * The formatter function for <a href="https://github.com/vsch/flexmark-java">flexmark-java</a>.
 */
public class FlexmarkFormatterFunc implements FormatterFunc {

	private static final Map<String, String> KNOWN_EXTENSIONS = new HashMap<>();
	static {
		// using strings to maximize compatibility with older flexmark versions
		KNOWN_EXTENSIONS.put("Abbreviation", "com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension");
		KNOWN_EXTENSIONS.put("Admonition", "com.vladsch.flexmark.ext.admonition.AdmonitionExtension");
		KNOWN_EXTENSIONS.put("Aside", "com.vladsch.flexmark.ext.aside.AsideExtension");
		KNOWN_EXTENSIONS.put("Attributes", "com.vladsch.flexmark.ext.attributes.AttributesExtension");
		KNOWN_EXTENSIONS.put("Definition", "com.vladsch.flexmark.ext.definition.DefinitionExtension");
		KNOWN_EXTENSIONS.put("Emoji", "com.vladsch.flexmark.ext.emoji.EmojiExtension");
		KNOWN_EXTENSIONS.put("EnumeratedReference", "com.vladsch.flexmark.ext.enumerated.reference.EnumeratedReferenceExtension");
		KNOWN_EXTENSIONS.put("Footnote", "com.vladsch.flexmark.ext.footnotes.FootnoteExtension");
		KNOWN_EXTENSIONS.put("GitLab", "com.vladsch.flexmark.ext.gitlab.GitLabExtension");
		KNOWN_EXTENSIONS.put("JekyllFrontMatter", "com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterExtension");
		KNOWN_EXTENSIONS.put("JekyllTag", "com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension");
		KNOWN_EXTENSIONS.put("Macros", "com.vladsch.flexmark.ext.macros.MacrosExtension");
		KNOWN_EXTENSIONS.put("SimToc", "com.vladsch.flexmark.ext.toc.SimTocExtension");
		KNOWN_EXTENSIONS.put("Tables", "com.vladsch.flexmark.ext.tables.TablesExtension");
		KNOWN_EXTENSIONS.put("TaskList", "com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension");
		KNOWN_EXTENSIONS.put("WikiLink", "com.vladsch.flexmark.ext.wikilink.WikiLinkExtension");
		KNOWN_EXTENSIONS.put("YamlFrontMatter", "com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension");
	}

	private final Parser parser;
	private final Formatter formatter;

	public FlexmarkFormatterFunc(FlexmarkConfig config) {
		// flexmark-java has a separate parser and renderer (formatter)
		// this is build from the example in https://github.com/vsch/flexmark-java/wiki/Markdown-Formatter

		// The emulation profile generally determines the markdown flavor. We use the same one for both the parser and
		// the formatter, to make sure this formatter func is idempotent.
		final ParserEmulationProfile emulationProfile = ParserEmulationProfile.valueOf(config.getEmulationProfile());

		final MutableDataHolder parserOptions = createParserOptions(emulationProfile, config);
		final MutableDataHolder formatterOptions = createFormatterOptions(parserOptions, emulationProfile);

		parser = Parser.builder(parserOptions).build();
		formatter = Formatter.builder(formatterOptions).build();
	}

	/**
	 * Creates the parser options.
	 * See: https://github.com/vsch/flexmark-java/wiki/Markdown-Formatter#options
	 *
	 * @param emulationProfile the emulation profile (or flavor of markdown) the parser should use
	 * @return the created parser options
	 */
	private static MutableDataHolder createParserOptions(ParserEmulationProfile emulationProfile, FlexmarkConfig config) {
		int pegdownExtensions = buildPegdownExtensions(config.getPegdownExtensions());
		Extension[] extensions = buildExtensions(config.getExtensions());
		final MutableDataHolder parserOptions = PegdownOptionsAdapter.flexmarkOptions(
				pegdownExtensions, extensions).toMutable();
		parserOptions.set(Parser.PARSER_EMULATION_PROFILE, emulationProfile);
		return parserOptions;
	}

	/**
	 * Loads all listed pegdown extensions by using the constants defined in {@link PegdownExtensions}.
	 * Additionally, pure digit strings are directly converted to allow highly customized configurations.
	 *
	 * @param config the string-array configuration for pegdown
	 * @return bit-wise or'd extensions
	 */
	private static int buildPegdownExtensions(List<String> config) {
		int extensions = PegdownExtensions.NONE;
		for (String str : config) {
			if (str.matches("\\d+")) {
				extensions |= Integer.parseInt(str);
			} else if (str.matches("(0x|0X)?[a-fA-F0-9]+")) {
				extensions |= Integer.decode(str);
			} else {
				try {
					Field field = PegdownExtensions.class.getField(str);
					extensions |= field.getInt(null);
				} catch (ReflectiveOperationException e) {
					throw new IllegalArgumentException("Unknown PegdownExtension '" + str + "'");
				}
			}
		}
		return extensions;
	}

	/**
	 * Loads all listed extensions by looking up the implementation class (optionally resolving shortcuts
	 * for known extensions) and calling the conventional create-method.
	 *
	 * @param config the string-list of the configured extensions
	 * @return the array of Extension instances
	 */
	private static Extension[] buildExtensions(List<String> config) {
		Extension[] extensions = new Extension[config.size()];
		for (int i = 0; i < extensions.length; i++) {
			String className = KNOWN_EXTENSIONS.getOrDefault(config.get(i), config.get(i));
			try {
				Class<?> c = Extension.class.getClassLoader().loadClass(className);
				Method create = c.getMethod("create");
				extensions[i] = Extension.class.cast(create.invoke(null));
			} catch (ReflectiveOperationException e) {
				throw new IllegalArgumentException("Unknown flexmark extension '" + config.get(i) + "'");
			}
		}
		return extensions;
	}

	/**
	 * Creates the formatter options, copies the parser extensions and changes defaults that make sense for a formatter.
	 * See: https://github.com/vsch/flexmark-java/wiki/Markdown-Formatter#options
	 *
	 * @param parserOptions the options used for the parser
	 * @param emulationProfile the emulation profile (or flavor of markdown) the formatter should use
	 * @return the created formatter options
	 */
	private static MutableDataHolder createFormatterOptions(MutableDataHolder parserOptions,
			ParserEmulationProfile emulationProfile) {
		final MutableDataHolder formatterOptions = new MutableDataSet();
		formatterOptions.set(Parser.EXTENSIONS, Parser.EXTENSIONS.get(parserOptions));
		formatterOptions.set(Formatter.FORMATTER_EMULATION_PROFILE, emulationProfile);
		return formatterOptions;
	}

	@Override
	public String apply(String input) throws Exception {
		final Document parsedMarkdown = parser.parse(input);
		return formatter.render(parsedMarkdown);
	}
}
