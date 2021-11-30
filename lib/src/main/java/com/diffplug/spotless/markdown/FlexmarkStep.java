/*
 * Copyright 2016-2021 DiffPlug
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
package com.diffplug.spotless.markdown;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/** A step for <a href="https://github.com/vsch/flexmark-java">flexmark-java</a>. */
public class FlexmarkStep {
	// prevent direct instantiation
	private FlexmarkStep() {}

	private static final String DEFAULT_VERSION = "0.62.2";
	private static final String NAME = "flexmark-java";
	private static final String MAVEN_COORDINATE = "com.vladsch.flexmark:flexmark-all:";

	/**
	 * The emulation profile is used by both the parser and the formatter and generally determines the markdown flavor.
	 * COMMONMARK is the default defined by flexmark-java. It's defined here so it can be used in both the parser and
	 * the formatter, to keep the step idempotent.
	 */
	private static final String DEFAULT_EMULATION_PROFILE = "COMMONMARK";

	/** Creates a formatter step for the default version. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a formatter step for the given version. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new State(JarState.from(MAVEN_COORDINATE + version, provisioner)),
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private static class State implements Serializable {
		private static final long serialVersionUID = 1L;

		final JarState jarState;

		State(JarState jarState) {
			this.jarState = jarState;
		}

		FormatterFunc createFormat() throws Exception {
			final ClassLoader classLoader = jarState.getClassLoader();

			// flexmark-java has a separate parser and renderer (formatter)
			// this is build from the example in https://github.com/vsch/flexmark-java/wiki/Markdown-Formatter

			// first we need to create the parser and find the parse method
			final Class<?> parserClazz = classLoader.loadClass("com.vladsch.flexmark.parser.Parser");
			final Class<?> parserBuilderClazz = classLoader.loadClass("com.vladsch.flexmark.parser.Parser$Builder");
			final Class<?> parserEmulationProfileClazz = classLoader.loadClass("com.vladsch.flexmark.parser.ParserEmulationProfile");
			final Class<?> dataHolderClazz = classLoader.loadClass("com.vladsch.flexmark.util.data.DataHolder");
			final Class<?> dataKeyClazz = classLoader.loadClass("com.vladsch.flexmark.util.data.DataKey");
			final Object parserEmulationProfile = parserEmulationProfileClazz.getField(DEFAULT_EMULATION_PROFILE).get(null);
			final Object parserOptions = buildParserOptions(classLoader, parserClazz, dataHolderClazz, dataKeyClazz, parserEmulationProfile);
			final Object parserBuilder = parserClazz.getMethod("builder", dataHolderClazz).invoke(null, parserOptions);
			final Object parser = parserBuilderClazz.getMethod("build").invoke(parserBuilder);
			final Method parseMethod = parserClazz.getMethod("parse", String.class);

			// now we can create the formatter and find the render method
			final Class<?> formatterClazz = classLoader.loadClass("com.vladsch.flexmark.formatter.Formatter");
			final Class<?> nodeClazz = classLoader.loadClass("com.vladsch.flexmark.util.ast.Node");
			final Class<?> formatterBuilderClazz = classLoader.loadClass("com.vladsch.flexmark.formatter.Formatter$Builder");
			final Object formatterOptions = buildFormatterOptions(
					classLoader, parserClazz, formatterClazz, dataKeyClazz, dataHolderClazz, parserOptions, parserEmulationProfile);
			final Object formatterBuilder = formatterClazz.getMethod("builder", dataHolderClazz).invoke(null, formatterOptions);
			final Object formatter = formatterBuilderClazz.getMethod("build").invoke(formatterBuilder);
			final Method renderMethod = formatterClazz.getMethod("render", nodeClazz);

			// the input must be parsed by the parser and then rendered by the formatter
			return input -> (String) renderMethod.invoke(formatter, parseMethod.invoke(parser, input));
		}

		private Object buildParserOptions(
				ClassLoader classLoader,
				Class<?> parserClazz,
				Class<?> dataHolderClazz,
				Class<?> dataKeyClazz,
				Object parserEmulationProfile) throws Exception {
			final Class<?> pegdownOptionsAdapterClazz = classLoader.loadClass("com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter");
			final Class<?> pegdownExtensionsClazz = classLoader.loadClass("com.vladsch.flexmark.parser.PegdownExtensions");
			final Class<?> extensionClazz = classLoader.loadClass("com.vladsch.flexmark.util.misc.Extension");
			final Class<?> mutableDataHolderClazz = classLoader.loadClass("com.vladsch.flexmark.util.data.MutableDataHolder");

			final int pegDownExtensionsConstantAll = pegdownExtensionsClazz.getField("ALL").getInt(null);
			final Object extensions = Array.newInstance(extensionClazz, 0);
			final Class<?> extensionArrayClazz = extensions.getClass();

			final Object parserOptions = pegdownOptionsAdapterClazz
					.getMethod("flexmarkOptions", Integer.TYPE, extensionArrayClazz)
					.invoke(null, pegDownExtensionsConstantAll, extensions);
			final Object mutableParserOptions = dataHolderClazz.getMethod("toMutable").invoke(parserOptions);
			final Object parserEmulationProfileKey = parserClazz.getField("PARSER_EMULATION_PROFILE").get(null);
			final Method mutableDataHolderSetMethod = mutableDataHolderClazz.getMethod("set", dataKeyClazz, Object.class);
			mutableDataHolderSetMethod.invoke(mutableParserOptions, parserEmulationProfileKey, parserEmulationProfile);
			return mutableParserOptions;
		}

		/**
		 * Creates the formatter options, copies the parser extensions and changes defaults that make sense for a formatter.
		 * See: https://github.com/vsch/flexmark-java/wiki/Markdown-Formatter#options
		 */
		private Object buildFormatterOptions(
				ClassLoader classLoader,
				Class<?> parserClazz,
				Class<?> formatterClazz,
				Class<?> dataKeyClazz,
				Class<?> dataHolderClazz,
				Object parserOptions,
				Object parserEmulationProfile) throws Exception {
			final Class<?> mutableDataSetClazz = classLoader.loadClass("com.vladsch.flexmark.util.data.MutableDataSet");
			final Object formatterOptions = mutableDataSetClazz.getConstructor().newInstance();
			final Method mutableDataSetMethodSet = mutableDataSetClazz.getMethod("set", dataKeyClazz, Object.class);

			// copy the parser extensions like the example in https://github.com/vsch/flexmark-java/wiki/Markdown-Formatter
			final Object parserExtensions = parserClazz.getField("EXTENSIONS").get(null);
			final Object copiedExtensions = dataKeyClazz.getMethod("get", dataHolderClazz).invoke(parserExtensions, parserOptions);
			mutableDataSetMethodSet.invoke(formatterOptions, parserExtensions, copiedExtensions);

			// use the same emulation profile for the parser and the formatted, to make sure the step is idempotent
			final Object formatterEmulationProfile = formatterClazz.getField("FORMATTER_EMULATION_PROFILE").get(null);
			mutableDataSetMethodSet.invoke(formatterOptions, formatterEmulationProfile, parserEmulationProfile);

			return formatterOptions;
		}

	}
}
