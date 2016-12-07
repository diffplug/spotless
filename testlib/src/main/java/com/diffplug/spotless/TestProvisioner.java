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
package com.diffplug.spotless;

import java.io.File;
import java.util.function.Consumer;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.testfixtures.ProjectBuilder;

import com.diffplug.common.base.StandardSystemProperty;

public class TestProvisioner {
	/**
	 * Creates a Provisioner for the given repositories.
	 *
	 * The first time a project is created, there are ~7 seconds of configuration
	 * which will go away for all subsequent runs.
	 *
	 * Every call to resolve will take about 1 second, even when all artifacts are resolved.
	 */
	private static Provisioner createWithRepositories(Consumer<RepositoryHandler> repoConfig) {
		// use the default gradle home directory to ensure that files are always resolved to the same location
		File gradleHome = new File(StandardSystemProperty.USER_DIR.value() + "/.gradle");
		Project project = ProjectBuilder.builder()
				.withGradleUserHomeDir(gradleHome)
				.build();
		repoConfig.accept(project.getRepositories());
		// temporary, just while spotless-ext-eclipse isn't in mavenCentral
		project.getRepositories().maven(mvn -> mvn.setUrl("https://dl.bintray.com/diffplug/opensource"));
		return mavenCoords -> {
			Dependency[] deps = mavenCoords.stream()
					.map(project.getDependencies()::create)
					.toArray(Dependency[]::new);
			Configuration config = project.getConfigurations().detachedConfiguration(deps);
			config.setDescription(mavenCoords.toString());
			return config.resolve();
		};
	}

	/** Creates a Provisioner for the jcenter repo. */
	public static Provisioner jcenter() {
		return createWithRepositories(repo -> repo.jcenter());
	}

	/** Creates a Provisioner for the mavenCentral repo. */
	public static Provisioner mavenCentral() {
		return createWithRepositories(repo -> repo.mavenCentral());
	}
}
