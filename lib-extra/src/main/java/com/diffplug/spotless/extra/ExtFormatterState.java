/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless.extra;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Properties;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterProperties;
import com.diffplug.spotless.JarState;

/**
 * Creates optional formatter
 *
 * Serializes full state of the formatter to support Gradle incremental build.
 */
public final class ExtFormatterState implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_FORMATTER_METHOD = "format";

	private final JarState jarState;
	private final FileSignature configFileSignature;
	private final String className;
	private String formatterMethod;

	private ExtFormatterState(
			final JarState jarState,
			final String className,
			final FileSignature configFileSignature) {
		this.jarState = jarState;
		this.configFileSignature = configFileSignature;
		this.className = className;
		this.formatterMethod = DEFAULT_FORMATTER_METHOD;
	}

	/** Create lazy formatter creator function initializing formatter based on property settings. */
	public FormatterFunc basedOnProperties() throws Exception {
		FormatterProperties formaterSetting = FormatterProperties.from(configFileSignature.files());
		Properties settingProperties = formaterSetting.getProperties();

		ClassLoader classLoader = jarState.getClassLoader();

		// instantiate the formatter and get its format getClassName()
		Class<?> formatterClass = classLoader.loadClass(className);
		Object formatter = formatterClass.getConstructor(Properties.class).newInstance(settingProperties);
		Method method = formatterClass.getMethod(formatterMethod, String.class);
		return input -> (String) method.invoke(formatter, input);
	}

	/**
	 * State initialization
	 * @param jarState State of JARs containing the formatter implementation and its dependencies.
	 * @param className Full qualified class name of the formatter
	 * @param configFiles Files containing configuration
	 * @return state formatter state
	 * @throws Exception All exceptions are passed to the Gradle framework.
	 */
	public static ExtFormatterState from(
			final JarState jarState,
			final String className,
			final Iterable<File> configFiles) throws Exception {
		Objects.requireNonNull(jarState);
		Objects.requireNonNull(className);
		return new ExtFormatterState(jarState, className, FileSignature.signAsList(configFiles));
	}

}
