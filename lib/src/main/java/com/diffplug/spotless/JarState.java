/*
 * Copyright 2016-2023 DiffPlug
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
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
	private final FileSignature fileSignature;

	private JarState(FileSignature fileSignature) {
		this.fileSignature = fileSignature;
	}

	/** Provisions the given maven coordinate and its transitive dependencies. */
	public static JarState from(Object dependency, Provisioner provisioner) throws IOException {
		final Collection<?> mavenCoordinates;
		if (dependency instanceof Collection) {
			mavenCoordinates = (Collection<?>) dependency;
		} else {
			mavenCoordinates = Collections.singletonList(dependency);
		}
		return from(mavenCoordinates, provisioner);
	}

	/** Provisions the given maven coordinates and their transitive dependencies. */
	public static JarState from(Collection<?> dependencies, Provisioner provisioner) throws IOException {
		return from(dependencies, provisioner, true);
	}

	public static JarState from(Collection<?> dependencies, Provisioner provisioner, boolean withTransitives) throws IOException {
		Objects.requireNonNull(dependencies, "dependencies");
		Objects.requireNonNull(provisioner, "provisioner");
		Set<File> jars = provisioner.provisionWithTransitives(withTransitives, dependencies);
		if (jars.isEmpty()) {
			throw new NoSuchElementException("Resolved to an empty result: " + dependencies.stream().map(Object::toString).collect(Collectors.joining(", ")));
		}
		FileSignature fileSignature = FileSignature.signAsSet(jars);
		return new JarState(fileSignature);
	}

	/** Wraps the given collection of a files as a JarState, maintaining the order in the Collection. */
	public static JarState preserveOrder(Collection<File> jars) throws IOException {
		FileSignature fileSignature = FileSignature.signAsList(jars);
		return new JarState(fileSignature);
	}

	URL[] jarUrls() {
		return fileSignature.files().stream().map(File::toURI).map(ThrowingEx.wrap(URI::toURL)).toArray(URL[]::new);
	}

	/**
	 * Returns a classloader containing the only jars in this JarState.
	 * Look-up of classes in the {@code org.slf4j} package
	 * are not taken from the JarState, but instead redirected to the class loader of this class to enable
	 * passthrough logging.
	 * <br/>
	 * The lifetime of the underlying cacheloader is controlled by {@link SpotlessCache}.
	 */
	public ClassLoader getClassLoader() {
		return SpotlessCache.instance().classloader(this);
	}

	/**
	 * Returns a classloader containing the only jars in this JarState.
	 * Look-up of classes in the {@code org.slf4j} package
	 * are not taken from the JarState, but instead redirected to the class loader of this class to enable
	 * passthrough logging.
	 * <br/>
	 * The lifetime of the underlying cacheloader is controlled by {@link SpotlessCache}.
	 */
	public ClassLoader getClassLoader(Serializable key) {
		return SpotlessCache.instance().classloader(key, this);
	}
}
