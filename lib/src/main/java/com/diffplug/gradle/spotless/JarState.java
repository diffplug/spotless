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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	private static final Logger logger = Logger.getLogger(JarState.class.getName());

	@SuppressWarnings("unused")
	private final String mavenCoordinate;
	@SuppressWarnings("unused")
	private final FileSignature fileSignature;

	/*
	 * Transient because not needed to uniquely identify a JarState instance, and also because
	 * Gradle only needs this class to be Serializable so it can compare JarState instances for
	 * incremental builds.
	 */
	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	private final transient Set<File> jars;

	public JarState(String mavenCoordinate, Provisioner provisioner) throws IOException {
		this.mavenCoordinate = Objects.requireNonNull(mavenCoordinate);
		try {
			jars = provisioner.provisionWithDependencies(mavenCoordinate);
			if (jars.isEmpty()) {
				throw new NoSuchElementException("Resolved to an empty result.");
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "You probably need to add a repository containing the `" + mavenCoordinate + "` artifact to your buildscript,");
			logger.log(Level.SEVERE, "e.g.: repositories { mavenCentral() }");
			throw e;
		}
		fileSignature = new FileSignature(jars);
	}

	private URL[] jarUrls() {
		return jars.stream().map(Errors.rethrow().wrapFunction(file -> file.toURI().toURL())).toArray(URL[]::new);
	}

	@SuppressFBWarnings("DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED")
	public URLClassLoader openClassLoader() {
		return new URLClassLoader(jarUrls());
	}

	@SuppressFBWarnings("DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED")
	public URLClassLoader openIsolatedClassLoader() {
		return new URLClassLoader(jarUrls(), null);
	}
}
