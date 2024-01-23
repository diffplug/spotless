package com.diffplug.spotless.java;

import com.diffplug.spotless.FormatterStep;

public final class IdeaStep {

	private IdeaStep() {}

	public static FormatterStep create() {
		return create(true);
	}

	public static FormatterStep create(boolean withDefaults) {
		return create(true, null);
	}

	public static FormatterStep create(boolean withDefaults,
			String binaryPath) {
		return create(withDefaults, binaryPath, null);
	}

	public static FormatterStep create(boolean withDefaults,
			String binaryPath, String configPath) {
		IdeaFormatterFunc formatterFunc =
				getFormatterFunc(withDefaults, binaryPath, configPath);
		// TODO: make it lazy
		return FormatterStep.createNeverUpToDate("IDEA", formatterFunc);
	}

	private static IdeaFormatterFunc getFormatterFunc(boolean withDefaults,
			String binaryPath, String configPath) {
		if (withDefaults) {
			return IdeaFormatterFunc
					.allowingDefaultsWithCustomBinary(binaryPath, configPath);
		}
		return IdeaFormatterFunc.noDefaultsWithCustomBinary(binaryPath, configPath);
	}

}
