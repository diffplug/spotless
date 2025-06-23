package com.diffplug.spotless.java;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.generic.ReplaceRegexStep;

/** Removes any wildcard import statements. */
public final class RemoveWildcardImportsStep {
	private RemoveWildcardImportsStep() {}

	public static FormatterStep create() {
		// Matches lines like 'import foo.*;' or 'import static foo.*;'.
		return ReplaceRegexStep.create(
			"removeWildcardImports",
			"(?m)^import\\s+(?:static\\s+)?[^;\\n]*\\*;\\R?",
			"");
	}
}
