/*
 * Copyright 2016-2024 DiffPlug
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
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;

import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.SpotlessCache;

public class SpotlessPlugin implements Plugin<Project> {
	static final String SPOTLESS_MODERN = "spotlessModern";
	static final String VER_GRADLE_min = "6.1.1";
	static final String VER_GRADLE_javaPluginExtension = "7.1";
	private static final int MINIMUM_JRE = 11;

	@Override
	public void apply(Project project) {
		if (SpotlessPluginRedirect.gradleIsTooOld(project)) {
			throw new GradleException("Spotless requires Gradle " + VER_GRADLE_min + " or newer, this was " + project.getGradle().getGradleVersion());
		}
		if (Jvm.version() < MINIMUM_JRE) {
			throw new GradleException("Spotless requires JRE " + MINIMUM_JRE + " or newer, this was " + JavaVersion.current() + ".\n"
					+ "You can upgrade your build JRE and still compile for older targets, see below\n"
					+ "https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation");
		}
		// if -PspotlessModern=true, then use the modern stuff instead of the legacy stuff
		if (project.getProviders().gradleProperty(SPOTLESS_MODERN).isPresent()) {
			project.getLogger().warn("'spotlessModern' has no effect as of Spotless 5.0, recommend removing it.");
		}
		// make sure there's a `clean` and a `check`
		project.getPlugins().apply(BasePlugin.class);

		// setup the extension
		project.getExtensions().create(SpotlessExtension.class, SpotlessExtension.EXTENSION, SpotlessExtensionImpl.class, project);

		// clear spotless' cache when the user does a clean
		// resolution for: https://github.com/diffplug/spotless/issues/243#issuecomment-564323856
		// project.getRootProject() is consistent across every project, so only of one the clears will
		// actually happen (as desired)
		//
		// we use System.identityHashCode() to avoid a memory leak by hanging on to the reference directly
		int cacheKey = System.identityHashCode(project.getRootProject());
		project.getTasks().named(BasePlugin.CLEAN_TASK_NAME).configure(clean -> clean.doLast(unused -> SpotlessCache.clearOnce(cacheKey)));
	}

	static String capitalize(String input) {
		return Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}
}
