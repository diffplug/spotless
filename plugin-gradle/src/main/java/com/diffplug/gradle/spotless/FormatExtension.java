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
package com.diffplug.gradle.spotless;

import static com.diffplug.gradle.spotless.PluginGradlePreconditions.requireElementsNonNull;
import static com.diffplug.gradle.spotless.SpotlessPluginRedirect.badSemver;
import static com.diffplug.gradle.spotless.SpotlessPluginRedirect.badSemverOfGradle;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.util.GradleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.common.base.Preconditions;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LazyForwardingEquality;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.LintSuppression;
import com.diffplug.spotless.OnMatch;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.SerializedFunction;
import com.diffplug.spotless.biome.BiomeFlavor;
import com.diffplug.spotless.cpp.ClangFormatStep;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep;
import com.diffplug.spotless.generic.EndWithNewlineStep;
import com.diffplug.spotless.generic.FenceStep;
import com.diffplug.spotless.generic.IndentStep;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.generic.LicenseHeaderStep.YearMode;
import com.diffplug.spotless.generic.NativeCmdStep;
import com.diffplug.spotless.generic.ReplaceRegexStep;
import com.diffplug.spotless.generic.ReplaceStep;
import com.diffplug.spotless.generic.TrimTrailingWhitespaceStep;
import com.diffplug.spotless.npm.NpmPathResolver;
import com.diffplug.spotless.npm.PrettierFormatterStep;

import groovy.lang.Closure;

/** Adds a {@code spotless{Name}Check} and {@code spotless{Name}Apply} task. */
public class FormatExtension {

	private static final Logger logger = LoggerFactory.getLogger(FormatExtension.class);

	final SpotlessExtension spotless;
	final List<Action<FormatExtension>> lazyActions = new ArrayList<>();

	@Inject
	public FormatExtension(SpotlessExtension spotless) {
		this.spotless = requireNonNull(spotless);
	}

	protected final Provisioner provisioner() {
		return spotless.getRegisterDependenciesTask().getTaskService().get().provisionerFor(spotless);
	}

	private String formatName() {
		for (Map.Entry<String, FormatExtension> entry : spotless.formats.entrySet()) {
			if (entry.getValue() == this) {
				return entry.getKey();
			}
		}
		throw new IllegalStateException("This format is not contained by any SpotlessExtension.");
	}

	LineEnding lineEndings;

	/**
	 * Returns the line endings to use (defaults to
	 * {@link SpotlessExtensionImpl#getLineEndings()}.
	 */
	public LineEnding getLineEndings() {
		return lineEndings == null ? spotless.getLineEndings() : lineEndings;
	}

	/**
	 * Sets the line endings to use (defaults to
	 * {@link SpotlessExtensionImpl#getLineEndings()}.
	 */
	public void setLineEndings(LineEnding lineEndings) {
		this.lineEndings = requireNonNull(lineEndings);
	}

	Charset encoding;

	/**
	 * Returns the encoding to use (defaults to
	 * {@link SpotlessExtensionImpl#getEncoding()}.
	 */
	public Charset getEncoding() {
		return encoding == null ? spotless.getEncoding() : encoding;
	}

	/**
	 * Sets the encoding to use (defaults to
	 * {@link SpotlessExtensionImpl#getEncoding()}.
	 */
	public void setEncoding(String name) {
		setEncoding(Charset.forName(requireNonNull(name)));
	}

	/**
	 * Sentinel to distinguish between "don't ratchet this format" and "use spotless
	 * parent format".
	 */
	private static final String RATCHETFROM_NOT_SET_AT_FORMAT_LEVEL = " not set at format level ";

	private String ratchetFrom = RATCHETFROM_NOT_SET_AT_FORMAT_LEVEL;

	/** @see #setRatchetFrom(String) */
	public String getRatchetFrom() {
		return ratchetFrom == RATCHETFROM_NOT_SET_AT_FORMAT_LEVEL ? spotless.getRatchetFrom() : ratchetFrom;
	}

	/**
	 * Allows you to override the value from the parent
	 * {@link SpotlessExtension#setRatchetFrom(String)} for this specific format.
	 */
	public void setRatchetFrom(String ratchetFrom) {
		this.ratchetFrom = ratchetFrom;
	}

	/** @see #setRatchetFrom(String) */
	public void ratchetFrom(String ratchetFrom) {
		setRatchetFrom(ratchetFrom);
	}

	/**
	 * Sets the encoding to use (defaults to
	 * {@link SpotlessExtensionImpl#getEncoding()}.
	 */
	public void setEncoding(Charset charset) {
		encoding = requireNonNull(charset);
	}

	final List<LintSuppression> lintSuppressions = new ArrayList<>();

