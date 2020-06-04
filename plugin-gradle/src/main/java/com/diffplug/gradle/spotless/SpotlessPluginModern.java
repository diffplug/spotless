/*
 * Copyright 2016-2020 DiffPlug
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

import com.diffplug.spotless.SpotlessCache;

public class SpotlessPluginModern implements Plugin<Project> {
	static final String SPOTLESS_MODERN = "spotlessModern";
	static final String MINIMUM_GRADLE = "5.4";

	@Override
	public void apply(Project project) {
		// make sure there's a `clean` task
		project.getPlugins().apply(BasePlugin.class);

		// setup the extension
		project.getExtensions().create(SpotlessExtension.EXTENSION, SpotlessExtensionModern.class, project);

		// clear spotless' cache when the user does a clean
		Task clean = project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME);
		clean.doLast(unused -> {
			// resolution for: https://github.com/diffplug/spotless/issues/243#issuecomment-564323856
			// project.getRootProject() is consistent across every project, so only of one the clears will
			// actually happen (as desired)
			//
			// we use System.identityHashCode() to avoid a memory leak by hanging on to the reference directly
			SpotlessCache.clearOnce(System.identityHashCode(project.getRootProject()));
		});
	}
}
