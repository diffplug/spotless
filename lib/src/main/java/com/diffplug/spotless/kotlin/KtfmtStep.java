/*
 * Copyright 2016-2020 DiffPlug
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
package com.diffplug.spotless.kotlin;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import com.diffplug.spotless.*;

/** Wraps up [ktfmt](https://github.com/facebookincubator/ktfmt) as a FormatterStep. */
public class KtfmtStep {
	// prevent direct instantiation
	private KtfmtStep() {}

	private static final String DEFAULT_VERSION = "0.15";
	static final String NAME = "ktfmt";
	static final String PACKAGE = "com.facebook";
	static final String MAVEN_COORDINATE = PACKAGE + ":ktfmt:";

	/**
	 * Used to allow drpobox style option through formatting options.
	 *
	 * @see: https://github.com/facebookincubator/ktfmt/blob/master/core/src/main/java/com/facebook/ktfmt/Formatter.kt#L47-L73
	 */
	private static final int MAX_WIDTH_LINE = 100;
	private static final int BLOCK_INDENT = 4;
	private static final int CONTINUATION_INDENT = 4;

	/**
	 * The <code>format</code> method is available in the link below.
	 *
	 * @see:
	 *     https://github.com/facebookincubator/ktfmt/blob/master/core/src/main/java/com/facebook/ktfmt/Formatter.kt#L79-L92
	 */
	static final String FORMATTER_METHOD = "format";

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, provisioner, false);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, Provisioner provisioner, Boolean withDropboxStyle) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		Objects.requireNonNull(withDropboxStyle, "withDropboxStyle");
		return FormatterStep.createLazy(
				NAME, () -> new State(version, provisioner, withDropboxStyle), State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String pkg;
		/**
		 * Option that allows to apply formatting options to perform a 4 spaces block and continuation indent.
		 */
		private final Boolean withDropboxStyle;
		/** The jar that contains the eclipse formatter. */
		final JarState jarState;

		State(String version, Provisioner provisioner, Boolean withDropboxStyle) throws IOException {
			this.pkg = PACKAGE;
			this.withDropboxStyle = withDropboxStyle;
			this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
		}

		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();
			Class<?> formatterClazz = classLoader.loadClass(pkg + ".ktfmt.FormatterKt");
			return input -> {
				try {
					if (withDropboxStyle) {
						Class<?> formattingOptionsClazz = classLoader.loadClass(pkg + ".ktfmt.FormattingOptions");
						Object formattingOptions = formattingOptionsClazz.getConstructor(
								int.class, int.class, int.class).newInstance(
										MAX_WIDTH_LINE, BLOCK_INDENT, CONTINUATION_INDENT);
						Method formatterMethod = formatterClazz.getMethod(FORMATTER_METHOD, formattingOptionsClazz,
								String.class);
						return (String) formatterMethod.invoke(formatterClazz, formattingOptions, input);
					} else {
						Method formatterMethod = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
						return (String) formatterMethod.invoke(formatterClazz, input);
					}
				} catch (InvocationTargetException e) {
					throw ThrowingEx.unwrapCause(e);
				}
			};
		}
	}
}
