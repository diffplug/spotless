/*
 * Copyright 2016-2024 DiffPlug
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

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

/**
 * Wraps up <a href="https://github.com/facebookincubator/ktfmt">ktfmt</a> as a FormatterStep.
 */
public class KtfmtStep implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_VERSION = "0.47";
	private static final String NAME = "ktfmt";
	private static final String MAVEN_COORDINATE = "com.facebook:ktfmt:";

	private final String version;
	/**
	 * Option that allows to apply formatting options to perform a 4-space block and continuation indent.
	 */
	@Nullable
	private final Style style;
	@Nullable
	private final KtfmtFormattingOptions options;
	/** The jar that contains the formatter. */
	private final JarState.Promised jarState;

	private KtfmtStep(String version,
			JarState.Promised jarState,
			@Nullable Style style,
			@Nullable KtfmtFormattingOptions options) {
		this.version = Objects.requireNonNull(version, "version");
		this.style = style;
		this.options = options;
		this.jarState = Objects.requireNonNull(jarState, "jarState");
	}

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

	public static class KtfmtFormattingOptions implements Serializable {

		private static final long serialVersionUID = 1L;

		@Nullable
		private Integer maxWidth = null;

		@Nullable
		private Integer blockIndent = null;

		@Nullable
		private Integer continuationIndent = null;

		@Nullable
		private Boolean removeUnusedImport = null;

		public KtfmtFormattingOptions() {}

		public KtfmtFormattingOptions(
				@Nullable Integer maxWidth,
				@Nullable Integer blockIndent,
				@Nullable Integer continuationIndent,
				@Nullable Boolean removeUnusedImport) {
			this.maxWidth = maxWidth;
			this.blockIndent = blockIndent;
			this.continuationIndent = continuationIndent;
			this.removeUnusedImport = removeUnusedImport;
		}

		public void setMaxWidth(int maxWidth) {
			this.maxWidth = maxWidth;
		}

		public void setBlockIndent(int blockIndent) {
			this.blockIndent = blockIndent;
		}

		public void setContinuationIndent(int continuationIndent) {
			this.continuationIndent = continuationIndent;
		}

		public void setRemoveUnusedImport(boolean removeUnusedImport) {
			this.removeUnusedImport = removeUnusedImport;
		}
	}

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
		return create(version, provisioner, null, null);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, Provisioner provisioner, @Nullable Style style, @Nullable KtfmtFormattingOptions options) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.create(NAME,
				new KtfmtStep(version, JarState.promise(() -> JarState.from(MAVEN_COORDINATE + version, provisioner)), style, options),
				KtfmtStep::equalityState,
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private State equalityState() {
		return new State(version, jarState.get(), style, options);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;
		private static final String PACKAGE = "com.facebook.ktfmt";
		private final String version;
		@Nullable
		private final Style style;
		@Nullable
		private final KtfmtFormattingOptions options;
		private final JarState jarState;

		State(String version,
				JarState jarState,
				@Nullable Style style,
				@Nullable KtfmtFormattingOptions options) {
			this.version = version;
			this.options = options;
			this.style = style;
			this.jarState = jarState;
		}

		FormatterFunc createFormat() throws Exception {
			final ClassLoader classLoader = jarState.getClassLoader();

			if (BadSemver.version(version) < BadSemver.version(0, 32)) {
				if (options != null) {
					throw new IllegalStateException("Ktfmt formatting options supported for version 0.32 and later");
				}
				return getFormatterFuncFallback(style != null ? style : DEFAULT, classLoader);
			}

			final Class<?> formatterFuncClass = classLoader.loadClass("com.diffplug.spotless.glue.ktfmt.KtfmtFormatterFunc");
			final Class<?> ktfmtStyleClass = classLoader.loadClass("com.diffplug.spotless.glue.ktfmt.KtfmtStyle");
			final Class<?> ktfmtFormattingOptionsClass = classLoader.loadClass("com.diffplug.spotless.glue.ktfmt.KtfmtFormattingOptions");

			if (style == null && options == null) {
				final Constructor<?> constructor = formatterFuncClass.getConstructor();
				return (FormatterFunc) constructor.newInstance();
			}

			final Object ktfmtStyle = style == null ? null : Enum.valueOf((Class<? extends Enum>) ktfmtStyleClass, getKtfmtStyleOption(style));
			if (options == null) {
				final Constructor<?> constructor = formatterFuncClass.getConstructor(ktfmtStyleClass);
				return (FormatterFunc) constructor.newInstance(ktfmtStyle);
			}

			final Constructor<?> optionsConstructor = ktfmtFormattingOptionsClass.getConstructor(
					Integer.class, Integer.class, Integer.class, Boolean.class);
			final Object ktfmtFormattingOptions = optionsConstructor.newInstance(
					options.maxWidth, options.blockIndent, options.continuationIndent, options.removeUnusedImport);
			if (style == null) {
				final Constructor<?> constructor = formatterFuncClass.getConstructor(ktfmtFormattingOptionsClass);
				return (FormatterFunc) constructor.newInstance(ktfmtFormattingOptions);
			}

			final Constructor<?> constructor = formatterFuncClass.getConstructor(ktfmtStyleClass, ktfmtFormattingOptionsClass);
			return (FormatterFunc) constructor.newInstance(ktfmtStyle, ktfmtFormattingOptions);
		}

		/**
		 * @param style
		 * @return com.diffplug.spotless.glue.ktfmt.KtfmtStyle enum value name
		 */
		private String getKtfmtStyleOption(Style style) {
			switch (style) {
			case DEFAULT:
				return "DEFAULT";
			case DROPBOX:
				return "DROPBOX";
			case GOOGLE:
				return "GOOGLE";
			case KOTLINLANG:
				return "KOTLIN_LANG";
			default:
				throw new IllegalStateException("Unsupported style: " + style);
			}
		}

		private FormatterFunc getFormatterFuncFallback(Style style, ClassLoader classLoader) {
			return input -> {
				try {
					if (style == DEFAULT) {
						Method formatterMethod = getFormatterClazz(classLoader).getMethod(FORMATTER_METHOD, String.class);
						return (String) formatterMethod.invoke(getFormatterClazz(classLoader), input);
					} else {
						Method formatterMethod = getFormatterClazz(classLoader).getMethod(FORMATTER_METHOD,
								getFormattingOptionsClazz(classLoader),
								String.class);
						Object formattingOptions = getCustomFormattingOptions(classLoader, style);
						return (String) formatterMethod.invoke(getFormatterClazz(classLoader), formattingOptions, input);
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
				return getFormatterClazz(classLoader).getField(style.getFormat()).get(null);
			} catch (NoSuchFieldException ignored) {}

			// fallback to old, pre-0.19 ktfmt interface.
			if (style == Style.DEFAULT || style == Style.DROPBOX) {
				Class<?> formattingOptionsCompanionClazz = classLoader.loadClass(PACKAGE + ".FormattingOptions$Companion");
				Object companion = formattingOptionsCompanionClazz.getConstructors()[0].newInstance((Object) null);
				Method formattingOptionsMethod = formattingOptionsCompanionClazz.getDeclaredMethod("dropboxStyle");
				return formattingOptionsMethod.invoke(companion);
			} else {
				throw new IllegalStateException("Versions pre-0.19 can only use Default and Dropbox styles");
			}
		}

		private Class<?> getFormatterClazz(ClassLoader classLoader) throws Exception {
			Class<?> formatterClazz;
			if (BadSemver.version(version) >= BadSemver.version(0, 31)) {
				formatterClazz = classLoader.loadClass(PACKAGE + ".format.Formatter");
			} else {
				formatterClazz = classLoader.loadClass(PACKAGE + ".FormatterKt");
			}
			return formatterClazz;
		}

		private Class<?> getFormattingOptionsClazz(ClassLoader classLoader) throws Exception {
			Class<?> formattingOptionsClazz;
			if (BadSemver.version(version) >= BadSemver.version(0, 31)) {
				formattingOptionsClazz = classLoader.loadClass(PACKAGE + ".format.FormattingOptions");
			} else {
				formattingOptionsClazz = classLoader.loadClass(PACKAGE + ".FormattingOptions");
			}
			return formattingOptionsClazz;
		}
	}
}
