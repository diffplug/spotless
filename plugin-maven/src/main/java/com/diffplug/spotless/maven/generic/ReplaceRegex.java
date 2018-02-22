/*
 * Copyright 2016 DiffPlug
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
import com.diffplug.spotless.generic.ReplaceRegexStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class ReplaceRegex implements FormatterStepFactory {

	@Parameter
	private String name;

	@Parameter
	private String searchRegex;

	@Parameter
	private String replacement;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		if (name == null || searchRegex == null || replacement == null) {
			throw new IllegalArgumentException("Must specify 'name', 'searchRegex' and 'replacement'.");
		}

		return ReplaceRegexStep.create(name, searchRegex, replacement);
	}
}
