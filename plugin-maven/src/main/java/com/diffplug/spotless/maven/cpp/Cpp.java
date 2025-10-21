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
package com.diffplug.spotless.maven.cpp;

import static java.util.Collections.emptySet;

import com.diffplug.spotless.cpp.CppDefaults;
import com.diffplug.spotless.maven.FormatterFactory;
import com.diffplug.spotless.maven.generic.LicenseHeader;
import java.util.Collections;
import java.util.Set;
import org.apache.maven.project.MavenProject;

/**
 * A {@link FormatterFactory} implementation that corresponds to {@code <cpp>...</cpp>} configuration element.
 * <p>
 * It defines a formatter for java source files that can execute both language agnostic (e.g. {@link LicenseHeader})
 * and cpp-specific (e.g. {@link EclipseCdt}) steps.
 */
public class Cpp extends FormatterFactory {
	@Override
	public Set<String> defaultIncludes(MavenProject project) {
		return emptySet();
	}

	public void addEclipseCdt(EclipseCdt eclipse) {
		addStepFactory(eclipse);
	}

	public void addClangFormat(Clang clang) {
		addStepFactory(clang);
	}

	@Override
	public String licenseHeaderDelimiter() {
		return CppDefaults.DELIMITER_EXPR;
	}

}
