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
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.util.GradleVersion;

import com.diffplug.common.base.StringPrinter;
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
		clean.doLast(unused -> {
			// resolution for: https://github.com/diffplug/spotless/issues/243#issuecomment-564323856
			// project.getRootProject() is consistent across every project, so only of one the clears will
			// actually happen (as desired)
			//
			// we use System.identityHashCode() to avoid a memory leak by hanging on to the reference directly
			SpotlessCache.clearOnce(System.identityHashCode(project.getRootProject()));
		});

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

			// the user hasn't specified where to resolve deps and they're going to get a deprecation warning
			if (spotlessExtension.resolveDependenciesIn == null
					&& project != project.getRootProject()
					&& GradleVersion.current().compareTo(GradleProvisioner.STRICT_CONFIG_ACCESS) >= 0) {
				boolean safeToForceProjectLocal = isMavenCentralSuperset(project.getRootProject().getBuildscript().getRepositories()) &&
						isMavenCentralSuperset(project.getRepositories());
				if (safeToForceProjectLocal) {
					// if the root buildscript is just `gradlePluginPortal()` (a superset of mavenCental)
					// and the project repositories are either empty or some superset of mavenCentral
					// then there's no harm in defaulting them to project-local resolution
					spotlessExtension.resolveDependenciesIn = GradleProvisioner.ResolveDependenciesIn.PROJECT;
				} else {
					GradleProvisioner.logger.severe(StringPrinter.buildStringFromLines(
							"The way that Spotless resolves formatter dependencies was deprecated in Gradle 6.0.",
							"To silence this warning, you need to set X: `spotless { resolveDependenciesIn 'X' }`",
							"  'ROOT_BUILDSCRIPT' - current behavior, causes gradle to emit deprecation warnings.",
							"  'PROJECT' *recommended* - uses this project's repositories to resolve dependencies.",
							"                            Only drawback is for the uncommon case that you want your",
							"                            build tools to draw from a different set of respositories than ",
							"                            than your build artifacts.",
							"  'PROJECT_BUILDSCRIPT' - uses this project's buildscript to resolve dependencies.",
							"                          You will probably need to add this:",
							"                            `buildscript { repositories { mavenCentral() } }`",
							"                          to the top of the `build.gradle` in this subproject.",
							"We're hoping to find a better resolution in the future, see issue below for more info",
							"  https://github.com/diffplug/spotless/issues/502"));
				}
			}
		});
	}

	private boolean isMavenCentralSuperset(RepositoryHandler repositories) {
		return repositories.stream().allMatch(SpotlessPlugin::isMavenCentralSuperset);
	}

	private static boolean isMavenCentralSuperset(ArtifactRepository repository) {
		if (!(repository instanceof MavenArtifactRepository)) {
			return false;
		}
		MavenArtifactRepository maven = (MavenArtifactRepository) repository;
		if ("MavenRepo".equals(maven.getName())
				&& "https://repo.maven.apache.org/maven2/".equals(maven.getUrl().toString())) {
			return true;
		} else if ("BintrayJCenter".equals(maven.getName())
				&& "https://jcenter.bintray.com/".equals(maven.getUrl().toString())) {
			return true;
		} else if ("Gradle Central Plugin Repository".equals(maven.getName())
				&& "https://plugins.gradle.org/m2".equals(maven.getUrl().toString())) {
			return true;
		} else {
			return false;
		}
	}

	/** The extension for this plugin. */
	public SpotlessExtension getExtension() {
		return spotlessExtension;
	}

	static String capitalize(String input) {
		return Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}
}
