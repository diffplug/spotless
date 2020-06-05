/*
 * Copyright 2020 DiffPlug
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

	private static final String DEFAULT_VERSION = "0.13";
	static final String NAME = "ktfmt";
	static final String PACKAGE = "com.facebook";
	static final String MAVEN_COORDINATE = PACKAGE + ":ktfmt:";

	/**
	 * The <code>format</code> method is available in the link below.
	 *
	 * @see:
	 *     https://github.com/facebookincubator/ktfmt/blob/master/core/src/main/java/com/facebook/ktfmt/Formatter.kt#L61-L65
	 */
	static final String FORMATTER_METHOD = "format";

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(
				NAME, () -> new State(version, provisioner), State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String pkg;
		/** The jar that contains the eclipse formatter. */
		final JarState jarState;

		State(String version, Provisioner provisioner) throws IOException {
			this.pkg = PACKAGE;
			this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
		}

		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();

			Class<?> formatterClazz = classLoader.loadClass(pkg + ".ktfmt.FormatterKt");
			Method formatterMethod = formatterClazz.getMethod(FORMATTER_METHOD, String.class);

			return input -> {
				try {
					return (String) formatterMethod.invoke(formatterClazz, input);
				} catch (InvocationTargetException e) {
					throw ThrowingEx.unwrapCause(e);
				}
			};
		}
	}
}
