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
package com.diffplug.spotless.maven.generic;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.generic.ReplaceStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class Replace implements FormatterStepFactory {

	@Parameter
	private String name;

	@Parameter
	private String search;

	@Parameter
	private String replacement;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		if (name == null || search == null) {
			throw new IllegalArgumentException("Must specify 'name' and 'search'.");
		}
		// Use empty string if replacement is not provided. In pom.xml there is no way to specify
		// an empty string as a property value as maven will always trim the value and if it is
		// empty, maven will consider the property as not provided.
		return ReplaceStep.create(name, search, replacement != null ? replacement : "");
	}
}
