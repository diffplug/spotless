/*
 * Copyright 2020-2023 DiffPlug
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
package com.diffplug.spotless.maven.groovy;

import java.util.Set;

import org.apache.maven.project.MavenProject;

import com.diffplug.common.collect.ImmutableSet;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.maven.FormatterFactory;
import com.diffplug.spotless.maven.generic.LicenseHeader;

/**
 * A {@link FormatterFactory} implementation that corresponds to {@code <groovy>...</groovy>} configuration element.
 * <p>
 * It defines a formatter for groovy source files that can execute both language agnostic (e.g. {@link LicenseHeader})
 * and groovy-specific (e.g. {@link GrEclipse}) steps.
 */
public class Groovy extends FormatterFactory {

	private static final Set<String> DEFAULT_INCLUDES = ImmutableSet.of("src/main/groovy/**/*.groovy", "src/test/groovy/**/*.groovy");
	private static final String LICENSE_HEADER_DELIMITER = LicenseHeaderStep.DEFAULT_JAVA_HEADER_DELIMITER;

	@Override
	public Set<String> defaultIncludes(MavenProject project) {
		return DEFAULT_INCLUDES;
	}

	@Override
	public String licenseHeaderDelimiter() {
		return LICENSE_HEADER_DELIMITER;
	}

	public void addGreclipse(GrEclipse greclipse) {
		addStepFactory(greclipse);
	}

	public void addImportOrder(ImportOrder importOrder) {
		addStepFactory(importOrder);
	}

	public void addRemoveSemiColons(RemoveSemiColons removeSemiColons) {
		addStepFactory(removeSemiColons);
	}
}
