/*
 * Copyright 2016-2025 DiffPlug
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

import static java.util.Collections.emptySet;

import java.util.Set;

import org.apache.maven.project.MavenProject;

import com.diffplug.spotless.maven.FormatterFactory;

/**
 * A {@link FormatterFactory} implementation that corresponds to {@code <format>...</format>} configuration element.
 * <p>
 * It defines a formatter for custom includes/excludes that executes list of generic, language agnostic steps,
 * like {@link LicenseHeader}.
 */
public class Format extends FormatterFactory {

	@Override
	public Set<String> defaultIncludes(MavenProject project) {
		return emptySet();
	}

	@Override
	public String licenseHeaderDelimiter() {
		// do not specify a default delimiter
		return null;
	}

	/**
	 * Adds a step to this format that format code with the Biome formatter.
	 * @param biome Biome configuration to use.
	 */
	public void addBiome(Biome biome) {
		addStepFactory(biome);
	}
}
