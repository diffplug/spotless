/*
 * Copyright 2016-2026 DiffPlug
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
package com.diffplug.spotless.extra.java;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import dev.equo.solstice.p2.P2ClientCache;
import dev.equo.solstice.p2.P2Model;
import dev.equo.solstice.p2.P2QueryCache;
import dev.equo.solstice.p2.P2QueryResult;

/**
 * Executable class for validating or updating embedded JDT lockfiles from Eclipse P2 metadata.
 * <p>
 * Intended for manual execution via the following entrypoints:
 * <pre>
 * - {@link EclipseJdtLockfileMetadataTool.Verify#main(String[])}
 * - {@link EclipseJdtLockfileMetadataTool.Update#main(String[])}
 * </pre>
 */
public class EclipseJdtLockfileMetadataTool {

	/**
	 * Every Eclipse release from {@value #OLDEST_MINOR}, inclusive, through the version Spotless
	 * defaults to gets an embedded lockfile, with no gaps.
	 * <p>
	 * Contiguity is the point: a user on any supported version should get the same resolution path
	 * as a user on any other. Sampling versions instead would make the P2 fallback -- and the
	 * behavior differences that come with it -- depend on which release you happen to pin.
	 * <p>
	 * Eclipse's switch from OSGi ranges to resolved concrete versions in its Maven metadata
	 * (<a href="https://github.com/eclipse-platform/eclipse.platform.releng/issues/135">eclipse-platform/eclipse.platform.releng#135</a>,
	 * effective 4.26) deliberately does <em>not</em> factor in. Full explicit lockfiles can be
	 * produced for any target by taking Solstice's P2-resolved Maven coordinates directly, which
	 * are already fully version-resolved under either style.
	 */
	private static final int OLDEST_MINOR = 9;

	static List<String> targetVersions() {
		String defaultVersion = EclipseJdtFormatterStep.defaultVersion();
		int newestMinor = minorOf(defaultVersion);
		if (newestMinor < OLDEST_MINOR) {
			throw new IllegalStateException("Default version " + defaultVersion + " is older than 4." + OLDEST_MINOR);
		}
		List<String> versions = new ArrayList<>();
		for (int minor = OLDEST_MINOR; minor <= newestMinor; minor++) {
			versions.add("4." + minor);
		}
		return versions;
	}

	private static int minorOf(String version) {
		if (!version.startsWith("4.")) {
			throw new IllegalArgumentException("Expected 4.x but got " + version);
		}
		return Integer.parseInt(version.substring(2).split("\\.")[0]);
	}

	/**
	 * Verifies the JDT lockfiles at {@link #targetVersions()} against Eclipse P2 metadata.
	 * <p>
	 * Exits non-zero if any lockfile is missing or out of date, so this can gate CI.
	 */
	public static class Verify {

		public static void main(String[] args) {
			if (run(false) > 0) {
				System.exit(1);
			}
		}

	}

	/**
	 * Updates the JDT lockfiles at {@link #targetVersions()} from Eclipse P2 metadata.
	 * <p>
	 * Missing lockfiles will be created.
	 */
	public static class Update {

		public static void main(String[] args) {
			if (run(true) > 0) {
				System.exit(1);
			}
		}

	}

	private static final String JDT_ID = "org.eclipse.jdt.core";
	private static final String JDT_MAVEN_KEY = "org.eclipse.jdt:org.eclipse.jdt.core";
	private static final List<String> ECLIPSE_UPDATE_BASE_URLS = List.of(
			"https://download.eclipse.org/eclipse/updates/",
			"https://archive.eclipse.org/eclipse/updates/");

	/** Returns the number of failures. */
	private static int run(boolean update) {
		Path lockfileDir = lockfileDir();
		Map<String, Path> lockfilesByVersion = targetLockfiles(lockfileDir, targetVersions());

		int checked = 0;
		int failures = 0;
		for (Map.Entry<String, Path> entry : lockfilesByVersion.entrySet()) {
			checked++;
			String eclipseVersion = entry.getKey();
			Path lockfilePath = entry.getValue();
			try {
				List<String> allCoordinates = resolveTransitiveClosure(eclipseVersion);
				String rootCoordinate = allCoordinates.get(0);
				if (update) {
					Files.writeString(lockfilePath, lockfileContent(eclipseVersion, allCoordinates), StandardCharsets.UTF_8);
					System.out.println("WROTE   v" + eclipseVersion + " -> " + allCoordinates.size() + " artifact(s), root: " + rootCoordinate);
				} else {
					if (!Files.exists(lockfilePath)) {
						System.err.println("MISSING v" + eclipseVersion + " -> " + lockfilePath + " (expected root: " + rootCoordinate + ")");
						failures++;
						continue;
					}
					List<String> actualCoordinates = lockfileCoordinates(lockfilePath);
					String mismatch = mismatchMessage(eclipseVersion, allCoordinates, actualCoordinates);
					if (mismatch != null) {
						System.err.println(mismatch);
						failures++;
						continue;
					}
					System.out.println("OK      v" + eclipseVersion + " -> " + rootCoordinate + " (" + allCoordinates.size() + " coordinates)");
				}
			} catch (Exception e) {
				System.err.println("ERROR   v" + eclipseVersion + " -> " + e.getMessage());
				failures++;
			}
		}
		if (update) {
			System.out.println("Updated " + checked + " lockfile(s) with " + failures + " issue(s).");
		} else {
			System.out.println("Verified " + checked + " lockfile(s) with " + failures + " issue(s).");
		}
		return failures;
	}

