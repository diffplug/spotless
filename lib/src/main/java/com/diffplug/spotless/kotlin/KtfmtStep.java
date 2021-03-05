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
package com.diffplug.spotless.kotlin;

import static com.diffplug.spotless.kotlin.KtfmtStep.Style.DEFAULT;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import com.diffplug.spotless.*;

/**
 * Wraps up [ktfmt](https://github.com/facebookincubator/ktfmt) as a FormatterStep.
 */
public class KtfmtStep {
	// prevent direct instantiation
	private KtfmtStep() {}

	private static final String DEFAULT_VERSION = "0.21";
	static final String NAME = "ktfmt";
	static final String PACKAGE = "com.facebook";
	static final String MAVEN_COORDINATE = PACKAGE + ":ktfmt:";

	/**
	 * Used to allow multiple style option through formatting options and since when is each of them available.
	 *
	 * @see <a href="https://github.com/facebookincubator/ktfmt/blob/38486b0fb2edcabeba5540fcb69c6f1fa336c331/core/src/main/java/com/facebook/ktfmt/Formatter.kt#L47-L80">ktfmt source</a>
	 */
	public enum Style {
		DEFAULT("DEFAULT_FORMAT", "0.0"), DROPBOX("DROPBOX_FORMAT", "0.11"), GOOGLE("GOOGLE_FORMAT", "0.21"), KOTLINLANG("KOTLINLANG_FORMAT", "0.21");

		final private String format;
		final private String since;

		Style(String format, String since) {
			this.format = format;
			this.since = since;
		}

		String getFormat() {
			return format;
		}

		String getSince() {
			return since;
		}
	}

	private static final String DROPBOX_STYLE_METHOD = "dropboxStyle";

	/**
	 * The <code>format</code> method is available in the link below.
	 *
	 * @see <a href="https://github.com/facebookincubator/ktfmt/blob/38486b0fb2edcabeba5540fcb69c6f1fa336c331/core/src/main/java/com/facebook/ktfmt/Formatter.kt#L82-L99">ktfmt source</a>
	 */
	static final String FORMATTER_METHOD = "format";

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, provisioner, DEFAULT);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, Provisioner provisioner, Style style) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		Objects.requireNonNull(style, "style");
		return FormatterStep.createLazy(
				NAME, () -> new State(version, provisioner, style), State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	public static String defaultStyle() {
		return Style.DEFAULT.name();
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String version;

		private final String pkg;
		/**
		 * Option that allows to apply formatting options to perform a 4 spaces block and continuation indent.
		 */
		private final Style style;
		/** The jar that contains the eclipse formatter. */
		final JarState jarState;

		State(String version, Provisioner provisioner, Style style) throws IOException {
			this.version = version;
			this.pkg = PACKAGE;
			this.style = style;
			this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
		}

		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();
			Class<?> formatterClazz = classLoader.loadClass(pkg + ".ktfmt.FormatterKt");
			return input -> {
				try {
					if (style == DEFAULT) {
						Method formatterMethod = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
						return (String) formatterMethod.invoke(formatterClazz, input);
					} else {
						Class<?> formattingOptionsClazz = classLoader.loadClass(pkg + ".ktfmt.FormattingOptions");
						Method formatterMethod = formatterClazz.getMethod(FORMATTER_METHOD, formattingOptionsClazz,
								String.class);
						Object formattingOptions = getCustomFormattingOptions(classLoader, style);
						return (String) formatterMethod.invoke(formatterClazz, formattingOptions, input);
					}
				} catch (InvocationTargetException e) {
					throw ThrowingEx.unwrapCause(e);
				}
			};
		}

		private Object getCustomFormattingOptions(ClassLoader classLoader, Style style) throws Exception {
			if (BadSemver.version(version) < BadSemver.version(style.since)) {
				throw new IllegalStateException(String.format("The style %s is available from version %s (current version: %s)", style.name(), style.since, version));
			}

			try {
				// ktfmt v0.19 and later
				return classLoader.loadClass(pkg + ".ktfmt.FormatterKt").getField(style.getFormat()).get(null);
			} catch (NoSuchFieldException ignored) {}

			// fallback to old, pre-0.19 ktfmt interface.
			if (style == Style.DEFAULT || style == Style.DROPBOX) {
				Class<?> formattingOptionsCompanionClazz = classLoader.loadClass(pkg + ".ktfmt.FormattingOptions$Companion");
				Object companion = formattingOptionsCompanionClazz.getConstructors()[0].newInstance((Object) null);
				Method formattingOptionsMethod = formattingOptionsCompanionClazz.getDeclaredMethod(DROPBOX_STYLE_METHOD);
				return formattingOptionsMethod.invoke(companion);
			} else {
				throw new IllegalStateException("Versions pre-0.19 can only use Default and Dropbox styles");
			}
		}
	}
}
