/*
 * Copyright 2015 DiffPlug
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

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.UnionFileCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import groovy.lang.Closure;

/** Adds a `spotless{Name}Check` and `spotless{Name}Apply` task. */
public class FormatExtension {
	final String name;
	private final SpotlessExtension root;
	/** The steps that need to be added. */
	private final List<FormatterStep> steps = new ArrayList<>();
	/** The files that need to be formatted. */
	protected FileCollection target;

	public FormatExtension(final String name, final SpotlessExtension root) {
		this.name = name;
		this.root = root;
		root.addFormatExtension(this);
	}

	/**
	 * FileCollections pass through raw.
	 * Strings are treated as the 'include' arg to fileTree, with project.rootDir as the dir.
	 * List<String> are treates as the 'includes' arg to fileTree, with project.rootDir as the dir.
	 * Anything else gets passed to getProject().files().
	 */
	public void target(final Object... targets) {
		if (targets.length == 0) {
			this.target = getProject().files();
		} else if (targets.length == 1) {
			this.target = parseTarget(targets[0]);
		} else {
			if (areAllObjectsString(targets)) {
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

	private boolean areAllObjectsString(final Object[] targets) {
		for (Object target : targets) {
			if (!(target instanceof String)) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private FileCollection parseTarget(final Object target) {
		if (target instanceof FileCollection) {
			return (FileCollection) target;
		} else if (target instanceof String) {
			Map<String, Object> args = new HashMap<>();
			args.put("dir", getProject().getProjectDir());
			args.put("include", target);
			return getProject().fileTree(args);
		} else if (isListOfString(target)) {
			Map<String, Object> args = new HashMap<>();
			args.put("dir", getProject().getProjectDir());
			args.put("includes", target);
			return getProject().fileTree(args);
		} else {
			return getProject().files(target);
		}
	}

	private boolean isListOfString(final Object target) {
		if (target instanceof List)
			for (Object o : ((List<?>) target)) {
				if (!(o instanceof String)) {
					return false;
				}
			}
		return true;
	}

	/**
	 * Adds the given custom step, which is constructed lazily for performance reasons.
	 * 
	 * The resulting function will receive a string with unix-newlines, and it must return a string unix newlines.
	 */
	protected void customLazy(String name, FormattingOperationSupplier formatterSupplier) {
		for (FormatterStep step : steps) {
			if (step.getName().equals(name)) {
				throw new GradleException("Multiple steps with name '" + name + "' for spotless '" + name + "'");
			}
		}
		steps.add(FormatterStep.createLazy(name, formatterSupplier));
	}

	/** Adds a custom step. Receives a string with unix-newlines, must return a string with unix newlines. */
	private void custom(final String name, Closure<String> formatter) {
		custom(name, formatter);
	}

	/** Adds a custom step. Receives a string with unix-newlines, must return a string with unix newlines. */
	private void custom(final String name, final FormattingOperation formatter) {
		customLazy(name, new FormattingOperationSupplier(formatter));
	}

	/** Highly efficient find-replace char sequence. */
	public void customReplace(final String name, final CharSequence original, final CharSequence after) {
		custom(name, new FormattingOperation() {
			@Override
			public String apply(String raw) {
				return raw.replace(original, after);
			}
		});
	}

	/** Highly efficient find-replace regex. */
	private void customReplaceRegex(final String name, final String regex, final String replacement) {
		customLazy(name, new FormattingOperationSupplier(new FormattingOperation() {
			Pattern pattern;

			@Override
			public String apply(String raw) {
				return pattern.matcher(raw).replaceAll(replacement);
			}

			@Override
			public void init() {
				pattern = Pattern.compile(regex, Pattern.UNIX_LINES | Pattern.MULTILINE);
			}
		}));
	}

	/** Removes trailing whitespace. */
	public void trimTrailingWhitespace() {
		customReplaceRegex("trimTrailingWhitespace", "[ \t]+$", "");
	}

	/** Ensures that files end with a single newline. */
	public void endWithNewline() {
		custom("endWithNewline", new FormattingOperation() {
			@Override
			public String apply(String raw) {
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
			}
		});
	}

	/** Ensures that the files are indented using spaces. */
	private void indentWithSpaces(final int tabToSpaces) {
		customLazy("indentWithSpaces", new FormattingOperationSupplier(new FormattingOperation() {
			IndentStep step;

			@Override
			public String apply(String raw) {
				return step.format(raw);
			}

			@Override
			public void init() {
				step = new IndentStep(IndentStep.Type.SPACE, tabToSpaces);
			}
		}));
	}

	/** Ensures that the files are indented using spaces. */
	public void indentWithSpaces() {
		indentWithSpaces(4);
	}

	/** Ensures that the files are indented using tabs. */
	private void indentWithTabs(final int tabToSpaces) {
		customLazy("indentWithTabs", new FormattingOperationSupplier(new FormattingOperation() {
			IndentStep step;

			@Override
			public String apply(String raw) {
				return step.format(raw);
			}

			@Override
			public void init() {
				step = new IndentStep(IndentStep.Type.TAB, tabToSpaces);
			}
		}));
	}

	/** Ensures that the files are indented using tabs. */
	public void indentWithTabs() {
		indentWithTabs(4);
	}

	/**
	 * @param licenseHeader Content that should be at the top of every file
	 * @param delimiter Spotless will look for a line that starts with this to know what the "top" is.
	 */
	protected void licenseHeader(final String licenseHeader, final String delimiter) {
		customLazy(LicenseHeaderStep.NAME, new FormattingOperationSupplier(new FormattingOperation() {
			LicenseHeaderStep step;

			@Override
			public String apply(String raw) {
				return step.format(raw);
			}

			@Override
			public void init() {
				step = new LicenseHeaderStep(licenseHeader, delimiter);
			}
		}));
	}

	/**
	 * @param licenseHeaderFile Content that should be at the top of every file
	 * @param delimiter Spotless will look for a line that starts with this to know what the "top" is.
	 */
	protected void licenseHeaderFile(final Object licenseHeaderFile, final String delimiter) {
		customLazy(LicenseHeaderStep.NAME, new FormattingOperationSupplier(new FormattingOperation() {
			LicenseHeaderStep step;

			@Override
			public String apply(String raw) {
				return step.format(raw);
			}

			@Override
			public void init() throws IOException {
				step = new LicenseHeaderStep(getProject().file(licenseHeaderFile), delimiter);
			}
		}));
	}

	/** Sets up a FormatTask according to the values in this extension. */
	protected void setupTask(FormatTask task) throws Exception {
		task.target = target;
		task.steps = steps;
	}

	/** Returns the project that this extension is attached to. */
	protected Project getProject() {
		return root.project;
	}
}
