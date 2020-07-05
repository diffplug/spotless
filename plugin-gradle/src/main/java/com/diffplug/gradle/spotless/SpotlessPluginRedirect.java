/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.gradle.spotless;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import com.diffplug.common.base.StringPrinter;

public class SpotlessPluginRedirect implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		throw new GradleException(StringPrinter.buildStringFromLines(
				"We have moved from 'com.diffplug.gradle.spotless'",
				"                to 'com.diffplug.spotless'",
				"To migrate:",
				"- Test your build with: id 'com.diffplug.gradle.spotless' version '4.5.1'",
				"- Fix any deprecation warnings (shouldn't be many / any)",
				"- Now you can use:      id 'com.diffplug.spotless' version '5.0.0'",
				"",
				"That's all you really need to know, but as always, there are more details in the changelog:",
				"https://github.com/diffplug/spotless/blob/main/plugin-gradle/CHANGES.md"));
	}
}
