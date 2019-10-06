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
package com.diffplug.gradle.spotless;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.util.GradleVersion;

import com.diffplug.spotless.SpotlessCache;

public class SpotlessPlugin implements Plugin<Project> {
	SpotlessExtension spotlessExtension;

	@Override
	public void apply(Project project) {
		// make sure there's a `clean` task
		project.getPlugins().apply(BasePlugin.class);

		// setup the extension
		spotlessExtension = project.getExtensions().create(SpotlessExtension.EXTENSION, SpotlessExtension.class, project);

		// clear spotless' cache when the user does a clean
		Task clean = project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME);
		clean.doLast(unused -> SpotlessCache.clear());

		project.afterEvaluate(unused -> {
			// Add our check task as a dependency on the global check task
			// getTasks() returns a "live" collection, so this works even if the
			// task doesn't exist at the time this call is made
			if (spotlessExtension.enforceCheck) {
				if (GradleVersion.current().compareTo(SpotlessPluginLegacy.CONFIG_AVOIDANCE_INTRODUCED) >= 0) {
					SpotlessPluginConfigAvoidance.enforceCheck(spotlessExtension, project);
				} else {
					SpotlessPluginLegacy.enforceCheck(spotlessExtension, project);
				}
			}
		});
	}

	/** The extension for this plugin. */
	public SpotlessExtension getExtension() {
		return spotlessExtension;
	}

	static String capitalize(String input) {
		return Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}
}
