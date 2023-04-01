/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.maven.antlr4;

import java.util.Set;

import org.apache.maven.project.MavenProject;

import com.diffplug.common.collect.ImmutableSet;
import com.diffplug.spotless.antlr4.Antlr4Defaults;
import com.diffplug.spotless.maven.FormatterFactory;
import com.diffplug.spotless.maven.generic.LicenseHeader;

/**
 * A {@link FormatterFactory} implementation that corresponds to {@code <antlr4>...</antlr4>} configuration element.
 * <p>
 * It defines a formatter for ANTLR4 source files that can execute both language agnostic (e.g. {@link LicenseHeader})
 * and anltr4-specific (e.g. {@link Antlr4Formatter}) steps.
 */
public class Antlr4 extends FormatterFactory {
	@Override
	public Set<String> defaultIncludes(MavenProject project) {
		return ImmutableSet.of(Antlr4Defaults.includes());
	}

	@Override
	public String licenseHeaderDelimiter() {
		return Antlr4Defaults.licenseHeaderDelimiter();
	}

	public void addAntlr4Formatter(Antlr4Formatter antlr4Formatter) {
		addStepFactory(antlr4Formatter);
	}

}
