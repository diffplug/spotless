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

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.UnionFileCollection;

import com.diffplug.common.base.Throwing;

import groovy.lang.Closure;

/** Adds a `spotless{Name}Check` and `spotless{Name}Apply` task. */
public class FormatExtension {
	protected final String name;
	protected final SpotlessExtension root;

	public FormatExtension(String name, SpotlessExtension root) {
		this.name = name;
		this.root = root;
		root.addFormatExtension(this);
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
		this.lineEndings = lineEndings;
	}

	Charset encoding;

	/** Returns the encoding to use (defaults to {@link SpotlessExtension#getEncoding()}. */
	public Charset getEncoding() {
		return encoding == null ? root.getEncoding() : encoding;
	}

	/** Sets the encoding to use (defaults to {@link SpotlessExtension#getEncoding()}. */
	public void setEncoding(String name) {
		setEncoding(Charset.forName(name));
	}

	/** Sets the encoding to use (defaults to {@link SpotlessExtension#getEncoding()}. */
	public void setEncoding(Charset charset) {
		encoding = Objects.requireNonNull(charset);
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
		if (targets.length == 0) {
			this.target = getProject().files();
		} else if (targets.length == 1) {
			this.target = parseTarget(targets[0]);
		} else {
			if (Stream.of(targets).allMatch(o -> o instanceof String)) {
				this.target = parseTarget(Arrays.asList(targets));
			} else {
				UnionFileCollection union = new UnionFileCollection();
				for (Object target : targets) {
					union.add(parseTarget(target));
				}
				this.target = union;
			}
		}
	}

