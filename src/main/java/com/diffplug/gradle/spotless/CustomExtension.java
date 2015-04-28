package com.diffplug.gradle.spotless;

import groovy.lang.Closure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

/** Allows the user to add custom format sets. */
public class CustomExtension {
	protected final String name;
	protected final SpotlessRootExtension root;

	public CustomExtension(String name, SpotlessRootExtension root) {
		this.name = name;
		this.root = root;
		root.addSubExtension(this);
	}

	/** The files that need to be formatted. */
	protected FileCollection toFormat;

	/**
	 * FileCollections pass through raw,
	 * Strings are treated as the 'include' arg to fileTree, with project.rootDir as the dir.
	 * Anything else gets passed to getProject().files(). 
	 */
	public void toFormat(Object toFormat) {
		if (toFormat instanceof FileCollection) {
			this.toFormat = (FileCollection) toFormat;
		} else if (toFormat instanceof String) {
			Map<String, Object> args = new HashMap<>();
			args.put("dir", getProject().getRootDir());
			args.put("include", (String) toFormat);
			this.toFormat = getProject().fileTree(args);
		} else {
			this.toFormat = getProject().files(toFormat);
		}
	}

	/** The steps that need to be added. */
	protected List<FormatterStep> steps = new ArrayList<>();

	/** Lazily adds the given step. */
	public void stepLazy(String name, Throwing.Supplier<Throwing.Function<String, String>> formatterSupplier) {
		for (FormatterStep step : steps) {
			if (step.getName().equals(name)) {
				throw new GradleException("Multiple steps with name '" + name + "' for spotless '" + name + "'");
			}
		}
		steps.add(FormatterStep.createLazy(name, formatterSupplier));
	}

	/** Adds the given step. */
	public void step(String name, Closure<String> formatter) {
		step(name, formatter::call);
	}

	/** Adds the given step. */
	public void step(String name, Throwing.Function<String, String> formatter) {
		stepLazy(name, () -> formatter);
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