	/** Suppresses any lints which meet the supplied criteria. */
	public void suppressLintsFor(Action<LintSuppression> lintSuppression) {
		LintSuppression suppression = new LintSuppression();
		lintSuppression.execute(suppression);
		suppression.ensureDoesNotSuppressAll();
		lintSuppressions.add(suppression);
	}

	/**
	 * Ignores errors in the given step.
	 *
	 * @deprecated Use {@link #suppressLintsFor(Action)} instead.
	 */
	@Deprecated
	public void ignoreErrorForStep(String stepName) {
		System.err.println("`ignoreErrorForStep('" + stepName + "') is deprecated, use `suppressLintsFor { step = '" + stepName + "' }` instead.");
		suppressLintsFor(it -> it.setStep(stepName));
	}

	/**
	 * Ignores errors for the given relative path.
	 *
	 * @deprecated Use {@link #suppressLintsFor(Action)} instead.
	 */
	@Deprecated
	public void ignoreErrorForPath(String relativePath) {
		System.err.println("`ignoreErrorForPath('" + relativePath + "') is deprecated, use `suppressLintsFor { path = '" + relativePath + "' }` instead.");
		suppressLintsFor(it -> it.setPath(relativePath));
	}

	/**
	 * Sets encoding to use (defaults to
	 * {@link SpotlessExtensionImpl#getEncoding()}).
	 */
	public void encoding(String charset) {
		setEncoding(charset);
	}

	/** The files to be formatted = (target - targetExclude). */
	protected FileCollection target, targetExclude;

	/** The value from which files will be excluded if their content contain it. */
	@Nullable
	protected String targetExcludeContentPattern = null;

	protected boolean isLicenseHeaderStep(FormatterStep formatterStep) {
		String formatterStepName = formatterStep.getName();

		if (formatterStepName.startsWith(LicenseHeaderStep.class.getName())) {
			return true;
		}

		return false;
	}

	/**
	 * Sets which files should be formatted. Files to be formatted = (target -
	 * targetExclude).
	 * <p>
	 * When this method is called multiple times, only the last call has any effect.
	 * <p>
	 * FileCollections pass through raw. Strings are treated as the 'include' arg to
	 * fileTree, with project.rootDir as the dir. List<String> are treated as the
	 * 'includes' arg to fileTree, with project.rootDir as the dir. Anything else
	 * gets passed to getProject().files().
	 * <p>
	 * If you pass any strings that start with "**\/*", this method will
	 * automatically filter out "build", ".gradle", and ".git" folders.
	 */
	public void target(Object... targets) {
		this.target = parseTargetsIsExclude(targets, false);
	}

	/**
	 * Sets which files will be excluded from formatting. Files to be formatted =
	 * (target - targetExclude).
	 * <p>
	 * When this method is called multiple times, only the last call has any effect.
	 * <p>
	 * FileCollections pass through raw. Strings are treated as the 'include' arg to
	 * fileTree, with project.rootDir as the dir. List<String> are treated as the
	 * 'includes' arg to fileTree, with project.rootDir as the dir. Anything else
	 * gets passed to getProject().files().
	 */
	public void targetExclude(Object... targets) {
		this.targetExclude = parseTargetsIsExclude(targets, true);
	}

	/**
	 * Excludes all files whose content contains {@code string}.
	 * <p>
	 * When this method is called multiple times, only the last call has any effect.
	 */
	public void targetExcludeIfContentContains(String string) {
		targetExcludeIfContentContainsRegex(Pattern.quote(string));
	}

	/**
	 * Excludes all files whose content contains the given regex.
	 * <p>
	 * When this method is called multiple times, only the last call has any effect.
	 */
	public void targetExcludeIfContentContainsRegex(String regex) {
		this.targetExcludeContentPattern = regex;
	}

	private FileCollection parseTargetsIsExclude(Object[] targets, boolean isExclude) {
		requireElementsNonNull(targets);
		if (targets.length == 0) {
			return getProject().files();
		} else if (targets.length == 1) {
			return parseTargetIsExclude(targets[0], isExclude);
		} else {
			FileCollection union = getProject().files();
			for (Object target : targets) {
				union = union.plus(parseTargetIsExclude(target, isExclude));
			}
			return union;
		}
	}

	/**
	 * FileCollections pass through raw. Strings are treated as the 'include' arg to
	 * fileTree, with project.rootDir as the dir. List<String> are treated as the
	 * 'includes' arg to fileTree, with project.rootDir as the dir. Anything else
	 * gets passed to getProject().files().
	 */
	protected final FileCollection parseTarget(Object target) {
		return parseTargetIsExclude(target, false);
	}

