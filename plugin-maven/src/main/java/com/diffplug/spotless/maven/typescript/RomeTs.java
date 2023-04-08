package com.diffplug.spotless.maven.typescript;

import com.diffplug.spotless.maven.rome.AbstractRome;

/**
 * Rome formatter step for TypeScript.
 */
public class RomeTs extends AbstractRome {
	@Override
	protected String getLanguage() {
		return "ts?";
	}
}
