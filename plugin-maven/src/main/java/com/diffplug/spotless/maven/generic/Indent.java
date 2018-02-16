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
import com.diffplug.spotless.generic.IndentStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class Indent implements FormatterStepFactory {

	private static final int DEFAULT_NUM_SPACES_PER_TAB = 4;

	@Parameter
	private boolean withTabs;

	@Parameter
	private boolean withSpaces;

	@Parameter
	private Integer spacesPerTab;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		System.out.println("withTabs: " + withTabs);
		System.out.println("withSpaces: " + withSpaces);
		System.out.println("spacesPerTab: " + spacesPerTab);

		int amountOfSpacesPerTab = DEFAULT_NUM_SPACES_PER_TAB;
		if (spacesPerTab != null) {
			amountOfSpacesPerTab = spacesPerTab;
		}

		if (withSpaces && withTabs) {
			throw new IllegalArgumentException("Must specify exactly one of 'withSpaces' or 'withTabs'.");
		}

		if (!withTabs && !withSpaces) {
			return IndentStep.create(IndentStep.Type.SPACE, amountOfSpacesPerTab);
		} else {
			if (withSpaces) {
				return IndentStep.create(IndentStep.Type.SPACE, amountOfSpacesPerTab);
			} else {
				return IndentStep.create(IndentStep.Type.TAB, amountOfSpacesPerTab);
			}
		}
	}
}
