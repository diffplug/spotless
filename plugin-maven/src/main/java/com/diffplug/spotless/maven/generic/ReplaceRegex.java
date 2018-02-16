package com.diffplug.spotless.maven.generic;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.generic.ReplaceRegexStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;
import org.apache.maven.plugins.annotations.Parameter;

public class ReplaceRegex implements FormatterStepFactory {

	@Parameter
	private String name;

	@Parameter
	private String searchRegex;

	@Parameter
	private String replacement;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		if (name == null || searchRegex == null ||replacement == null) {
			throw new IllegalArgumentException("Must specify 'name', 'searchRegex' and 'replacement'.");
		}

		return ReplaceRegexStep.create(name, searchRegex, replacement);
	}
}
