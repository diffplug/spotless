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
package com.diffplug.spotless.extra.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Configuration of Maven coordinates */
class MavenCoordinates implements Serializable {

	// Not used, only the serialization output is required to determine whether the object has changed
	private static final long serialVersionUID = 1L;
	private final Set<Coordinate> coordinates;

	/** Creates empty coordinate set */
	MavenCoordinates() {
		this.coordinates = new TreeSet<Coordinate>(new DependencyComparator());
	}

	/** Get all dependency restrictions */
	String[] get() {
		return coordinates.stream().map(c -> c.toString()).toArray(size -> new String[size]);
	}

	/** Add missing coordinates and overwrite existing ones */
	void update(MavenCoordinates other) {
		for (Coordinate coordinate : other.coordinates) {
			update(coordinate);
		}
	}

	/** Add missing coordinates and overwrite existing ones */
	void update(String... coordinates) {
		for (String coordinate : coordinates) {
			update(new Coordinate(coordinate));
		}
	}

	/** Add missing coordinates and overwrite existing ones */
	void update(Coordinate... coordinates) {
		for (Coordinate coordinate : coordinates) {
			this.coordinates.remove(coordinate); //Update existing
			this.coordinates.add(coordinate);
		}
	}

	/**
	 * Add coordinates if missing.
	 * Expecting reference to ASCII content.
	 * Each line must correspond to the {@link Coordinate#FORMAT}.
	 */
	void add(URL coordinates) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(coordinates.openStream(), "UTF-8"));
			try {
				List<Coordinate> additionalCoordinates = reader.lines().map(l -> new Coordinate(l)).collect(Collectors.toList());
				this.coordinates.addAll(additionalCoordinates);
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			throw new UserArgumentException(coordinates, "Cannot access URL for optaining Maven coordinates.", e);
		}
	}

	/** Add coordinates if missing */
	void add(Coordinate... coordinates) {
		for (Coordinate coordinate : coordinates) {
			this.coordinates.add(coordinate);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MavenCoordinates) {
			MavenCoordinates other = (MavenCoordinates) obj;
			//Compare restrictions and not only the dependency
			List<Coordinate> otherRestrictions = new ArrayList<Coordinate>(other.coordinates);
			List<Coordinate> myRestrictions = new ArrayList<Coordinate>(coordinates);
			return otherRestrictions.equals(myRestrictions);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return coordinates.hashCode();
	}

	@Override
	public String toString() {
		return coordinates.stream().map(c -> c.toString()).collect(Collectors.joining(System.lineSeparator()));
	}

	static class Coordinate implements Serializable {
		// Not used, only the serialization output is required to determine whether the object has changed
		private static final long serialVersionUID = 1L;

		// Format of a dependency version restriction (type is always JAR)
		public static final String FORMAT = "<groupId>:<artifactId>[:packaging][:classifier]:<versionRestriction>";
		private final String groupId;
		private final String artifactId;
		private final String packagingOrNull;
		private final String classifierOrNull;
		private final String versionRange;
		private String dependency = null;

		/**
		 * Converts the format {@code <groupId>:<artifactId>[:packaging][:classifier]} dependency and
		 * {@code <versionRestriction>} value
		 */
		Coordinate(String dependency, String version) {
			this(String.format("%s:%s", dependency, version));
		}

		/** Creates version restriction from string. String format is {@value #FORMAT}. */
		Coordinate(String coordinate) {
			if (null == coordinate) {
				throw new UserArgumentException(null, "Maven coordinate");
			}
			LinkedList<String> dependencyParts = new LinkedList<String>(
					Arrays.asList(coordinate.split(":", -1)));
			if (dependencyParts.size() < 3 || dependencyParts.size() > 5) {
				throw new UserArgumentException(dependencyParts,
						String.format("Maven coordinate format '%s', does not allow %d elements.", FORMAT, dependencyParts.size()));
			}
			versionRange = trimAndCheckNotEmpty(coordinate, dependencyParts.pollLast());
			groupId = trimAndCheckNotEmpty(coordinate, dependencyParts.pollFirst());
			artifactId = trimAndCheckNotEmpty(coordinate, dependencyParts.pollFirst());
			packagingOrNull = dependencyParts.isEmpty() ? null : trimAndCheckNotEmpty(coordinate, dependencyParts.pollFirst());
			classifierOrNull = dependencyParts.isEmpty() ? null : trimAndCheckNotEmpty(coordinate, dependencyParts.pollFirst());
		}

		private static String trimAndCheckNotEmpty(String coordinate, String item) {
			item = item.trim();
			if (item.isEmpty()) {
				throw new UserArgumentException(coordinate, String.format("Empty elements are not allowed by format '%s'.", FORMAT));
			}
			return item;
		}

		/** Returns Maven version (range) */
		String getVersionRange() {
			return versionRange;
		}

		/**
		 * Get dependency string in the format
		 * {@code <groupId>:<artifactId>[:packaging][:classifier]}
		 */
		String getDependency() {
			if (null == dependency) {
				StringBuilder dependencyVersionRestriction = new StringBuilder();
				dependencyVersionRestriction.append(groupId).append(':').append(artifactId);
				if (null != packagingOrNull) {
					dependencyVersionRestriction.append(':').append(packagingOrNull);
				}
				if (null != classifierOrNull) {
					dependencyVersionRestriction.append(':').append(classifierOrNull);
				}
				dependency = dependencyVersionRestriction.toString();
			}
			return dependency;
		}

		@Override
		public String toString() {
			return String.format("%s:%s", getDependency(), getVersionRange());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Coordinate) {
				return ((Coordinate) obj).toString().equals(toString());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return toString().hashCode();
		}

	}

	/** Compares dependency, ignores version range */
	private static class DependencyComparator implements Comparator<Coordinate>, Serializable {
		// Not used, only the serialization output is required to determine whether the object has changed
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(Coordinate o1, Coordinate o2) {
			// Objects cannot be null, since M2Configuration prevents it
			return o1.getDependency().compareTo(o2.getDependency());
		}

	}

}
