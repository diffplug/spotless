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
package com.diffplug.gradle.spotless;

import static com.diffplug.gradle.spotless.PluginGradlePreconditions.requireElementsNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.util.PatternFilterable;

import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LazyForwardingEquality;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep;
import com.diffplug.spotless.generic.EndWithNewlineStep;
import com.diffplug.spotless.generic.IndentStep;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.generic.ReplaceRegexStep;
import com.diffplug.spotless.generic.ReplaceStep;
import com.diffplug.spotless.generic.TrimTrailingWhitespaceStep;
import com.diffplug.spotless.npm.PrettierFormatterStep;

import groovy.lang.Closure;

/** Adds a `spotless{Name}Check` and `spotless{Name}Apply` task. */
public class FormatExtension {
	final SpotlessExtension root;

	public FormatExtension(SpotlessExtension root) {
		this.root = Objects.requireNonNull(root);
	}

	private String formatName() {
		for (Map.Entry<String, FormatExtension> entry : root.formats.entrySet()) {
			if (entry.getValue() == this) {
				return entry.getKey();
			}
		}
		throw new IllegalStateException("This format is not contained by any SpotlessExtension.");
	}

	/** Enables paddedCell mode. @see <a href="https://github.com/diffplug/spotless/blob/master/PADDEDCELL.md">Padded cell</a> */
	@Deprecated
	public void paddedCell() {
		paddedCell(true);
	}

	/** Enables paddedCell mode. @see <a href="https://github.com/diffplug/spotless/blob/master/PADDEDCELL.md">Padded cell</a> */
	@Deprecated
	public void paddedCell(boolean paddedCell) {
		root.project.getLogger().warn("PaddedCell is now always on, and cannot be turned off.");
	}

	LineEnding lineEndings;

	/** Returns the line endings to use (defaults to {@link SpotlessExtension#getLineEndings()}. */
	public LineEnding getLineEndings() {
		return lineEndings == null ? root.getLineEndings() : lineEndings;
	}

	/** Sets the line endings to use (defaults to {@link SpotlessExtension#getLineEndings()}. */
	public void setLineEndings(LineEnding lineEndings) {
		this.lineEndings = Objects.requireNonNull(lineEndings);
	}

	Charset encoding;

	/** Returns the encoding to use (defaults to {@link SpotlessExtension#getEncoding()}. */
	public Charset getEncoding() {
		return encoding == null ? root.getEncoding() : encoding;
	}

	/** Sets the encoding to use (defaults to {@link SpotlessExtension#getEncoding()}. */
	public void setEncoding(String name) {
		setEncoding(Charset.forName(Objects.requireNonNull(name)));
	}

	/** Sets the encoding to use (defaults to {@link SpotlessExtension#getEncoding()}. */
	public void setEncoding(Charset charset) {
		encoding = Objects.requireNonNull(charset);
	}

	final FormatExceptionPolicyStrict exceptionPolicy = new FormatExceptionPolicyStrict();

	/** Ignores errors in the given step. */
	public void ignoreErrorForStep(String stepName) {
		exceptionPolicy.excludeStep(Objects.requireNonNull(stepName));
	}

	/** Ignores errors for the given relative path. */
	public void ignoreErrorForPath(String relativePath) {
		exceptionPolicy.excludePath(Objects.requireNonNull(relativePath));
	}

	/** Sets encoding to use (defaults to {@link SpotlessExtension#getEncoding()}). */
	public void encoding(String charset) {
		setEncoding(charset);
	}

	/** The files to be formatted = (target - targetExclude). */
	protected FileCollection target, targetExclude;

	/**
	 * Sets which files should be formatted.  Files to be formatted = (target - targetExclude).
	 *
	 * When this method is called multiple times, only the last call has any effect.
	 *
	 * FileCollections pass through raw.
	 * Strings are treated as the 'include' arg to fileTree, with project.rootDir as the dir.
	 * List<String> are treated as the 'includes' arg to fileTree, with project.rootDir as the dir.
	 * Anything else gets passed to getProject().files().
	 *
	 * If you pass any strings that start with "**\/*", this method will automatically filter out
	 * "build", ".gradle", and ".git" folders.
	 */
	public void target(Object... targets) {
		this.target = parseTargetsIsExclude(targets, false);
	}

