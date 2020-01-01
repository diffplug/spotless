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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.gradle.api.Project;
import org.gradle.util.GradleVersion;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;

class RegisterDependenciesInRoot {
	static final GradleVersion STRICT_CONFIG_ACCESS_WARNING = GradleVersion.version("6.0");

	static final String ENABLE_KEY = "spotless_register_dependencies_in_root";
	static final String TASK_NAME = "spotlessRegisterDependencies";

	/** Determines if the "spotless_register_dependencies_in_root" mode is enabled. */
	public static boolean isEnabled(Project project) {
		Object enable = project.getRootProject().findProperty(ENABLE_KEY);
		if (Boolean.TRUE.equals(enable) || "true".equals(enable)) {
			return true;
		}
		boolean onlyOneProjectInEntireBuild = project == project.getRootProject()
				&& project.getChildProjects().isEmpty();
		if (onlyOneProjectInEntireBuild) {
			return false;
		}
		if (GradleVersion.current().compareTo(STRICT_CONFIG_ACCESS_WARNING) >= 0) {
			return true;
		}
		return false;
	}

	/** Models a request to the provisioner. */
	private static class Request {
		final boolean withTransitives;
		final ImmutableList<String> mavenCoords;

		public Request(boolean withTransitives, Collection<String> mavenCoords) {
			this.withTransitives = withTransitives;
			this.mavenCoords = ImmutableList.copyOf(mavenCoords);
		}

		@Override
		public int hashCode() {
			return withTransitives ? mavenCoords.hashCode() : ~mavenCoords.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof Request) {
				Request o = (Request) obj;
				return o.withTransitives == withTransitives && o.mavenCoords.equals(mavenCoords);
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			String coords = mavenCoords.toString();
			StringBuilder builder = new StringBuilder();
			builder.append(coords.substring(1, coords.length() - 1)); // strip off []
			if (withTransitives) {
				builder.append(" with transitives");
			} else {
				builder.append(" no transitives");
			}
			return builder.toString();
		}
	}

	/** The provisioner used for all sub-projects. */
	static class SubProvisioner implements Provisioner {
		private final RootProvisioner root;
		private final Project project;

		public SubProvisioner(RootProvisioner root, Project project) {
			this.root = Objects.requireNonNull(root);
			this.project = Objects.requireNonNull(project);
		}

		@Override
		public Set<File> provisionWithTransitives(boolean withTransitives, Collection<String> mavenCoordinates) {
			return root.provisionForSub(project, withTransitives, mavenCoordinates);
		}
	}

	/** The provisioner used for the root project. */
	static class RootProvisioner implements Provisioner {
		private final Project rootProject;

		RootProvisioner(Project rootProject) {
			Preconditions.checkArgument(rootProject == rootProject.getRootProject());
			this.rootProject = rootProject;
		}

		@Override
		public Set<File> provisionWithTransitives(boolean withTransitives, Collection<String> mavenCoordinates) {
			return doProvision(new Request(withTransitives, mavenCoordinates), true);
		}

		private Map<Request, Set<File>> cache = new HashMap<>();

		/** Guaranteed to return non-null for internal requests, but might return null for an external request which isn't cached already. */
		private synchronized @Nullable Set<File> doProvision(Request req, boolean isRoot) {
			Set<File> result = cache.get(req);
			if (result != null) {
				return result;
			}
			if (isRoot) {
				result = GradleProvisioner.fromRootBuildscript(rootProject).provisionWithTransitives(req.withTransitives, req.mavenCoords);
				cache.put(req, result);
				return result;
			} else {
				return null;
			}
		}

		private Set<File> provisionForSub(Project project, boolean withTransitives, Collection<String> mavenCoordinates) {
			Request req = new Request(withTransitives, mavenCoordinates);
			Set<File> result = doProvision(req, false);
			if (result != null) {
				return result;
			} else {
				// if it wasn't cached, complain loudly and use the crappy workaround
				GradleProvisioner.logger.severe(warningMsg(req));
				return GradleProvisioner.fromRootBuildscript(project).provisionWithTransitives(withTransitives, mavenCoordinates);
			}
		}
	}

	private static String warningMsg(Request requestedDeps) {
		FormatterStep beingResolved = FormatterStep.lazyStepBeingResolvedInThisThread();
		return String.format(
				"This subproject is using a formatter that was not used in the root project.  To enable%n" +
						"performance optimzations (and avoid Gradle 7 deprecation warnings), you must declare%n" +
						"all of your formatters within the root project.  For example, if your subproject has%n" +
						"a `java {}` block but your root project does not, just add a matching `java {}` block to%n" +
						"your root project.  If you want to make it clear that it is intentional that the target%n" +
						"is empty, you can do this in your root build.gradle:%n" +
						"%n" +
						"  spotless {%n" +
						"    java {%n" +
						"      targetEmptyForDeclaration()%n" +
						"      [...same steps as subproject...]%n" +
						"    }%n" +
						"  }%n" +
						"%n" +
						"To help you figure out which block is missing, the step you are missing is%n" +
						"  step name: %s%n" +
						"  requested: %s%n",
				beingResolved == null ? "(unknown)" : beingResolved.getName(),
				requestedDeps);
	}
}
