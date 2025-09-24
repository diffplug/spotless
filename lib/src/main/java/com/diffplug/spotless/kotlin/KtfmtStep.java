/*
 * Copyright 2016-2025 DiffPlug
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
import static com.diffplug.spotless.kotlin.KtfmtStep.Style.DROPBOX;
import static com.diffplug.spotless.kotlin.KtfmtStep.Style.META;
import static com.diffplug.spotless.kotlin.KtfmtStep.TrailingCommaManagementStrategy.ONLY_ADD;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

/**
 * Wraps up <a href="https://github.com/facebook/ktfmt">ktfmt</a> as a FormatterStep.
 */
public class KtfmtStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_VERSION = "0.58";
	private static final String NAME = "ktfmt";
	private static final String MAVEN_COORDINATE = "com.facebook:ktfmt:";

	private final String version;
	/**
	 * Option that allows to apply formatting options to perform a 4-space block and continuation indent.
	 */
	@Nullable private final Style style;
	@Nullable private final KtfmtFormattingOptions options;
	/**
	 * The jar that contains the formatter.
	 */
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
	 * @see <a href="https://github.com/facebook/ktfmt/blob/v0.51/core/src/main/java/com/facebook/ktfmt/format/Formatter.kt#L45-L68">ktfmt source</a>
	 */
	public enum Style {
		// @formatter:off
		DEFAULT("DEFAULT_FORMAT", "0.0", "0.50"),
		META("META_FORMAT", "0.51"),
		DROPBOX("DROPBOX_FORMAT", "0.16", "0.50"),
		GOOGLE("GOOGLE_FORMAT", "0.19"),
		KOTLINLANG("KOTLINLANG_FORMAT", "0.21"),
		;
		// @formatter:on

		final private String format;
		final private String since;
		final private @Nullable String until;

		Style(String format, String since) {
			this.format = format;
			this.since = since;
			this.until = null;
		}

		Style(String format, String since, @Nullable String until) {
			this.format = format;
			this.since = since;
			this.until = until;
		}

		String getFormat() {
			return format;
		}

		String getSince() {
			return since;
		}

		/**
		 * Last version (inclusive) that supports this style
		 */
		@Nullable String getUntil() {
			return until;
		}
	}

	public enum TrailingCommaManagementStrategy {
		/**
		 * Do not manage trailing commas at all, only format what is already present.
		 */
		NONE,
		/**
		 * <p>
		 * Only add trailing commas when necessary, but do not remove them.
		 * </p>
		 * <p>
		 * Lists that cannot fit on one line will have trailing commas inserted.
		 * Trailing commas can to be used to "hint" ktfmt that the list should be broken to multiple lines
		 * </p>
		 */
		ONLY_ADD,
		/**
		 * <p>
		 * Fully manage trailing commas, adding and removing them where necessary.
		 * </p>
		 * <p>
		 * Lists that cannot fit on one line will have trailing commas inserted.
		 * Lists that span multiple lines will have them removed. Manually inserted trailing commas
		 * cannot be used as a hint to force breaking lists to multiple lines.
		 * </p>
		 */
		COMPLETE,
	}

	public static class KtfmtFormattingOptions implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		@Nullable private Integer maxWidth = null;

		@Nullable private Integer blockIndent = null;

		@Nullable private Integer continuationIndent = null;

		@Nullable private Boolean removeUnusedImports = null;

		@Nullable private TrailingCommaManagementStrategy trailingCommaManagementStrategy;

		public KtfmtFormattingOptions() {}

		public KtfmtFormattingOptions(
				@Nullable Integer maxWidth,
				@Nullable Integer blockIndent,
				@Nullable Integer continuationIndent,
				@Nullable Boolean removeUnusedImports,
				@Nullable TrailingCommaManagementStrategy trailingCommaManagementStrategy) {
			this.maxWidth = maxWidth;
			this.blockIndent = blockIndent;
			this.continuationIndent = continuationIndent;
			this.removeUnusedImports = removeUnusedImports;
			this.trailingCommaManagementStrategy = trailingCommaManagementStrategy;
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

		public void setRemoveUnusedImports(boolean removeUnusedImports) {
			this.removeUnusedImports = removeUnusedImports;
		}

		public void setTrailingCommaManagementStrategy(TrailingCommaManagementStrategy trailingCommaManagementStrategy) {
			this.trailingCommaManagementStrategy = trailingCommaManagementStrategy;
		}
	}

	/**
	 * Creates a step which formats everything - code, import order, and unused imports.
	 */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/**
	 * Creates a step which formats everything - code, import order, and unused imports.
	 */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, provisioner, null, null);
	}

	/**
	 * Creates a step which formats everything - code, import order, and unused imports.
	 */
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
		@Serial
		private static final long serialVersionUID = 1L;
		private final String version;
		@Nullable private final Style style;
		@Nullable private final KtfmtFormattingOptions options;
		private final JarState jarState;

		State(String version,
				JarState jarState,
				@Nullable Style style,
				@Nullable KtfmtFormattingOptions options) {
			this.version = version;
			this.options = options;
			this.style = style;
			this.jarState = jarState;
			validateStyle();
			validateOptions();
		}

		FormatterFunc createFormat() throws Exception {
			final ClassLoader classLoader = jarState.getClassLoader();

			if (BadSemver.version(version) < BadSemver.version(0, 51)) {
				return new KtfmtFormatterFuncCompat(version, style, options, classLoader).getFormatterFunc();
			}

			final Class<?> formatterFuncClass = classLoader.loadClass("com.diffplug.spotless.glue.ktfmt.KtfmtFormatterFunc");
			final Class<?> ktfmtStyleClass = classLoader.loadClass("com.diffplug.spotless.glue.ktfmt.KtfmtStyle");
			final Class<?> ktfmtFormattingOptionsClass = classLoader.loadClass("com.diffplug.spotless.glue.ktfmt.KtfmtFormattingOptions");
			final Class<?> ktfmtTrailingCommaManagmentStrategyClass = classLoader.loadClass("com.diffplug.spotless.glue.ktfmt.KtfmtTrailingCommaManagementStrategy");

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
					Integer.class, Integer.class, Integer.class, Boolean.class, ktfmtTrailingCommaManagmentStrategyClass);

			final Object ktfmtTrailingCommaManagementStrategy = options.trailingCommaManagementStrategy == null
					? null
					: Enum.valueOf((Class<? extends Enum>) ktfmtTrailingCommaManagmentStrategyClass, options.trailingCommaManagementStrategy.name());
			final Object ktfmtFormattingOptions = optionsConstructor.newInstance(
					options.maxWidth, options.blockIndent, options.continuationIndent, options.removeUnusedImports, ktfmtTrailingCommaManagementStrategy);
			if (style == null) {
				final Constructor<?> constructor = formatterFuncClass.getConstructor(ktfmtFormattingOptionsClass);
				return (FormatterFunc) constructor.newInstance(ktfmtFormattingOptions);
			}

			final Constructor<?> constructor = formatterFuncClass.getConstructor(ktfmtStyleClass, ktfmtFormattingOptionsClass);
			return (FormatterFunc) constructor.newInstance(ktfmtStyle, ktfmtFormattingOptions);
		}

		private void validateOptions() {
			if (BadSemver.version(version) < BadSemver.version(0, 11)) {
				if (options != null) {
					throw new IllegalStateException("Ktfmt formatting options supported for version 0.11 and later");
				}
				return;
			}

			if (BadSemver.version(version) < BadSemver.version(0, 17)) {
				if (options != null && options.removeUnusedImports != null) {
					throw new IllegalStateException("Ktfmt formatting option `removeUnusedImports` supported for version 0.17 and later");
				}
			}

			if (BadSemver.version(version) < BadSemver.version(0, 57)) {
				if (options != null && options.trailingCommaManagementStrategy == ONLY_ADD) {
					throw new IllegalStateException("Value ONLY_ADD for Ktfmt formatting option `trailingCommaManagementStrategy` supported for version 0.57 and later");
				}
			}
		}

		private void validateStyle() {
			if (style == null) {
				return;
			}

			if (BadSemver.version(version) < BadSemver.version(style.since)) {
				throw new IllegalStateException("The style %s is available from version %s (current version: %s)".formatted(style.name(), style.since, version));
			}
			if (style.until != null && BadSemver.version(version) > BadSemver.version(style.until)) {
				throw new IllegalStateException("The style %s is no longer available from version %s (current version: %s)".formatted(style.name(), style.until, version));
			}
		}

		/**
		 * @param style
		 * @return com.diffplug.spotless.glue.ktfmt.KtfmtStyle enum value name
		 */
		private String getKtfmtStyleOption(Style style) {
			switch (style) {
			case META:
				return "META";
			case GOOGLE:
				return "GOOGLE";
			case KOTLINLANG:
				return "KOTLIN_LANG";
			default:
				throw new IllegalStateException("Unsupported style: " + style);
			}
		}
	}

	private static final class KtfmtFormatterFuncCompat {
		private static final String PACKAGE = "com.facebook.ktfmt";

		/**
		 * The <code>format</code> method is available in the link below.
		 *
		 * @see <a href="https://github.com/facebook/ktfmt/blob/v0.51/core/src/main/java/com/facebook/ktfmt/format/Formatter.kt#L78-L94">ktfmt source</a>
		 */
		static final String FORMATTER_METHOD = "format";

		private final String version;
		private final Style style;
		private final KtfmtFormattingOptions options;
		private final ClassLoader classLoader;

		public KtfmtFormatterFuncCompat(String currentVersion, @Nullable Style style, @Nullable KtfmtFormattingOptions options, ClassLoader classLoader) {
			this.version = currentVersion;
			this.style = style;
			this.options = options;
			this.classLoader = classLoader;
		}

		public FormatterFunc getFormatterFunc() {
			return input -> {
				try {
					return applyFormat(input);
				} catch (InvocationTargetException e) {
					throw ThrowingEx.unwrapCause(e);
				}
			};
		}

		protected String applyFormat(String input) throws Exception {
			Class<?> formatterClass = getFormatterClazz();
			if (style == null && options == null || style == DEFAULT) {
				Method formatterMethod = formatterClass.getMethod(FORMATTER_METHOD, String.class);
				return (String) formatterMethod.invoke(formatterClass, input);
			} else {
				Method formatterMethod = formatterClass.getMethod(FORMATTER_METHOD, getFormattingOptionsClazz(), String.class);
				Object formattingOptions = getCustomFormattingOptions(formatterClass);
				return (String) formatterMethod.invoke(formatterClass, formattingOptions, input);
			}
		}

		private Object getCustomFormattingOptions(Class<?> formatterClass) throws Exception {
			Object formattingOptions = getFormattingOptionsFromStyle(formatterClass);
			Class<?> formattingOptionsClass = formattingOptions.getClass();

			if (options != null) {
				if (BadSemver.version(version) < BadSemver.version(0, 17)) {
					formattingOptions = formattingOptions.getClass().getConstructor(int.class, int.class, int.class).newInstance(
							/* maxWidth = */ Optional.ofNullable(options.maxWidth).orElse((Integer) formattingOptionsClass.getMethod("getMaxWidth").invoke(formattingOptions)),
							/* blockIndent = */ Optional.ofNullable(options.blockIndent).orElse((Integer) formattingOptionsClass.getMethod("getBlockIndent").invoke(formattingOptions)),
							/* continuationIndent = */ Optional.ofNullable(options.continuationIndent).orElse((Integer) formattingOptionsClass.getMethod("getContinuationIndent").invoke(formattingOptions)));
				} else if (BadSemver.version(version) < BadSemver.version(0, 19)) {
					formattingOptions = formattingOptions.getClass().getConstructor(int.class, int.class, int.class, boolean.class, boolean.class).newInstance(
							/* maxWidth = */ Optional.ofNullable(options.maxWidth).orElse((Integer) formattingOptionsClass.getMethod("getMaxWidth").invoke(formattingOptions)),
							/* blockIndent = */ Optional.ofNullable(options.blockIndent).orElse((Integer) formattingOptionsClass.getMethod("getBlockIndent").invoke(formattingOptions)),
							/* continuationIndent = */ Optional.ofNullable(options.continuationIndent).orElse((Integer) formattingOptionsClass.getMethod("getContinuationIndent").invoke(formattingOptions)),
							/* removeUnusedImports = */ Optional.ofNullable(options.removeUnusedImports).orElse((Boolean) formattingOptionsClass.getMethod("getRemoveUnusedImports").invoke(formattingOptions)),
							/* debuggingPrintOpsAfterFormatting = */ (Boolean) formattingOptionsClass.getMethod("getDebuggingPrintOpsAfterFormatting").invoke(formattingOptions));
				} else if (BadSemver.version(version) < BadSemver.version(0, 47)) {
					Class<?> styleClass = classLoader.loadClass(formattingOptionsClass.getName() + "$Style");
					formattingOptions = formattingOptions.getClass().getConstructor(styleClass, int.class, int.class, int.class, boolean.class, boolean.class).newInstance(
							/* style = */ formattingOptionsClass.getMethod("getStyle").invoke(formattingOptions),
							/* maxWidth = */ Optional.ofNullable(options.maxWidth).orElse((Integer) formattingOptionsClass.getMethod("getMaxWidth").invoke(formattingOptions)),
							/* blockIndent = */ Optional.ofNullable(options.blockIndent).orElse((Integer) formattingOptionsClass.getMethod("getBlockIndent").invoke(formattingOptions)),
							/* continuationIndent = */ Optional.ofNullable(options.continuationIndent).orElse((Integer) formattingOptionsClass.getMethod("getContinuationIndent").invoke(formattingOptions)),
							/* removeUnusedImports = */ Optional.ofNullable(options.removeUnusedImports).orElse((Boolean) formattingOptionsClass.getMethod("getRemoveUnusedImports").invoke(formattingOptions)),
							/* debuggingPrintOpsAfterFormatting = */ (Boolean) formattingOptionsClass.getMethod("getDebuggingPrintOpsAfterFormatting").invoke(formattingOptions));
				} else if (BadSemver.version(version) < BadSemver.version(0, 57)) {
					Class<?> styleClass = classLoader.loadClass(formattingOptionsClass.getName() + "$Style");
					formattingOptions = formattingOptions.getClass().getConstructor(styleClass, int.class, int.class, int.class, boolean.class, boolean.class, boolean.class).newInstance(
							/* style = */ formattingOptionsClass.getMethod("getStyle").invoke(formattingOptions),
							/* maxWidth = */ Optional.ofNullable(options.maxWidth).orElse((Integer) formattingOptionsClass.getMethod("getMaxWidth").invoke(formattingOptions)),
							/* blockIndent = */ Optional.ofNullable(options.blockIndent).orElse((Integer) formattingOptionsClass.getMethod("getBlockIndent").invoke(formattingOptions)),
							/* continuationIndent = */ Optional.ofNullable(options.continuationIndent).orElse((Integer) formattingOptionsClass.getMethod("getContinuationIndent").invoke(formattingOptions)),
							/* removeUnusedImports = */ Optional.ofNullable(options.removeUnusedImports).orElse((Boolean) formattingOptionsClass.getMethod("getRemoveUnusedImports").invoke(formattingOptions)),
							/* debuggingPrintOpsAfterFormatting = */ (Boolean) formattingOptionsClass.getMethod("getDebuggingPrintOpsAfterFormatting").invoke(formattingOptions),
							/* manageTrailingCommas = */ Optional.ofNullable(getManageTrailingCommasFrom(options.trailingCommaManagementStrategy)).orElse((Boolean) formattingOptionsClass.getMethod("getManageTrailingCommas").invoke(formattingOptions)));
				} else {
					Class<?> styleClass = classLoader.loadClass(formattingOptionsClass.getName() + "$Style");
					formattingOptions = formattingOptions.getClass().getConstructor(styleClass, int.class, int.class, int.class, boolean.class, boolean.class, TrailingCommaManagementStrategy.class).newInstance(
							/* style = */ formattingOptionsClass.getMethod("getStyle").invoke(formattingOptions),
							/* maxWidth = */ Optional.ofNullable(options.maxWidth).orElse((Integer) formattingOptionsClass.getMethod("getMaxWidth").invoke(formattingOptions)),
							/* blockIndent = */ Optional.ofNullable(options.blockIndent).orElse((Integer) formattingOptionsClass.getMethod("getBlockIndent").invoke(formattingOptions)),
							/* continuationIndent = */ Optional.ofNullable(options.continuationIndent).orElse((Integer) formattingOptionsClass.getMethod("getContinuationIndent").invoke(formattingOptions)),
							/* removeUnusedImports = */ Optional.ofNullable(options.removeUnusedImports).orElse((Boolean) formattingOptionsClass.getMethod("getRemoveUnusedImports").invoke(formattingOptions)),
							/* debuggingPrintOpsAfterFormatting = */ (Boolean) formattingOptionsClass.getMethod("getDebuggingPrintOpsAfterFormatting").invoke(formattingOptions),
							/* trailingCommaManagementStrategy */ Optional.ofNullable(options.trailingCommaManagementStrategy).orElse((TrailingCommaManagementStrategy) formattingOptionsClass.getMethod("getTrailingCommaManagementStrategy").invoke(formattingOptions)));
				}
			}

			return formattingOptions;
		}

		private Object getFormattingOptionsFromStyle(Class<?> formatterClass) throws Exception {
			Style style = this.style;
			if (style == null) {
				if (BadSemver.version(version) < BadSemver.version(0, 51)) {
					style = DEFAULT;
				} else {
					style = META;
				}
			}
			if (BadSemver.version(version) < BadSemver.version(0, 19)) {
				if (style != DROPBOX) {
					throw new IllegalStateException("Invalid style " + style + " for version " + version);
				}
				Class<?> formattingOptionsCompanionClazz = classLoader.loadClass(PACKAGE + ".FormattingOptions$Companion");
				Object companion = formattingOptionsCompanionClazz.getConstructors()[0].newInstance((Object) null);
				Method formattingOptionsMethod = formattingOptionsCompanionClazz.getDeclaredMethod("dropboxStyle");
				return formattingOptionsMethod.invoke(companion);
			} else {
				return formatterClass.getField(style.getFormat()).get(null);
			}
		}

		private Class<?> getFormatterClazz() throws Exception {
			Class<?> formatterClazz;
			if (BadSemver.version(version) >= BadSemver.version(0, 31)) {
				formatterClazz = classLoader.loadClass(PACKAGE + ".format.Formatter");
			} else {
				formatterClazz = classLoader.loadClass(PACKAGE + ".FormatterKt");
			}
			return formatterClazz;
		}

		private Class<?> getFormattingOptionsClazz() throws Exception {
			Class<?> formattingOptionsClazz;
			if (BadSemver.version(version) >= BadSemver.version(0, 31)) {
				formattingOptionsClazz = classLoader.loadClass(PACKAGE + ".format.FormattingOptions");
			} else {
				formattingOptionsClazz = classLoader.loadClass(PACKAGE + ".FormattingOptions");
			}
			return formattingOptionsClazz;
		}

		private @Nullable Boolean getManageTrailingCommasFrom(
			@Nullable TrailingCommaManagementStrategy trailingCommaManagementStrategy
		) {
			if (trailingCommaManagementStrategy == null) {
				return null;
			}

			return switch (trailingCommaManagementStrategy) {
				case NONE, ONLY_ADD -> false;
				case COMPLETE -> true;
			};
		}
	}
}
