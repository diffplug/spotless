/*
 * Copyright 2015-2018 DiffPlug
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.diffplug.spotless.cpp.CppDefaults;
import com.diffplug.spotless.maven.FormatterFactory;
import com.diffplug.spotless.maven.generic.LicenseHeader;

/**
 * A {@link FormatterFactory} implementation that corresponds to {@code <cpp>...</cpp>} configuration element.
 * <p>
 * It defines a formatter for java source files that can execute both language agnostic (e.g. {@link LicenseHeader})
 * and cpp-specific (e.g. {@link Eclipse}) steps.
 */
public class Cpp extends FormatterFactory {

	private static final Set<String> DEFAULT_INCLUDES = CppDefaults.FILE_FILTER
			.stream().map(s -> {
				return Arrays.asList("src/main/cpp/" + s, "src/test/cpp/" + s);
			}).flatMap(Collection::stream).collect(Collectors.toSet());

	@Override
	public Set<String> defaultIncludes() {
		return DEFAULT_INCLUDES;
	}

	public void addEclipse(Eclipse eclipse) {
		addStepFactory(eclipse);
	}

	@Override
	public String licenseHeaderDelimiter() {
		return CppDefaults.DELIMITER_EXPR;
	}

}
