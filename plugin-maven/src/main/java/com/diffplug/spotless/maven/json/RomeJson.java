package com.diffplug.spotless.maven.json;

import com.diffplug.spotless.maven.rome.AbstractRome;

/**
 * Rome formatter step for JSON.
 */
public class RomeJson extends AbstractRome {
	@Override
	protected String getLanguage() {
		return "json";
	}
}