	/**
	 * Sets which files will be excluded from formatting.  Files to be formatted = (target - targetExclude).
	 *
	 * When this method is called multiple times, only the last call has any effect.
	 *
	 * FileCollections pass through raw.
	 * Strings are treated as the 'include' arg to fileTree, with project.rootDir as the dir.
	 * List<String> are treated as the 'includes' arg to fileTree, with project.rootDir as the dir.
	 * Anything else gets passed to getProject().files().
	 */
	public void targetExclude(Object... targets) {
		this.targetExclude = parseTargetsIsExclude(targets, true);
	}

	private FileCollection parseTargetsIsExclude(Object[] targets, boolean isExclude) {
		requireElementsNonNull(targets);
		if (targets.length == 0) {
			return getProject().files();
		} else if (targets.length == 1) {
			return parseTargetIsExclude(targets[0], isExclude);
		} else {
			if (Stream.of(targets).allMatch(o -> o instanceof String)) {
				return parseTargetIsExclude(Arrays.asList(targets), isExclude);
			} else {
				FileCollection union = getProject().files();
				for (Object target : targets) {
					union = union.plus(parseTargetIsExclude(target, isExclude));
				}
				return union;
			}
		}
	}

	/**
	 * FileCollections pass through raw.
	 * Strings are treated as the 'include' arg to fileTree, with project.rootDir as the dir.
	 * List<String> are treated as the 'includes' arg to fileTree, with project.rootDir as the dir.
	 * Anything else gets passed to getProject().files().
	 */
	protected final FileCollection parseTarget(Object target) {
		return parseTargetIsExclude(target, false);
	}

	private final FileCollection parseTargetIsExclude(Object target, boolean isExclude) {
		if (target instanceof FileCollection) {
			return (FileCollection) target;
		} else if (target instanceof String ||
				(target instanceof List && ((List<?>) target).stream().allMatch(o -> o instanceof String))) {
			File dir = getProject().getProjectDir();
			PatternFilterable userExact; // exactly the collection that the user specified
			if (target instanceof String) {
				userExact = getProject().fileTree(dir).include((String) target);
			} else {
				// target can only be a List<String> at this point
				@SuppressWarnings("unchecked")
				List<String> targetList = (List<String>) target;
				userExact = getProject().fileTree(dir).include(targetList);
			}
			boolean filterOutGitAndGradle;
			// since people are likely to do '**/*.md', we want to make sure to exclude folders
			// they don't want to format which will slow down the operation greatly
			// but we only want to do that if they are *including* - if they are specifying
			// what they want to exclude, we shouldn't filter at all
			if (target instanceof String && !isExclude) {
				String str = (String) target;
				filterOutGitAndGradle = str.startsWith("**/*") || str.startsWith("**\\*");
			} else {
				filterOutGitAndGradle = false;
			}
			if (filterOutGitAndGradle) {
				List<String> excludes = new ArrayList<>();
				// no git
				excludes.add(".git");
				// no .gradle
				if (getProject() == getProject().getRootProject()) {
					excludes.add(".gradle");
				}
				// no build folders (flatInclude means that subproject might not be subfolders, see https://github.com/diffplug/spotless/issues/121)
				relativizeIfSubdir(excludes, dir, getProject().getBuildDir());
				for (Project subproject : getProject().getSubprojects()) {
					relativizeIfSubdir(excludes, dir, subproject.getBuildDir());
				}
				userExact = userExact.exclude(excludes);
			}
			return (FileCollection) userExact;
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
	 * Returns the relative path between root and dest,
	 * or null if dest is not a child of root.
	 */
	static @Nullable String relativize(File root, File dest) {
		String rootPath = root.getAbsolutePath();
		String destPath = dest.getAbsolutePath();
		if (!destPath.startsWith(rootPath)) {
			return null;
		} else {
			return destPath.substring(rootPath.length());
		}
	}

	/** The steps that need to be added. */
	protected final List<FormatterStep> steps = new ArrayList<>();

	/** Adds a new step. */
	public void addStep(FormatterStep newStep) {
		Objects.requireNonNull(newStep);
		int existingIdx = getExistingStepIdx(newStep.getName());
		if (existingIdx != -1) {
			throw new GradleException("Multiple steps with name '" + newStep.getName() + "' for spotless format '" + formatName() + "'");
		}
		steps.add(newStep);
	}

	/** Returns the existing step with the given name, if any. */
	@Deprecated
	protected @Nullable FormatterStep getExistingStep(String stepName) {
		return steps.stream() //
				.filter(step -> stepName.equals(step.getName())) //
				.findFirst() //
				.orElse(null);
	}

	/** Returns the index of the existing step with the given name, or -1 if no such step exists. */
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
			throw new GradleException("Cannot replace step '" + replacementStep.getName() + "' for spotless format '" + formatName() + "' because it hasn't been added yet.");
		}
		steps.set(existingIdx, replacementStep);
	}

