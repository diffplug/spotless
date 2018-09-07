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
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Grabs a jar and its dependencies from maven,
 * and makes it easy to access the collection in
 * a classloader.
 * <p>
 * Serializes the full state of the jar, so it can
 * catch changes in a SNAPSHOT version.
 */
public final class JarState implements Serializable {
	private static final long serialVersionUID = 1L;

	private final Set<String> mavenCoordinates;
	@SuppressWarnings("unused")
	private final FileSignature fileSignature;

	/*
	 * Transient because not needed to uniquely identify a JarState instance, and also because
	 * Gradle only needs this class to be Serializable so it can compare JarState instances for
	 * incremental builds.
	 */
	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	private final transient Set<File> jars;

	public JarState(String mavenCoordinate, FileSignature fileSignature, Set<File> jars) {
		this(Arrays.asList(mavenCoordinate), fileSignature, jars);
	}

	public JarState(Collection<String> mavenCoordinates, FileSignature fileSignature, Set<File> jars) {
		this.mavenCoordinates = new TreeSet<String>(mavenCoordinates);
		this.fileSignature = fileSignature;
		this.jars = jars;
	}

	public static JarState from(String mavenCoordinate, Provisioner provisioner) throws IOException {
		return from(Collections.singletonList(mavenCoordinate), provisioner);
	}

	public static JarState from(Collection<String> mavenCoordinates, Provisioner provisioner) throws IOException {
		Objects.requireNonNull(provisioner, "provisioner");
		Objects.requireNonNull(mavenCoordinates, "mavenCoordinates");
		Set<File> jars = provisioner.provisionWithDependencies(mavenCoordinates);
		if (jars.isEmpty()) {
			throw new NoSuchElementException("Resolved to an empty result: " + mavenCoordinates.stream().collect(Collectors.joining(", ")));
		}
		FileSignature fileSignature = FileSignature.signAsSet(jars);
		return new JarState(mavenCoordinates, fileSignature, jars);
	}

	URL[] jarUrls() {
		return jars.stream().map(File::toURI).map(ThrowingEx.wrap(URI::toURL)).toArray(URL[]::new);
	}

	/**
	 * Returns a classloader containing only the jars in this JarState.
	 * <p>
	 * The lifetime of the underlying cacheloader is controlled by {@link SpotlessCache}.
	 */
	public ClassLoader getClassLoader() {
		return SpotlessCache.instance().classloader(this);
	}

	/**
	 * Returns a classloader containing only the jars in this JarState.
	 *
	 * The lifetime of the underlying cacheloader is controlled by {@link SpotlessCache}.
	 */
	public ClassLoader getClassLoader(Serializable key) {
		return SpotlessCache.instance().classloader(key, this);
	}

	/** Returns unmodifiable view on sorted Maven coordinates */
	public Set<String> getMavenCoordinates() {
		return Collections.unmodifiableSet(mavenCoordinates);
	}
}