	private static Map<String, Path> targetLockfiles(Path lockfileDir, List<String> versions) {
		Map<String, Path> targets = new LinkedHashMap<>();
		for (String version : versions) {
			targets.put(version, lockfileDir.resolve("v" + version + ".lockfile"));
		}
		return targets;
	}

	private static String lockfileContent(String eclipseVersion, List<String> coordinates) {
		String prefix = "# Spotless formatter based on Eclipse-JDT " + eclipseVersion + "\n";
		StringJoiner joiner = new StringJoiner("\n", prefix, "\n");
		coordinates.forEach(joiner::add);
		return joiner.toString();
	}

	private static List<String> lockfileCoordinates(Path lockfilePath) throws IOException {
		try (var lines = Files.lines(lockfilePath, StandardCharsets.UTF_8)) {
			List<String> coordinates = lines.map(String::trim)
					.filter(line -> !line.isEmpty())
					.filter(line -> !line.startsWith("#"))
					.toList();
			if (coordinates.isEmpty()) {
				throw new IllegalArgumentException("No dependency coordinate found in " + lockfilePath);
			}
			return coordinates;
		}
	}

	static String mismatchMessage(String eclipseVersion, List<String> expectedCoordinates, List<String> actualCoordinates) {
		if (expectedCoordinates.equals(actualCoordinates)) {
			return null;
		}
		String expectedRoot = expectedCoordinates.isEmpty() ? "<empty>" : expectedCoordinates.get(0);
		String actualRoot = actualCoordinates.isEmpty() ? "<empty>" : actualCoordinates.get(0);
		return "MISMATCH v" + eclipseVersion + " -> expected root: " + expectedRoot + " (" + expectedCoordinates.size()
				+ " coordinates), actual: " + actualRoot + " (" + actualCoordinates.size() + " coordinates)";
	}

	static Path lockfileDir() {
		Path fromRepoRoot = Path.of("lib-extra", "src", "main", "resources", "com", "diffplug", "spotless", "extra", "eclipse_jdt_formatter");
		if (Files.isDirectory(fromRepoRoot)) {
			return fromRepoRoot;
		}
		Path fromLibExtra = Path.of("src", "main", "resources", "com", "diffplug", "spotless", "extra", "eclipse_jdt_formatter");
		if (Files.isDirectory(fromLibExtra)) {
			return fromLibExtra;
		}
		throw new IllegalStateException("Unable to locate eclipse_jdt_formatter resource directory");
	}

	/**
	 * Resolves full, explicit Maven coordinates from the P2 query result for the given Eclipse version.
	 * <p>
	 * This supports both exact and range-based historical metadata because the P2 solver has already
	 * chosen concrete versions before exposing Maven coordinates.
	 */
	private static List<String> resolveTransitiveClosure(String eclipseVersion) throws Exception {
		P2QueryResult query = queryJdtFromP2(eclipseVersion);

		// "g:a" -> selected version
		Map<String, String> selected = new LinkedHashMap<>();
		for (String coordinate : query.getJarsOnMavenCentral()) {
			String[] parts = coordinate.split(":");
			if (parts.length != 3) {
				throw new IllegalStateException("Expected Maven coordinate g:a:v but got: " + coordinate);
			}
			String groupId = parts[0], artifactId = parts[1], version = parts[2];
			String key = groupId + ":" + artifactId;
			String existing = selected.get(key);
			if (existing == null || compareVersions(existing, version) < 0) {
				selected.put(key, version);
			}
		}
		String rootVersion = selected.remove(JDT_MAVEN_KEY);
		if (rootVersion == null) {
			throw new IllegalStateException("P2 result for Eclipse " + eclipseVersion + " did not contain " + JDT_MAVEN_KEY);
		}
		List<String> result = new ArrayList<>();
		result.add(JDT_MAVEN_KEY + ":" + rootVersion);
		selected.entrySet().stream()
				.map(e -> e.getKey() + ":" + e.getValue())
				.sorted()
				.forEach(result::add);
		return result;
	}

	private static P2QueryResult queryJdtFromP2(String eclipseVersion) throws Exception {
		Exception lastError = null;
		for (String baseUrl : ECLIPSE_UPDATE_BASE_URLS) {
			try {
				P2Model model = new P2Model();
				addPlatformRepo(model, eclipseVersion, baseUrl);
				model.getInstall().add(JDT_ID);
				return model.query(P2ClientCache.PREFER_OFFLINE, P2QueryCache.ALLOW);
			} catch (Exception e) {
				lastError = e;
			}
		}
		throw new IllegalStateException("Failed to query Eclipse " + eclipseVersion + " from known update sites", lastError);
	}

	private static void addPlatformRepo(P2Model model, String version, String baseUrl) {
		if (!version.startsWith("4.")) {
			throw new IllegalArgumentException("Expected 4.x");
		}
		model.addP2Repo(baseUrl + version + "/");
	}

	/** Compares two version strings numerically, segment by segment. Returns positive if v1 &gt; v2. */
	private static int compareVersions(String v1, String v2) {
		String[] p1 = v1.split("\\.");
		String[] p2 = v2.split("\\.");
		int len = Math.max(p1.length, p2.length);
		for (int i = 0; i < len; i++) {
			int n1 = i < p1.length ? parseVersionSegment(p1[i]) : 0;
			int n2 = i < p2.length ? parseVersionSegment(p2[i]) : 0;
			if (n1 != n2)
				return Integer.compare(n1, n2);
		}
		return 0;
	}

	private static int parseVersionSegment(String segment) {
		try {
			return Integer.parseInt(segment);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