	private final FileCollection parseTargetIsExclude(Object target, boolean isExclude) {
		if (target instanceof Collection) {
			return parseTargetsIsExclude(((Collection<?>) target).toArray(), isExclude);
		} else if (target instanceof FileCollection) {
			return (FileCollection) target;
		} else if (target instanceof String) {
			File dir = getProject().getProjectDir();
			ConfigurableFileTree matchedFiles = getProject().fileTree(dir);
			String targetString = (String) target;
			matchedFiles.include(targetString);

			// since people are likely to do '**/*.md', we want to make sure to exclude
			// folders
			// they don't want to format which will slow down the operation greatly
			// but we only want to do that if they are *including* - if they are specifying
			// what they want to exclude, we shouldn't filter at all
			if (isExclude) {
				return matchedFiles;
			}
			if (targetString.startsWith("**/") || targetString.startsWith("**\\")) {
				List<String> excludes = new ArrayList<>();
				// no git
				excludes.add(".git");
				// no .gradle
				if (getProject() == getProject().getRootProject()) {
					excludes.add(".gradle");
				}
				// no build folders (flatInclude means that subproject might not be subfolders,
				// see https://github.com/diffplug/spotless/issues/121)
				relativizeIfSubdir(excludes, dir, getProject().getLayout().getBuildDirectory().getAsFile().get());
				for (Project subproject : getProject().getSubprojects()) {
					relativizeIfSubdir(excludes, dir, subproject.getLayout().getBuildDirectory().getAsFile().get());
				}
				matchedFiles.exclude(excludes);
			}
			return matchedFiles;
		} else {
			return getProject().files(target);
		}
	}

	private static void relativizeIfSubdir(List<String> relativePaths, File root, File dest) {
		String relativized = relativize(root, dest);
		if (relativized != null) {
			relativePaths.add(relativized);
		}
	}

	/**
	 * Returns the relative path between root and dest, or null if dest is not a
	 * child of root.
	 */
	static @Nullable String relativize(File root, File dest) {
		String rootPath = root.getAbsolutePath();
		String destPath = dest.getAbsolutePath();
		if (!destPath.startsWith(rootPath)) {
			return null;
		} else {
			String relativized = destPath.substring(rootPath.length());
			return relativized.startsWith("/") || relativized.startsWith("\\") ? relativized.substring(1) : relativized;
		}
	}

	/** The steps that need to be added. */
	protected final List<FormatterStep> steps = new ArrayList<>();

	/** Adds a new step. */
	public void addStep(FormatterStep newStep) {
		requireNonNull(newStep);
		int existingIdx = getExistingStepIdx(newStep.getName());
		if (existingIdx != -1) {
			throw new GradleException(
					"Multiple steps with name '" + newStep.getName() + "' for spotless format '" + formatName() + "'");
		}
		steps.add(newStep);
	}

	/** Adds a new step that requires a Provisioner. */
	public void addStep(Function<Provisioner, FormatterStep> createStepFn) {
		requireNonNull(createStepFn);
		FormatterStep newStep = createStepFn.apply(provisioner());
		addStep(newStep);
	}

	/**
	 * Returns the index of the existing step with the given name, or -1 if no such
	 * step exists.
	 */
	protected int getExistingStepIdx(String stepName) {
		for (int i = 0; i < steps.size(); ++i) {
			if (steps.get(i).getName().equals(stepName)) {
				return i;
			}
		}
		return -1;
	}

	/** Replaces the given step. */
	protected void replaceStep(FormatterStep replacementStep) {
		int existingIdx = getExistingStepIdx(replacementStep.getName());
		if (existingIdx == -1) {
			throw new GradleException("Cannot replace step '" + replacementStep.getName() + "' for spotless format '"
					+ formatName() + "' because it hasn't been added yet.");
		}
		steps.set(existingIdx, replacementStep);
	}

	/** Clears all of the existing steps. */
	public void clearSteps() {
		steps.clear();
	}

	/**
	 * An optional performance optimization if you are using any of the
	 * {@code custom} methods. If you aren't explicitly calling {@code custom}, then
	 * this method has no effect.
	 * <p>
	 * Spotless tracks what files have changed from run to run, so that it can run
	 * faster by only checking files which have changed, or whose formatting steps
	 * have changed. If you use the {@code custom} methods, then Gradle can never
	 * mark your files as {@code up-to-date}, because it can't know if perhaps the
	 * behavior of your custom function has changed.
	 * <p>
	 * If you set {@code bumpThisNumberIfACustomStepChanges( <some number> )}, then
	 * spotless will assume that the custom rules have not changed if the number has
	 * not changed. If a custom rule does change, then you must bump the number so
	 * that spotless will know that it must recheck the files it has already
	 * checked.
	 */
	public void bumpThisNumberIfACustomStepChanges(int number) {
		globalState = number;
	}

	private Serializable globalState = new NeverUpToDateBetweenRuns();