	/** Clears all of the existing steps. */
	public void clearSteps() {
		steps.clear();
	}

	/**
	 * An optional performance optimization if you are using any of the `custom` or `customLazy`
	 * methods.  If you aren't explicitly calling `custom` or `customLazy`, then this method
	 * has no effect.
	 *
	 * Spotless tracks what files have changed from run to run, so that it can run faster
	 * by only checking files which have changed, or whose formatting steps have changed.
	 * If you use either the `custom` or `customLazy` methods, then gradle can never mark
	 * your files as `up-to-date`, because it can't know if perhaps the behavior of your
	 * custom function has changed.
	 *
	 * If you set `bumpThisNumberIfACustomStepChanges( <some number> )`, then spotless will
	 * assume that the custom rules have not changed if the number has not changed.  If a
	 * custom rule does change, then you must bump the number so that spotless will know
	 * that it must recheck the files it has already checked.
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
	 * Adds the given custom step, which is constructed lazily for performance reasons.
	 *
	 * The resulting function will receive a string with unix-newlines, and it must return a string unix newlines.
	 *
	 * If you're getting errors about `closure cannot be cast to com.diffplug.common.base.Throwing$Function`, then use
	 * {@link #customLazyGroovy(String, ThrowingEx.Supplier)}.
	 */
	public void customLazy(String name, ThrowingEx.Supplier<FormatterFunc> formatterSupplier) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(formatterSupplier, "formatterSupplier");
		addStep(FormatterStep.createLazy(name, () -> globalState, unusedState -> formatterSupplier.get()));
	}

	/** Same as {@link #customLazy(String, ThrowingEx.Supplier)}, but for Groovy closures. */
	public void customLazyGroovy(String name, ThrowingEx.Supplier<Closure<String>> formatterSupplier) {
		Objects.requireNonNull(formatterSupplier, "formatterSupplier");
		customLazy(name, () -> formatterSupplier.get()::call);
	}

	/** Adds a custom step. Receives a string with unix-newlines, must return a string with unix newlines. */
	public void custom(String name, Closure<String> formatter) {
		Objects.requireNonNull(formatter, "formatter");
		custom(name, formatter::call);
	}

	/** Adds a custom step. Receives a string with unix-newlines, must return a string with unix newlines. */
	public void custom(String name, FormatterFunc formatter) {
		Objects.requireNonNull(formatter, "formatter");
		customLazy(name, () -> formatter);
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
	public void indentWithSpaces(int numSpacesPerTab) {
		addStep(IndentStep.Type.SPACE.create(numSpacesPerTab));
	}

	/** Ensures that the files are indented using spaces. */
	public void indentWithSpaces() {
		addStep(IndentStep.Type.SPACE.create());
	}

	/** Ensures that the files are indented using tabs. */
	public void indentWithTabs(int tabToSpaces) {
		addStep(IndentStep.Type.TAB.create(tabToSpaces));
	}

	/** Ensures that the files are indented using tabs. */
	public void indentWithTabs() {
		addStep(IndentStep.Type.TAB.create());
	}

	/**
	 * Created by {@link FormatExtension#licenseHeader(String, String)} or {@link FormatExtension#licenseHeaderFile(Object, String)}.
	 * For most language-specific formats (e.g. java, scala, etc.) you can omit the second `delimiter` argument, because it is supplied
	 * automatically ({@link HasBuiltinDelimiterForLicense}).
	 */
	public abstract class LicenseHeaderConfig {
		String delimiter;
		String yearSeparator = LicenseHeaderStep.defaultYearDelimiter();
		Boolean updateYearWithLatest = null;

		public LicenseHeaderConfig(String delimiter) {
			this.delimiter = Objects.requireNonNull(delimiter, "delimiter");
		}

		/**
		 * @param delimiter
		 *            Spotless will look for a line that starts with this regular expression pattern to know what the "top" is.
		 */
		public LicenseHeaderConfig delimiter(String delimiter) {
			this.delimiter = Objects.requireNonNull(delimiter, "delimiter");
			replaceStep(createStep());
			return this;
		}

		/**
		 * @param yearSeparator
		 *           The characters used to separate the first and last years in multi years patterns.
		 */
		public LicenseHeaderConfig yearSeparator(String yearSeparator) {
			this.yearSeparator = Objects.requireNonNull(yearSeparator, "yearSeparator");
			replaceStep(createStep());
			return this;
		}

		/**
		 * @param updateYearWithLatest
		 *           Will turn `2004` into `2004-2020`, and `2004-2019` into `2004-2020`
		 *           Default value is false, unless {@link SpotlessExtension#ratchetFrom(String)} is used, in which case default value is true.
		 */
		public LicenseHeaderConfig updateYearWithLatest(boolean overwriteYearLatest) {
			this.updateYearWithLatest = overwriteYearLatest;
			replaceStep(createStep());
			return this;
		}

		protected abstract String licenseHeader() throws IOException;

		FormatterStep createStep() {
			return FormatterStep.createLazy(LicenseHeaderStep.name(), () -> {
				// by default, we should update the year if the user is using ratchetFrom
				boolean updateYear = updateYearWithLatest == null ? FormatExtension.this.root.getRatchetFrom() != null : updateYearWithLatest;
				return new LicenseHeaderStep(licenseHeader(), delimiter, yearSeparator, updateYear);
			}, step -> step::format);
		}
	}

	private class LicenseStringHeaderConfig extends LicenseHeaderConfig {
		private String header;

		LicenseStringHeaderConfig(String delimiter, String header) {
			super(delimiter);
			this.header = Objects.requireNonNull(header, "header");
		}

		@Override
		protected String licenseHeader() {
			return header;
		}
	}

	private class LicenseFileHeaderConfig extends LicenseHeaderConfig {
		private Object headerFile;

		LicenseFileHeaderConfig(String delimiter, Object headerFile) {
			super(delimiter);
			this.headerFile = Objects.requireNonNull(headerFile, "headerFile");
		}

		@Override
		protected String licenseHeader() throws IOException {
			byte[] content = Files.readAllBytes(getProject().file(headerFile).toPath());
			return new String(content, getEncoding());
		}
	}

	/**
	 * @param licenseHeader
	 *            Content that should be at the top of every file.
	 * @param delimiter
	 *            Spotless will look for a line that starts with this regular expression pattern to know what the "top" is.
	 */
	public LicenseHeaderConfig licenseHeader(String licenseHeader, String delimiter) {
		LicenseHeaderConfig config = new LicenseStringHeaderConfig(delimiter, licenseHeader);
		addStep(config.createStep());
		return config;
	}

	/**
	 * @param licenseHeaderFile
	 *            Content that should be at the top of every file.
	 * @param delimiter
	 *            Spotless will look for a line that starts with this regular expression pattern to know what the "top" is.
	 */
	public LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile, String delimiter) {
		LicenseHeaderConfig config = new LicenseFileHeaderConfig(delimiter, licenseHeaderFile);
		addStep(config.createStep());
		return config;
	}

	public abstract class NpmStepConfig<T extends NpmStepConfig<?>> {
		@Nullable
		protected Object npmFile;

		@SuppressWarnings("unchecked")
		public T npmExecutable(final Object npmFile) {
			this.npmFile = npmFile;
			replaceStep(createStep());
			return (T) this;
		}

		File npmFileOrNull() {
			return npmFile != null ? getProject().file(npmFile) : null;
		}

		abstract FormatterStep createStep();

	}

	public class PrettierConfig extends NpmStepConfig<PrettierConfig> {

		@Nullable
		Object prettierConfigFile;

		@Nullable
		Map<String, Object> prettierConfig;

		final Map<String, String> devDependencies;

		PrettierConfig(Map<String, String> devDependencies) {
			this.devDependencies = Objects.requireNonNull(devDependencies);
		}

		public PrettierConfig configFile(final Object prettierConfigFile) {
			this.prettierConfigFile = prettierConfigFile;
			replaceStep(createStep());
			return this;
		}

		public PrettierConfig config(final Map<String, Object> prettierConfig) {
			this.prettierConfig = new TreeMap<>(prettierConfig);
			replaceStep(createStep());
			return this;
		}

		FormatterStep createStep() {
			final Project project = getProject();
			return PrettierFormatterStep.create(
					devDependencies,
					GradleProvisioner.fromProject(project),
					project.getBuildDir(),
					npmFileOrNull(),
					new com.diffplug.spotless.npm.PrettierConfig(
							this.prettierConfigFile != null ? project.file(this.prettierConfigFile) : null,
							this.prettierConfig));
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

	public class EclipseWtpConfig {
		private final EclipseBasedStepBuilder builder;

		EclipseWtpConfig(EclipseWtpFormatterStep type, String version) {
			builder = type.createBuilder(GradleProvisioner.fromProject(getProject()));
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

	/** Sets up a format task according to the values in this extension. */
	protected void setupTask(SpotlessTask task) {
		task.setEncoding(getEncoding().name());
		task.setExceptionPolicy(exceptionPolicy);
		if (targetExclude == null) {
			task.setTarget(target);
		} else {
			task.setTarget(target.minus(targetExclude));
		}
		task.setSteps(steps);
		task.setLineEndingsPolicy(getLineEndings().createPolicy(getProject().getProjectDir(), () -> task.target));
		if (root.project != root.project.getRootProject()) {
			root.registerDependenciesTask.hookSubprojectTask(task);
		}
		if (root.getRatchetFrom() != null) {
			task.treeSha = GitRatchet.treeShaOf(root.project, root.getRatchetFrom());
		}
	}

	/** Returns the project that this extension is attached to. */
	protected Project getProject() {
		return root.project;
	}

	/**
	 * Creates an independent {@link SpotlessApply} for (very) unusual circumstances.
	 *
	 * Most users will not want this method.  In the rare case that you want to create
	 * a `SpotlessApply` which is independent of the normal Spotless machinery, this will
	 * let you do that.
	 *
	 * The returned task will not be hooked up to the global `spotlessApply`, and there will be no corresponding `check` task.
	 *
	 * NOTE: does not respect the rarely-used [`spotlessFiles` property](https://github.com/diffplug/spotless/blob/b7f8c551a97dcb92cc4b0ee665448da5013b30a3/plugin-gradle/README.md#can-i-apply-spotless-to-specific-files).
	 */
	public SpotlessApply createIndependentApplyTask(String taskName) {
		// create and setup the task
		SpotlessTask spotlessTask = root.project.getTasks().create(taskName + "Helper", SpotlessTask.class);
		setupTask(spotlessTask);
		// enforce the clean ordering
		Task clean = root.project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME);
		spotlessTask.mustRunAfter(clean);
		// ignore the filePatterns
		spotlessTask.setFilePatterns("");
		// create the apply task
		SpotlessApply applyTask = root.project.getTasks().create(taskName, SpotlessApply.class);
		applyTask.setSpotlessOutDirectory(spotlessTask.getOutputDirectory());
		applyTask.linkSource(spotlessTask);
		applyTask.dependsOn(spotlessTask);

		return applyTask;
	}
}
