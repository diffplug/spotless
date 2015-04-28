package com.diffplug.gradle.spotless;

import groovy.lang.Closure;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.file.FileCollection;

public class SpotlessExtension {
	protected final String name;

	public SpotlessExtension(String name) {
		this.name = name;
	}

	protected List<FormatterStep> steps = new ArrayList<>();

	protected FileCollection toFormat;

	public void setToFormat(FileCollection toFormat) {
		this.toFormat = toFormat;
	}

	public void step(FormatterStep step) {
		steps.add(step);
	}

	public void step(String name, Closure<String> formatter) {
		steps.add(new FormatterStep() {
			@Override
			public String getName() {
				return name;
			}

			@Override
			public String format(String content) throws Exception {
				return formatter.call(content);
			}
		});
	}

	/** Sets up a FormatTask according to the values in this extension. */
	protected void setupTask(FormatTask task) throws Exception {
		task.toFormat = toFormat;
		task.steps = steps;
	}
}
