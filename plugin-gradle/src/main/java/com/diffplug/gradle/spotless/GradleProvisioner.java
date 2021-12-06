/*
 * Copyright 2016-2021 DiffPlug
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Bundling;

import com.diffplug.common.collect.ImmutableList;
import com.diffplug.spotless.Provisioner;

/** Should be package-private. */
class GradleProvisioner {
	private GradleProvisioner() {}

	static Provisioner newDedupingProvisioner(Project project) {
		return new DedupingProvisioner(project);
	}

	static class DedupingProvisioner implements Provisioner {
		private final Project project;
		private final Map<Request, Set<File>> cache = new HashMap<>();

		DedupingProvisioner(Project project) {
			this.project = project;
		}

		@Override
		public Set<File> provisionWithTransitives(boolean withTransitives, Collection<String> mavenCoordinates) {
			Request req = new Request(withTransitives, mavenCoordinates);
			Set<File> result = cache.get(req);
			if (result != null) {
				return result;
			} else {
				result = cache.get(req);
				if (result != null) {
					return result;
				} else {
					result = forProject(project).provisionWithTransitives(req.withTransitives, req.mavenCoords);
					cache.put(req, result);
					return result;
				}
			}
		}
	}

	private static Provisioner forProject(Project project) {
		Objects.requireNonNull(project);
		return (withTransitives, mavenCoords) -> {
			try {
				Configuration config = project.getConfigurations().create("spotless"
						+ new Request(withTransitives, mavenCoords).hashCode());
				mavenCoords.stream()
						.map(project.getDependencies()::create)
						.forEach(config.getDependencies()::add);
				config.setDescription(mavenCoords.toString());
				config.setTransitive(withTransitives);
				config.attributes(attr -> {
					attr.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
				});
				return config.resolve();
			} catch (Exception e) {
				String projName = project.getPath().substring(1).replace(':', '/');
				if (!projName.isEmpty()) {
					projName = projName + "/";
				}
				logger.log(
						Level.SEVERE,
						"You need to add a repository containing the '" + mavenCoords + "' artifact in '" + projName + "build.gradle'.\n" +
								"E.g.: 'repositories { mavenCentral() }'",
						e);
				throw e;
			}
		};
	}

	private static final Logger logger = Logger.getLogger(GradleProvisioner.class.getName());

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
			builder.append(coords, 1, coords.length() - 1); // strip off []
			if (withTransitives) {
				builder.append(" with transitives");
			} else {
				builder.append(" no transitives");
			}
			return builder.toString();
		}
	}
}
