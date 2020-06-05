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
package com.diffplug.spotless.maven.css;

import java.util.Set;
import java.util.stream.Collectors;

import com.diffplug.spotless.css.CssDefaults;
import com.diffplug.spotless.maven.FormatterFactory;
import com.diffplug.spotless.maven.generic.LicenseHeader;

/**
 * A {@link FormatterFactory} implementation that corresponds to {@code <css>...</css>} configuration element.
 * <br/>
 * It defines a formatter for CSS source files that can execute both language agnostic (e.g. {@link LicenseHeader})
 * and css-specific (e.g. {@link Eclipse}) steps.
 * <br/>
 * The CSS extension is discontinued. CSS formatters are now part of the generic {@link FormatterFactory}.
 */
@Deprecated
public class Css extends FormatterFactory {

	private static final Set<String> DEFAULT_INCLUDES = CssDefaults.FILE_FILTER
			.stream().map(s -> "src/" + s).collect(Collectors.toSet());

	@Override
	public Set<String> defaultIncludes() {
		return DEFAULT_INCLUDES;
	}

	public void addEclipse(Eclipse eclipse) {
		addStepFactory(eclipse);
	}

	@Override
	public String licenseHeaderDelimiter() {
		return CssDefaults.DELIMITER_EXPR;
	}

}
