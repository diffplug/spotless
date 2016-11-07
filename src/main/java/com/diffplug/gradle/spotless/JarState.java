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
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import com.diffplug.common.base.Errors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Grabs a jar and its dependencies from maven,
 * and makes it easy to access the collection in
 * a classloader.
 *
 * Serializes the full state of the jar, so it can
 * catch changes in a SNAPSHOT version.
 */
public class JarState implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logging.getLogger(JarState.class);

	private final String mavenCoordinate;
	private final FileSignature fileSignature;

	/*
	 * Transient because not needed to uniquely identify a JarState instance, and also because
	 * Gradle only needs this class to be Serializable so it can compare JarState instances for
	 * incremental builds.
	 */
	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	private final transient Set<File> jars;

	public JarState(String mavenCoordinate, Project project) throws IOException {
		this.mavenCoordinate = Objects.requireNonNull(mavenCoordinate);
		Dependency dep = project.getDependencies().create(mavenCoordinate);
		Configuration config = project.getConfigurations().detachedConfiguration(dep);
		config.setDescription(mavenCoordinate);
		try {
			jars = config.resolve();
		} catch (Exception e) {
			logger.error("You probably need to add a repository containing the `google-java-format` artifact to your buildscript,");
			logger.error("e.g.: repositories { mavenCentral() }");
			throw e;
		}
		fileSignature = new FileSignature(jars);
	}

	@SuppressFBWarnings("DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED")
	public URLClassLoader openClassLoader() {
		URL[] jarUrls = jars.stream().map(Errors.rethrow().wrapFunction(file -> file.toURI().toURL())).toArray(URL[]::new);
		return new URLClassLoader(jarUrls);
	}
}