	protected FileCollection parseTarget(Object target) {
		if (target instanceof FileCollection) {
			return (FileCollection) target;
		} else if (target instanceof String ||
				(target instanceof List && ((List<?>) target).stream().allMatch(o -> o instanceof String))) {
			File dir = getProject().getProjectDir();
			Iterable<String> excludes = Arrays.asList(
					getProject().getBuildDir().toString() + File.separatorChar + "**",
					getProject().getProjectDir().toString() + File.separatorChar + ".gradle" + File.separatorChar + "**");
			if (target instanceof String) {
				return (FileCollection) getProject().fileTree(dir).include((String) target).exclude(excludes);
			} else {
				// target can only be a List<String> at this point
				return (FileCollection) getProject().fileTree(dir).include(castToStringList(target)).exclude(excludes);
			}
		} else {
			return getProject().files(target);
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> castToStringList(Object target) {
		return (List<String>) target;
	}

	/** The steps that need to be added. */
	protected List<FormatterStep> steps = new ArrayList<>();

	/** Adds a new step. */
	public void addStep(FormatterStep newStep) {
		for (FormatterStep step : steps) {
			if (step.getName().equals(name)) {
				throw new GradleException("Multiple steps with name '" + name + "' for spotless '" + name + "'");
			}
		}
		steps.add(newStep);
	}

	/**
	 * Spotless tracks what files have changed from run to run, so that it can run faster
	 * by only checking files which have changed.
	 *
	 * If you have changed a custom function, then you must increment this number so
	 * that spotless knows it needs to rerun the format check.  This is not necessary
	 * if you don't use any custom functions.
	 *
	 * If you use a custom function and don't call bumpThisNumberIfACustomRuleChanges, then spotless
	 * cannot tell if you have changed the rules, and will be forced to always recheck all files.
	 */
	public void bumpThisNumberIfACustomRuleChanges(int number) {
		globalKey = number;
	}

	private Serializable globalKey = new NeverUpToDateBetweenRuns();

	static class NeverUpToDateBetweenRuns extends LazyForwardingEquality<Integer> {
		private static final long serialVersionUID = 1L;
		private static final Random RANDOM = new Random();

		@Override
		protected Integer calculateKey() throws Exception {
			return RANDOM.nextInt();
		}
	}

	/**
	 * Adds the given custom step, which is constructed lazily for performance reasons.
	 *
	 * The resulting function will receive a string with unix-newlines, and it must return a string unix newlines.
	 *
	 * If you're getting errors about `closure cannot be cast to com.diffplug.common.base.Throwing$Function`, then use
	 * {@link #customLazyGroovy(String, com.diffplug.common.base.Throwing.Supplier)}.
	 */
	public void customLazy(String name, Throwing.Supplier<Throwing.Function<String, String>> formatterSupplier) {
		addStep(FormatterStep.createLazy(name, () -> globalKey, (unusedKey) -> formatterSupplier.get()));
	}

	/** Same as {@link #customLazy(String, com.diffplug.common.base.Throwing.Supplier)}, but for Groovy closures. */
	public void customLazyGroovy(String name, Throwing.Supplier<Closure<String>> formatterSupplier) {
		customLazy(name, () -> formatterSupplier.get()::call);
	}

	/** Adds a custom step. Receives a string with unix-newlines, must return a string with unix newlines. */
	public void custom(String name, Closure<String> formatter) {
		custom(name, formatter::call);
	}

	/** Adds a custom step. Receives a string with unix-newlines, must return a string with unix newlines. */
	public void custom(String name, Throwing.Function<String, String> formatter) {
		customLazy(name, () -> formatter);
	}

	/** Highly efficient find-replace char sequence. */
	public void customReplace(String name, CharSequence original, CharSequence after) {
		custom(name, raw -> raw.replace(original, after));
	}

	/** Highly efficient find-replace regex. */
	public void customReplaceRegex(String name, String regex, String replacement) {
		customLazy(name, () -> {
			Pattern pattern = Pattern.compile(regex, Pattern.UNIX_LINES | Pattern.MULTILINE);
			return raw -> pattern.matcher(raw).replaceAll(replacement);
		});
	}

	/** Removes trailing whitespace. */
	public void trimTrailingWhitespace() {
		customReplaceRegex("trimTrailingWhitespace", "[ \t]+$", "");
	}

	/** Ensures that files end with a single newline. */
	public void endWithNewline() {
		custom("endWithNewline", raw -> {
			// simplifies the logic below if we can assume length > 0
			if (raw.isEmpty()) {
				return "\n";
			}

			// find the last character which has real content
			int lastContentCharacter = raw.length() - 1;
			char c;
			while (lastContentCharacter >= 0) {
				c = raw.charAt(lastContentCharacter);
				if (c == '\n' || c == '\t' || c == ' ') {
					--lastContentCharacter;
				} else {
					break;
				}
			}

			// if it's already clean, no need to create another string
			if (lastContentCharacter == -1) {
				return "\n";
			} else if (lastContentCharacter == raw.length() - 2 && raw.charAt(raw.length() - 1) == '\n') {
				return raw;
			} else {
				StringBuilder builder = new StringBuilder(lastContentCharacter + 2);
				builder.append(raw, 0, lastContentCharacter + 1);
				builder.append('\n');
				return builder.toString();
			}
		});
	}

	/** Ensures that the files are indented using spaces. */
	public void indentWithSpaces(int tabToSpaces) {
		customLazy("indentWithSpaces", () -> new IndentStep(IndentStep.Type.SPACE, tabToSpaces)::format);
	}

	/** Ensures that the files are indented using spaces. */
	public void indentWithSpaces() {
		indentWithSpaces(4);
	}

	/** Ensures that the files are indented using tabs. */
	public void indentWithTabs(int tabToSpaces) {
		customLazy("indentWithTabs", () -> new IndentStep(IndentStep.Type.TAB, tabToSpaces)::format);
	}

	/** Ensures that the files are indented using tabs. */
	public void indentWithTabs() {
		indentWithTabs(4);
	}

	/**
	 * @param licenseHeader
	 *            Content that should be at the top of every file
	 * @param delimiter
	 *            Spotless will look for a line that starts with this to know what the "top" is.
	 */
	public void licenseHeader(String licenseHeader, String delimiter) {
		addStep(FormatterStep.create(LicenseHeaderStep.NAME,
				new LicenseHeaderStep(licenseHeader, delimiter),
				step -> step::format));
	}

	/**
	 * @param licenseHeaderFile
	 *            Content that should be at the top of every file
	 * @param delimiter
	 *            Spotless will look for a line that starts with this to know what the "top" is.
	 */
	public void licenseHeaderFile(Object licenseHeaderFile, String delimiter) {
		addStep(FormatterStep.createLazy(LicenseHeaderStep.NAME,
				() -> new LicenseHeaderStep(getProject().file(licenseHeaderFile), getEncoding(), delimiter),
				step -> step::format));
	}

	/** Sets up a format task according to the values in this extension. */
	protected void setupTask(BaseFormatTask task) {
		task.setPaddedCell(paddedCell);
		task.setEncoding(getEncoding().name());
		task.setTarget(target);
		task.setSteps(steps);
		task.setLineEndingsPolicy(getLineEndings().createPolicy(getProject().getProjectDir(), () -> task.target));
	}

	/** Returns the project that this extension is attached to. */
	protected Project getProject() {
		return root.project;
	}
}
