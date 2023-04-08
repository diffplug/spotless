package com.diffplug.spotless.maven.generic;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.maven.rome.AbstractRome;

/**
 * Generic Rome formatter step that detects the language of the input file from
 * the file name. It should be specified as a formatter step for a generic
 * {@code <format>}.
 */
public class Rome extends AbstractRome {
	/**
	 * Gets the language (syntax) of the input files to format. When
	 * <code>null</code> or the empty string, the language is detected automatically
	 * from the file name. Currently the following languages are supported by Rome:
	 * <ul>
	 * <ul>
	 * <li>js (JavaScript)</li>
	 * <li>jsx (JavaScript + JSX)</li>
	 * <li>js? (JavaScript or JavaScript + JSX, depending on the file
	 * extension)</li>
	 * <li>ts (TypeScript)</li>
	 * <li>tsx (TypeScript + JSX)</li>
	 * <li>ts? (TypeScript or TypeScript + JSX, depending on the file
	 * extension)</li>
	 * <li>json (JSON)</li>
	 * </ul>
	 * </ul>
	 * 
	 * @return The language of the input files.
	 */
	@Parameter
	private String language;

	@Override
	protected String getLanguage() {
		return language;
	}
}
