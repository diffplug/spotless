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
package com.diffplug.spotless.asciidoc;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

public final class AdocfmtStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_VERSION = "0.2.0";
	private static final String NAME = "adocfmt";
	private static final String MAVEN_COORDINATE = "org.drjekyll:adocfmt:";

	private final String version;
	private final AdocfmtConfig config;
	private final JarState.Promised jarState;

	private AdocfmtStep(String version, AdocfmtConfig config, JarState.Promised jarState) {
		this.version = version;
		this.config = config;
		this.jarState = jarState;
	}

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, provisioner, new AdocfmtConfig());
	}

	public static FormatterStep create(String version, Provisioner provisioner, AdocfmtConfig config) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		Objects.requireNonNull(config, "config");
		return FormatterStep.create(NAME,
				new AdocfmtStep(version, config, JarState.promise(() -> JarState.from(MAVEN_COORDINATE + version, provisioner))),
				AdocfmtStep::equalityState,
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private State equalityState() {
		return new State(version, config, jarState.get());
	}

	private static final class State implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;
		private final String version;
		private final AdocfmtConfig config;
		private final JarState jarState;

		State(String version, AdocfmtConfig config, JarState jarState) {
			this.version = version;
			this.config = config;
			this.jarState = jarState;
		}

		FormatterFunc createFormat() throws Exception {
			final ClassLoader classLoader = jarState.getClassLoader();
			final Class<?> formatterClass = classLoader.loadClass("org.drjekyll.adocfmt.AsciidocFormatter");
			final Class<?> configClass = classLoader.loadClass("org.drjekyll.adocfmt.AsciidocFormatterConfig");
			final Method builderMethod = configClass.getMethod("builder");
			Object builder = builderMethod.invoke(null);
			final Class<?> builderClass = builder.getClass();
			builder = builderClass.getMethod("normalizeSetextHeadings", boolean.class).invoke(builder, config.normalizeSetextHeadings);
			builder = builderClass.getMethod("collapseConsecutiveBlankLines", boolean.class).invoke(builder, config.collapseConsecutiveBlankLines);
			builder = builderClass.getMethod("oneSentencePerLine", boolean.class).invoke(builder, config.oneSentencePerLine);
			builder = builderClass.getMethod("normalizeBlockDelimiters", boolean.class).invoke(builder, config.normalizeBlockDelimiters);
			builder = builderClass.getMethod("removeTrailingHeaderEqualsSign", boolean.class).invoke(builder, config.removeTrailingHeaderEqualsSign);
			builder = builderClass.getMethod("titleCase", boolean.class).invoke(builder, config.titleCase);
			builder = builderClass.getMethod("removeTrailingWhitespace", boolean.class).invoke(builder, config.removeTrailingWhitespace);
			builder = builderClass.getMethod("normalizeListBullets", boolean.class).invoke(builder, config.normalizeListBullets);
			builder = builderClass.getMethod("normalizeOrderedListMarkers", boolean.class).invoke(builder, config.normalizeOrderedListMarkers);
			builder = builderClass.getMethod("ensureHeadingBlankLines", boolean.class).invoke(builder, config.ensureHeadingBlankLines);
			builder = builderClass.getMethod("ensureSourceDelimiters", boolean.class).invoke(builder, config.ensureSourceDelimiters);
			final Object adocfmtConfig = builderClass.getMethod("build").invoke(builder);

			final Object formatter = formatterClass.getConstructor(configClass).newInstance(adocfmtConfig);
			final Method formatMethod = formatterClass.getMethod("format", String.class);
			return input -> {
				try {
					return (String) formatMethod.invoke(formatter, input);
				} catch (InvocationTargetException e) {
					throw ThrowingEx.unwrapCause(e);
				}
			};
		}
	}
}
