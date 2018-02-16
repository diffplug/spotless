package com.diffplug.spotless.maven.generic;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.generic.ReplaceStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;
import org.apache.maven.plugins.annotations.Parameter;

public class Replace implements FormatterStepFactory {

	@Parameter
	private String name;

	@Parameter
	private String search;

	@Parameter
	private String replacement;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		if (name == null || search == null ||replacement == null) {
			throw new IllegalArgumentException("Must specify 'name', 'search' and 'replacement'.");
		}

		return ReplaceStep.create(name, search, replacement);
	}
}
