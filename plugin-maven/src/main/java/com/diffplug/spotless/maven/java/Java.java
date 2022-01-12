/*
 * Copyright 2016-2022 DiffPlug
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
package com.diffplug.spotless.maven.java;

import java.util.Set;

import com.diffplug.common.collect.ImmutableSet;
import com.diffplug.spotless.maven.FormatterFactory;
import com.diffplug.spotless.maven.generic.LicenseHeader;

/**
 * A {@link FormatterFactory} implementation that corresponds to {@code <java>...</java>} configuration element.
 * <p>
 * It defines a formatter for java source files that can execute both language agnostic (e.g. {@link LicenseHeader})
 * and java-specific (e.g. {@link Eclipse}) steps.
 */
public class Java extends FormatterFactory {

	private static final Set<String> DEFAULT_INCLUDES = ImmutableSet.of("src/main/java/**/*.java", "src/test/java/**/*.java");
	private static final String LICENSE_HEADER_DELIMITER = "package ";

	@Override
	public Set<String> defaultIncludes() {
		return DEFAULT_INCLUDES;
	}

	@Override
	public String licenseHeaderDelimiter() {
		return LICENSE_HEADER_DELIMITER;
	}

	public void addEclipse(Eclipse eclipse) {
		addStepFactory(eclipse);
	}

	public void addGoogleJavaFormat(GoogleJavaFormat googleJavaFormat) {
		addStepFactory(googleJavaFormat);
	}

	public void addImportOrder(ImportOrder importOrder) {
		addStepFactory(importOrder);
	}

	public void addPalantirJavaFormat(PalantirJavaFormat palantirJavaFormat) {
		addStepFactory(palantirJavaFormat);
	}

	public void addRemoveUnusedImports(RemoveUnusedImports removeUnusedImports) {
		addStepFactory(removeUnusedImports);
	}
}
