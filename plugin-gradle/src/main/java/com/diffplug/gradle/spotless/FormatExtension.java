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
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LazyForwardingEquality;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.generic.EndWithNewlineStep;
import com.diffplug.spotless.generic.IndentStep;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.generic.ReplaceRegexStep;
import com.diffplug.spotless.generic.ReplaceStep;
import com.diffplug.spotless.generic.TrimTrailingWhitespaceStep;

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

	boolean paddedCell = false;

	/** Enables paddedCell mode. @see <a href="https://github.com/diffplug/spotless/blob/master/PADDEDCELL.md">Padded cell</a> */
	public void paddedCell() {
		paddedCell(true);
	}

	/** Enables paddedCell mode. @see <a href="https://github.com/diffplug/spotless/blob/master/PADDEDCELL.md">Padded cell</a> */
	public void paddedCell(boolean paddedCell) {
		this.paddedCell = paddedCell;
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

	/** The files that need to be formatted. */
	protected FileCollection target;

	/**
	 * FileCollections pass through raw.
	 * Strings are treated as the 'include' arg to fileTree, with project.rootDir as the dir.
	 * List<String> are treated as the 'includes' arg to fileTree, with project.rootDir as the dir.
	 * Anything else gets passed to getProject().files().
	 */
	public void target(Object... targets) {
		requireElementsNonNull(targets);
		if (targets.length == 0) {
			this.target = getProject().files();
		} else if (targets.length == 1) {
			this.target = parseTarget(targets[0]);
		} else {
			if (Stream.of(targets).allMatch(o -> o instanceof String)) {
				this.target = parseTarget(Arrays.asList(targets));
			} else {
				FileCollection union = getProject().files();
				for (Object target : targets) {
					union = union.plus(parseTarget(target));
				}
				this.target = union;
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected FileCollection parseTarget(Object target) {
		if (target instanceof FileCollection) {
			return (FileCollection) target;
		} else if (target instanceof String ||
				(target instanceof List && ((List<?>) target).stream().allMatch(o -> o instanceof String))) {
			// since people are likely to do '**/*.md', we want to make sure to exclude folders
			// they don't want to format which will slow down the operation greatly
			File dir = getProject().getProjectDir();
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
			if (target instanceof String) {
				return (FileCollection) getProject().fileTree(dir).include((String) target).exclude(excludes);
			} else {
				// target can only be a List<String> at this point
				return (FileCollection) getProject().fileTree(dir).include((List<String>) target).exclude(excludes);
			}
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
		FormatterStep existing = getExistingStep(newStep.getName());
		if (existing != null) {
			throw new GradleException("Multiple steps with name '" + newStep.getName() + "' for spotless format '" + formatName() + "'");
		}
		steps.add(newStep);
	}

	/** Returns the existing step with the given name, if any. */
	protected @Nullable FormatterStep getExistingStep(String stepName) {
		return steps.stream() //
				.filter(step -> stepName.equals(step.getName())) //
				.findFirst() //
				.orElse(null);
	}

	/** Replaces the given step. */
	protected void replaceStep(FormatterStep replacementStep) {
		FormatterStep existing = getExistingStep(replacementStep.getName());
		if (existing == null) {
			throw new GradleException("Cannot replace step '" + replacementStep.getName() + "' for spotless format '" + formatName() + "' because it hasn't been added yet.");
		}
		int index = steps.indexOf(existing);
		steps.set(index, replacementStep);
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

	abstract class LicenseHeaderConfig {
		String delimiter;
		String yearSeparator = LicenseHeaderStep.defaultYearDelimiter();

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

		abstract FormatterStep createStep();
	}

	class LicenseStringHeaderConfig extends LicenseHeaderConfig {

		private String header;

		LicenseStringHeaderConfig(String delimiter, String header) {
			super(delimiter);
			this.header = Objects.requireNonNull(header, "header");
		}

		FormatterStep createStep() {
			return LicenseHeaderStep.createFromHeader(header, delimiter, yearSeparator);
		}
	}

	class LicenseFileHeaderConfig extends LicenseHeaderConfig {

		private Object headerFile;

		LicenseFileHeaderConfig(String delimiter, Object headerFile) {
			super(delimiter);
			this.headerFile = Objects.requireNonNull(headerFile, "headerFile");
		}

		FormatterStep createStep() {
			return LicenseHeaderStep
					.createFromFile(getProject().file(headerFile), getEncoding(), delimiter,
							yearSeparator);
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

	/** Sets up a format task according to the values in this extension. */
	protected void setupTask(SpotlessTask task) {
		task.setPaddedCell(paddedCell);
		task.setEncoding(getEncoding().name());
		task.setExceptionPolicy(exceptionPolicy);
		task.setTarget(target);
		task.setSteps(steps);
		task.setLineEndingsPolicy(getLineEndings().createPolicy(getProject().getProjectDir(), () -> task.target));
	}

	/** Returns the project that this extension is attached to. */
	protected Project getProject() {
		return root.project;
	}
}
