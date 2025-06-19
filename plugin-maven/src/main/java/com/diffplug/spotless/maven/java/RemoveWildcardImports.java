package com.diffplug.spotless.maven.java;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.java.RemoveWildcardImportsStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class RemoveWildcardImports implements FormatterStepFactory {
	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		return RemoveWildcardImportsStep.create();
	}
}