	static class NeverUpToDateBetweenRuns extends LazyForwardingEquality<Integer> {
		private static final long serialVersionUID = 1L;
		private static final Random RANDOM = new Random();

		@Override
		protected Integer calculateState() throws Exception {
			return RANDOM.nextInt();
		}
	}

	/**
	 * Adds a custom step. Receives a string with unix-newlines, must return a
	 * string with unix newlines.
	 */
	public void custom(String name, Closure<String> formatter) {
		requireNonNull(formatter, "formatter");
		custom(name, new ClosureFormatterFunc(formatter));
	}

	static class ClosureFormatterFunc implements FormatterFunc, Serializable {
		private Closure<String> closure;

		ClosureFormatterFunc(Closure<String> closure) {
			this.closure = closure;
		}

		@Override
		public String apply(String unixNewlines) {
			return closure.call(unixNewlines);
		}

		private void writeObject(java.io.ObjectOutputStream stream) throws java.io.IOException {
			stream.writeObject(closure.dehydrate());
		}

		private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
			this.closure = (Closure<String>) stream.readObject();
		}
	}

	/**
	 * Adds a custom step. Receives a string with unix-newlines, must return a
	 * string with unix newlines.
	 */
	public void custom(String name, FormatterFunc formatter) {
		requireNonNull(formatter, "formatter");
		if (badSemverOfGradle() < badSemver(SpotlessPlugin.VER_GRADLE_minVersionForCustom)) {
			throw new GradleException("The 'custom' method is only available if you are using Gradle "
					+ SpotlessPlugin.VER_GRADLE_minVersionForCustom
					+ " or newer, this is "
					+ GradleVersion.current().getVersion());
		}
		addStep(FormatterStep.createLazy(name, () -> globalState, SerializedFunction.alwaysReturns(formatter)));
	}

	/** Highly efficient find-replace char sequence. */
	public void replace(String name, CharSequence original, CharSequence after) {
		addStep(ReplaceStep.create(name, original, after));
	}

	/** Highly efficient find-replace regex. */
	public void replaceRegex(String name, String regex, String replacement) {
		addStep(ReplaceRegexStep.create(name, regex, replacement));
	}

	/** Removes trailing whitespace. */
	public void trimTrailingWhitespace() {
		addStep(TrimTrailingWhitespaceStep.create());
	}

	/** Ensures that files end with a single newline. */
	public void endWithNewline() {
		addStep(EndWithNewlineStep.create());
	}

	/** Ensures that the files are indented using spaces. */
	public void leadingTabsToSpaces(int spacesPerTab) {
		addStep(IndentStep.Type.SPACE.create(spacesPerTab));
	}

	@Deprecated
	public void indentWithSpaces(int numSpacesPerTab) {
		logDeprecation("indentWithSpaces", "leadingTabsToSpaces");
		leadingTabsToSpaces(numSpacesPerTab);
	}

	/** Ensures that the files are indented using spaces. */
	public void leadingTabsToSpaces() {
		addStep(IndentStep.Type.SPACE.create());
	}

	@Deprecated
	public void indentWithSpaces() {
		logDeprecation("indentWithSpaces", "leadingTabsToSpaces");
		leadingTabsToSpaces();
	}

	/** Ensures that the files are indented using tabs. */
	public void leadingSpacesToTabs(int spacesPerTab) {
		addStep(IndentStep.Type.TAB.create(spacesPerTab));
	}

	@Deprecated
	public void indentWithTabs(int tabToSpaces) {
		logDeprecation("indentWithTabs", "leadingSpacesToTabs");
		leadingSpacesToTabs(tabToSpaces);
	}

	/** Ensures that the files are indented using tabs. */
	public void leadingSpacesToTabs() {
		addStep(IndentStep.Type.TAB.create());
	}

	@Deprecated
	public void indentWithTabs() {
		logDeprecation("indentWithTabs", "leadingSpacesToTabs");
		leadingSpacesToTabs();
	}

	private static void logDeprecation(String methodName, String replacement) {
		logger.warn("'{}' is deprecated, use '{}' in your gradle build script instead.", methodName, replacement);
	}

	/** Ensures formatting of files via native binary. */
	public void nativeCmd(String name, String pathToExe, List<String> arguments) {
		addStep(NativeCmdStep.create(name, new File(pathToExe), arguments));
	}

	/**
	 * Created by {@link FormatExtension#licenseHeader(String, String)} or
	 * {@link FormatExtension#licenseHeaderFile(Object, String)}. For most
	 * language-specific formats (e.g. java, scala, etc.) you can omit the second
	 * {@code delimiter} argument, because it is supplied automatically
	 * ({@link HasBuiltinDelimiterForLicense}).
	 */
	public class LicenseHeaderConfig {
		LicenseHeaderStep builder;
		Boolean updateYearWithLatest = null;

		public LicenseHeaderConfig named(String name) {
			String existingStepName = builder.getName();
			builder = builder.withName(name);
			int existingStepIdx = getExistingStepIdx(existingStepName);
			if (existingStepIdx != -1) {
				steps.set(existingStepIdx, createStep());
			} else {
				addStep(createStep());
			}
			return this;
		}

		public LicenseHeaderConfig onlyIfContentMatches(String contentPattern) {
			builder = builder.withContentPattern(contentPattern);
			replaceStep(createStep());
			return this;
		}

		public LicenseHeaderConfig(LicenseHeaderStep builder) {
			this.builder = builder;
		}

		/**
		 * @param delimiter Spotless will look for a line that starts with this regular
		 *                  expression pattern to know what the "top" is.
		 */
		public LicenseHeaderConfig delimiter(String delimiter) {
			builder = builder.withDelimiter(delimiter);
			replaceStep(createStep());
			return this;
		}

		/**
		 * @param yearSeparator The characters used to separate the first and last years
		 *                      in multi years patterns.
		 */
		public LicenseHeaderConfig yearSeparator(String yearSeparator) {
			builder = builder.withYearSeparator(yearSeparator);
			replaceStep(createStep());
			return this;
		}

		public LicenseHeaderConfig skipLinesMatching(String skipLinesMatching) {
			builder = builder.withSkipLinesMatching(skipLinesMatching);
			replaceStep(createStep());
			return this;
		}

		/**
		 * @param updateYearWithLatest Will turn {@code 2004} into {@code 2004-2020},
		 *                             and {@code 2004-2019} into {@code 2004-2020}
		 *                             Default value is false, unless
		 *                             {@link SpotlessExtensionImpl#ratchetFrom(String)}
		 *                             is used, in which case default value is true.
		 */
		public LicenseHeaderConfig updateYearWithLatest(boolean updateYearWithLatest) {
			this.updateYearWithLatest = updateYearWithLatest;
			replaceStep(createStep());
			return this;
		}

		FormatterStep createStep() {
			return builder.withYearModeLazy(() -> {
				if ("true".equals(spotless.project
						.findProperty(LicenseHeaderStep.FLAG_SET_LICENSE_HEADER_YEARS_FROM_GIT_HISTORY()))) {
					return YearMode.SET_FROM_GIT;
				} else {
					boolean updateYear = updateYearWithLatest == null ? getRatchetFrom() != null : updateYearWithLatest;
					return updateYear ? YearMode.UPDATE_TO_TODAY : YearMode.PRESERVE;
				}
			}).build();
		}
	}

	/**
	 * @param licenseHeader Content that should be at the top of every file.
	 * @param delimiter     Spotless will look for a line that starts with this
	 *                      regular expression pattern to know what the "top" is.
	 */
	public LicenseHeaderConfig licenseHeader(String licenseHeader, String delimiter) {
		LicenseHeaderConfig config = new LicenseHeaderConfig(
				LicenseHeaderStep.headerDelimiter(licenseHeader, delimiter));
		addStep(config.createStep());
		return config;
	}

	/**
	 * @param licenseHeaderFile Content that should be at the top of every file.
	 * @param delimiter         Spotless will look for a line that starts with this
	 *                          regular expression pattern to know what the "top"
	 *                          is.
	 */
	public LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile, String delimiter) {
		LicenseHeaderConfig config = new LicenseHeaderConfig(LicenseHeaderStep.headerDelimiter(() -> {
			File file = getProject().file(licenseHeaderFile);
			byte[] data = Files.readAllBytes(file.toPath());
			return new String(data, getEncoding());
		}, delimiter));
		addStep(config.createStep());
		return config;
	}

	public abstract static class NpmStepConfig<T extends NpmStepConfig<?>> {

		public static final String SPOTLESS_NPM_INSTALL_CACHE_DEFAULT_NAME = "spotless-npm-install-cache";

		@Nullable
		protected Object npmFile;

		@Nullable
		protected Object nodeFile;

		@Nullable
		protected Object npmInstallCache;

		@Nullable
		protected Object npmrcFile;

		protected Project project;

		private Consumer<FormatterStep> replaceStep;

		public NpmStepConfig(Project project, Consumer<FormatterStep> replaceStep) {
			this.project = requireNonNull(project);
			this.replaceStep = requireNonNull(replaceStep);
		}

		@SuppressWarnings("unchecked")
		public T npmExecutable(final Object npmFile) {
			this.npmFile = npmFile;
			replaceStep();
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T nodeExecutable(final Object nodeFile) {
			this.nodeFile = nodeFile;
			replaceStep();
			return (T) this;
		}

		public T npmrc(final Object npmrcFile) {
			this.npmrcFile = npmrcFile;
			replaceStep();
			return (T) this;
		}

		public T npmInstallCache(final Object npmInstallCache) {
			this.npmInstallCache = npmInstallCache;
			replaceStep();
			return (T) this;
		}

		public T npmInstallCache() {
			this.npmInstallCache = new File(project.getLayout().getBuildDirectory().getAsFile().get(),
					SPOTLESS_NPM_INSTALL_CACHE_DEFAULT_NAME);
			replaceStep();
			return (T) this;
		}

		File npmFileOrNull() {
			return fileOrNull(npmFile);
		}

		File nodeFileOrNull() {
			return fileOrNull(nodeFile);
		}

		File npmrcFileOrNull() {
			return fileOrNull(npmrcFile);
		}

		File npmModulesCacheOrNull() {
			return fileOrNull(npmInstallCache);
		}

		private File fileOrNull(Object npmFile) {
			return npmFile != null ? project.file(npmFile) : null;
		}

		protected void replaceStep() {
			replaceStep.accept(createStep());
		}

		abstract protected FormatterStep createStep();

	}

	public class PrettierConfig extends NpmStepConfig<PrettierConfig> {

		@Nullable
		Object prettierConfigFile;

		@Nullable
		Map<String, Object> prettierConfig;

		final Map<String, String> devDependencies;

		PrettierConfig(Map<String, String> devDependencies) {
			super(getProject(), FormatExtension.this::replaceStep);
			this.devDependencies = requireNonNull(devDependencies);
		}

		public PrettierConfig configFile(final Object prettierConfigFile) {
			this.prettierConfigFile = prettierConfigFile;
			replaceStep();
			return this;
		}

		public PrettierConfig config(final Map<String, Object> prettierConfig) {
			this.prettierConfig = new TreeMap<>(prettierConfig);
			replaceStep();
			return this;
		}

		@Override
		protected FormatterStep createStep() {
			final Project project = getProject();
			return PrettierFormatterStep.create(devDependencies, provisioner(), project.getProjectDir(),
					project.getLayout().getBuildDirectory().getAsFile().get(), npmModulesCacheOrNull(),
					new NpmPathResolver(npmFileOrNull(), nodeFileOrNull(), npmrcFileOrNull(),
							Arrays.asList(project.getProjectDir(), project.getRootDir())),
					new com.diffplug.spotless.npm.PrettierConfig(
							this.prettierConfigFile != null ? project.file(this.prettierConfigFile) : null,
							this.prettierConfig));
		}
	}

	/**
	 * Generic Biome formatter step that detects the language of the input file from
	 * the file name. It should be specified as a formatter step for a generic
	 * <code>format{ ... }</code>.
	 */
	public class BiomeGeneric extends BiomeStepConfig<BiomeGeneric> {
		@Nullable
		String language;

		/**
		 * Creates a new Biome config that downloads the Biome executable for the given
		 * version from the network.
		 *
		 * @param version Biome version to use. The default version is used when
		 *                <code>null</code>.
		 */
		public BiomeGeneric(String version) {
			super(getProject(), FormatExtension.this::replaceStep, BiomeFlavor.BIOME, version);
		}

		/**
		 * Sets the language (syntax) of the input files to format. When
		 * <code>null</code> or the empty string, the language is detected automatically
		 * from the file name. Currently the following languages are supported by Biome:
		 * <ul>
		 * <li>js (JavaScript)</li>
		 * <li>jsx (JavaScript + JSX)</li>
		 * <li>js? (JavaScript or JavaScript + JSX, depending on the file
		 * extension)</li>
		 * <li>ts (TypeScript)</li>
		 * <li>tsx (TypeScript + JSX)</li>
		 * <li>ts? (TypeScript or TypeScript + JSX, depending on the file
		 * extension)</li>
		 * <li>css (CSS, requires biome &gt;= 1.9.0)</li>
		 * <li>json (JSON)</li>
		 * <li>jsonc (JSON + comments)</li>
		 * </ul>
		 *
		 * @param language The language of the files to format.
		 * @return This step for further configuration.
		 */
		public BiomeGeneric language(String language) {
			this.language = language;
			replaceStep();
			return this;
		}

		@Override
		protected String getLanguage() {
			return language;
		}

		@Override
		protected BiomeGeneric getThis() {
			return this;
		}
	}

	/** Uses the default version of prettier. */
	public PrettierConfig prettier() {
		return prettier(PrettierFormatterStep.defaultDevDependencies());
	}

	/** Uses the specified version of prettier. */
	public PrettierConfig prettier(String version) {
		return prettier(PrettierFormatterStep.defaultDevDependenciesWithPrettier(version));
	}

	/** Uses exactly the npm packages specified in the map. */
	public PrettierConfig prettier(Map<String, String> devDependencies) {
		PrettierConfig prettierConfig = new PrettierConfig(devDependencies);
		addStep(prettierConfig.createStep());
		return prettierConfig;
	}

	/**
	 * Defaults to downloading the default Biome version from the network. To work
	 * offline, you can specify the path to the Biome executable via
	 * {@code biome().pathToExe(...)}.
	 */
	public BiomeStepConfig<?> biome() {
		return biome(null);
	}

	/** Downloads the given Biome version from the network. */
	public BiomeStepConfig<?> biome(String version) {
		var biomeConfig = new BiomeGeneric(version);
		addStep(biomeConfig.createStep());
		return biomeConfig;
	}

	/** Uses the default version of clang-format. */
	public ClangFormatConfig clangFormat() {
		return clangFormat(ClangFormatStep.defaultVersion());
	}

	/** Uses the specified version of clang-format. */
	public ClangFormatConfig clangFormat(String version) {
		return new ClangFormatConfig(version);
	}

	public class ClangFormatConfig {
		ClangFormatStep stepCfg;

		ClangFormatConfig(String version) {
			this.stepCfg = ClangFormatStep.withVersion(version);
			addStep(createStep());
		}

		/** Any of: LLVM, Google, Chromium, Mozilla, WebKit. */
		public ClangFormatConfig style(String style) {
			stepCfg = stepCfg.withStyle(style);
			replaceStep(createStep());
			return this;
		}

		public ClangFormatConfig pathToExe(String pathToBlack) {
			stepCfg = stepCfg.withPathToExe(pathToBlack);
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return stepCfg.create();
		}
	}

	public class EclipseWtpConfig {
		private final EclipseBasedStepBuilder builder;

		EclipseWtpConfig(EclipseWtpFormatterStep type, String version) {
			builder = type.createBuilder(provisioner());
			builder.setVersion(version);
			addStep(builder.build());
		}

		public void configFile(Object... configFiles) {
			requireElementsNonNull(configFiles);
			Project project = getProject();
			builder.setPreferences(project.files(configFiles).getFiles());
			replaceStep(builder.build());
		}
	}

	public EclipseWtpConfig eclipseWtp(EclipseWtpFormatterStep type) {
		return eclipseWtp(type, EclipseWtpFormatterStep.defaultVersion());
	}

	public EclipseWtpConfig eclipseWtp(EclipseWtpFormatterStep type, String version) {
		return new EclipseWtpConfig(type, version);
	}

	/**
	 * <pre>
	 * spotless {
	 *   format 'examples', {
	 *     target '*.md'
	 *     withinBlocks 'javascript examples', '\n```javascript\n', '\n```\n', {
	 *       prettier().config(['parser': 'javascript'])
	 *     }
	 *     ...
	 * </pre>
	 */
	public void withinBlocks(String name, String open, String close, Action<FormatExtension> configure) {
		withinBlocks(name, open, close, FormatExtension.class, configure);
	}

	/**
	 * Same as {@link #withinBlocks(String, String, String, Action)}, except you can
	 * specify any language-specific subclass of {@link FormatExtension} to get
	 * language-specific steps.
	 *
	 * <pre>
	 * spotless {
	 *   format 'examples', {
	 *     target '*.md'
	 *     withinBlocks 'java examples', '\n```java\n', '\n```\n', com.diffplug.gradle.spotless.JavaExtension, {
	 *       googleJavaFormat()
	 *       formatAnnotations()
	 *     }
	 *     ...
	 * </pre>
	 */
	public <T extends FormatExtension> void withinBlocks(String name, String open, String close, Class<T> clazz,
			Action<T> configure) {
		withinBlocksHelper(FenceStep.named(name).openClose(open, close), clazz, configure);
	}

	/**
	 * Same as {@link #withinBlocks(String, String, String, Action)}, except instead
	 * of an open/close pair, you specify a regex with exactly one capturing group.
	 */
	public void withinBlocksRegex(String name, String regex, Action<FormatExtension> configure) {
		withinBlocksRegex(name, regex, FormatExtension.class, configure);
	}

	/**
	 * Same as {@link #withinBlocksRegex(String, String, Action)}, except you can
	 * specify any language-specific subclass of {@link FormatExtension} to get
	 * language-specific steps.
	 */
	public <T extends FormatExtension> void withinBlocksRegex(String name, String regex, Class<T> clazz,
			Action<T> configure) {
		withinBlocksHelper(FenceStep.named(name).regex(regex), clazz, configure);
	}

	private <T extends FormatExtension> void withinBlocksHelper(FenceStep fence, Class<T> clazz,
			Action<T> configure) {
		// create the sub-extension
		T formatExtension = spotless.instantiateFormatExtension(clazz);
		// configure it
		configure.execute(formatExtension);
		// create a step which applies all of those steps as sub-steps
		FormatterStep step = fence.applyWithin(formatExtension.steps);
		addStep(step);
	}

	/**
	 * Given a regex with *exactly one capturing group*, disables formatting inside
	 * that captured group.
	 */
	public void toggleOffOnRegex(String regex) {
		this.toggleFence = FenceStep.named(FenceStep.defaultToggleName()).regex(regex);
	}

	/** Disables formatting between the given tags. */
	public void toggleOffOn(String off, String on) {
		this.toggleFence = FenceStep.named(FenceStep.defaultToggleName()).openClose(off, on);
	}

	/** Disables formatting between {@code spotless:off} and {@code spotless:on}. */
	public void toggleOffOn() {
		toggleOffOn(FenceStep.defaultToggleOff(), FenceStep.defaultToggleOn());
	}

	/**
	 * Undoes all previous calls to {@link #toggleOffOn()} and
	 * {@link #toggleOffOn(String, String)}.
	 */
	public void toggleOffOnDisable() {
		this.toggleFence = null;
	}

	private @Nullable FenceStep toggleFence;

	/** Sets up a format task according to the values in this extension. */
	protected void setupTask(SpotlessTask task) {
		task.setEncoding(getEncoding().name());
		task.setLintSuppressions(lintSuppressions);
		FileCollection totalTarget = targetExclude == null ? target : target.minus(targetExclude);
		task.setTarget(totalTarget);
		List<FormatterStep> steps;
		if (toggleFence != null) {
			// need a mutable List, 'steps' is mutated by 'steps.replaceAll()' below
			steps = new ArrayList<>();
			steps.add(toggleFence.preserveWithin(this.steps));
		} else {
			steps = this.steps;
		}
		if (targetExcludeContentPattern != null) {
			steps.replaceAll(
					formatterStep -> formatterStep.filterByContent(OnMatch.EXCLUDE, targetExcludeContentPattern));
		}
		task.setSteps(steps);
		Directory projectDir = getProject().getLayout().getProjectDirectory();
		LineEnding lineEndings = getLineEndings();
		task.setLineEndingsPolicy(
				getProject().provider(() -> lineEndings.createPolicy(projectDir.getAsFile(), () -> totalTarget)));
		spotless.getRegisterDependenciesTask().hookSubprojectTask(task);
		task.setupRatchet(getRatchetFrom() != null ? getRatchetFrom() : "");
	}

	/** Returns the project that this extension is attached to. */
	protected Project getProject() {
		return spotless.project;
	}

	/** Eager version of {@link #createIndependentApplyTaskLazy(String)} */
	public SpotlessApply createIndependentApplyTask(String taskName) {
		return createIndependentApplyTaskLazy(taskName).get();
	}

	/**
	 * Creates an independent {@link SpotlessApply} for (very) unusual
	 * circumstances.
	 * <p>
	 * Most users will not want this method. In the rare case that you want to
	 * create a {@code SpotlessApply} which is independent of the normal Spotless
	 * machinery, this will let you do that.
	 * <p>
	 * The returned task will not be hooked up to the global {@code spotlessApply},
	 * and there will be no corresponding {@code check} task.
	 * <p>
	 * The task name must not end with `Apply`.
	 * <p>
	 * NOTE: does not respect the rarely-used <a href=
	 * "https://github.com/diffplug/spotless/blob/b7f8c551a97dcb92cc4b0ee665448da5013b30a3/plugin-gradle/README.md#can-i-apply-spotless-to-specific-files">{@code spotlessFiles}
	 * property</a>.
	 */
	public TaskProvider<SpotlessApply> createIndependentApplyTaskLazy(String taskName) {
		Preconditions.checkArgument(!taskName.endsWith(SpotlessExtension.APPLY),
				"Task name must not end with " + SpotlessExtension.APPLY);
		TaskProvider<SpotlessTaskImpl> spotlessTask = spotless.project.getTasks()
				.register(taskName + SpotlessTaskService.INDEPENDENT_HELPER, SpotlessTaskImpl.class, task -> {
					task.init(spotless.getRegisterDependenciesTask().getTaskService());
					setupTask(task);
					// clean removes the SpotlessCache, so we have to run after clean
					task.mustRunAfter(BasePlugin.CLEAN_TASK_NAME);
				});
		// create the apply task
		TaskProvider<SpotlessApply> applyTask = spotless.project.getTasks().register(taskName, SpotlessApply.class,
				task -> {
					task.dependsOn(spotlessTask);
					task.init(spotlessTask.get());
				});
		return applyTask;
	}

	protected GradleException noDefaultTargetException() {
		return new GradleException("Spotless failure, no target set!  You must set a target for " + formatName());
	}
}
