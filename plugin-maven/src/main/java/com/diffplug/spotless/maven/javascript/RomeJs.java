package com.diffplug.spotless.maven.javascript;

import com.diffplug.spotless.maven.rome.AbstractRome;

/**
 * Rome formatter step for JavaScript.
 */
public class RomeJs extends AbstractRome {
	@Override
	protected String getLanguage() {
		return "js?";
	}
}
