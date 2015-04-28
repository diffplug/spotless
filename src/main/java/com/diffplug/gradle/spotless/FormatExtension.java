package com.diffplug.gradle.spotless;

import groovy.lang.Closure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

/** Adds a `spotless{Name}Check` and `spotless{Name}Apply` task. */
public class FormatExtension {
	protected final String name;
	protected final SpotlessExtension root;

	public FormatExtension(String name, SpotlessExtension root) {
		this.name = name;
		this.root = root;
		root.addFormatExtension(this);
	}

	/** The files that need to be formatted. */
	protected FileCollection toFormat;

	/**
	 * FileCollections pass through raw.
	 * Strings are treated as the 'include' arg to fileTree, with project.rootDir as the dir.
	 * List<String> are treates as the 'includes' arg to fileTree, with project.rootDir as the dir.
	 * Anything else gets passed to getProject().files(). 
	 */
	@SuppressWarnings("unchecked")
	public void toFormat(Object toFormat) {
		if (toFormat instanceof FileCollection) {
			this.toFormat = (FileCollection) toFormat;
		} else if (toFormat instanceof String) {
			Map<String, Object> args = new HashMap<>();
			args.put("dir", getProject().getRootDir());
			args.put("include", (String) toFormat);
			this.toFormat = getProject().fileTree(args);
		} else if (toFormat instanceof List && ((List<?>) toFormat).stream().allMatch(e -> e instanceof String)) {
			Map<String, Object> args = new HashMap<>();
			args.put("dir", getProject().getRootDir());
			args.put("includes", (List<String>) toFormat);
			this.toFormat = getProject().fileTree(args);			
		} else {
			this.toFormat = getProject().files(toFormat);
		}
	}

	/** The steps that need to be added. */
	protected List<FormatterStep> steps = new ArrayList<>();

	/**
	 * Adds the given custom step, which is constructed lazily for performance reasons.
	 * 
	 * The resulting function will receive a string with unix-newlines, and it must return a string unix newlines.
	 */
	public void customLazy(String name, Throwing.Supplier<Throwing.Function<String, String>> formatterSupplier) {
		for (FormatterStep step : steps) {
			if (step.getName().equals(name)) {
				throw new GradleException("Multiple steps with name '" + name + "' for spotless '" + name + "'");
			}
		}
		steps.add(FormatterStep.createLazy(name, formatterSupplier));
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
			Pattern pattern = Pattern.compile(regex);
			return raw -> pattern.matcher(raw).replaceAll(replacement);
		});
	}

	/** Removes trailing whitespace. */
	public void trimTrailingWhitespace() {
		customReplaceRegex("trimTrailingWhitespace", "\\s+$", "");
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
		indentWithSpaces(4);
	}

	/**
	 * @param licenseHeader Content that should be at the top of every file
	 * @param delimiter Spotless will look for a line that starts with this to know what the "top" is.
	 */
	public void licenseHeader(String licenseHeader, String delimiter) {
		customLazy(LicenseHeaderStep.NAME, () -> new LicenseHeaderStep(licenseHeader, delimiter)::format);
	}

	/**
	 * @param licenseHeaderFile Content that should be at the top of every file
	 * @param delimiter Spotless will look for a line that starts with this to know what the "top" is.
	 */
	public void licenseHeaderFile(Object licenseHeaderFile, String delimiter) {
		customLazy(LicenseHeaderStep.NAME, () -> new LicenseHeaderStep(getProject().file(licenseHeaderFile), delimiter)::format);
	}

	/** Sets up a FormatTask according to the values in this extension. */
	protected void setupTask(FormatTask task) throws Exception {
		task.toFormat = toFormat;
		task.steps = steps;
	}

	/** Returns the project that this extension is attached to. */
	protected Project getProject() {
		return root.project;
	}
}
