/*
 * Copyright 2016-2024 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.maven.generic;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.biome.BiomeFlavor;

/**
 * Generic Biome formatter step that detects the language of the input file from
 * the file name. It should be specified as a formatter step for a generic
 * {@code <format>}.
 */
public class Biome extends AbstractBiome {
	public Biome() {
		super(BiomeFlavor.BIOME);
	}

	/**
	 * Gets the language (syntax) of the input files to format. When
	 * <code>null</code> or the empty string, the language is detected automatically
	 * from the file name. Currently the following languages are supported by Biome:
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
