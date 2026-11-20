/*
 * Copyright 2026 DiffPlug
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
package com.diffplug.spotless.npm;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Test helper to inspect which npm packages a npm-based formatter step installed.
 */
public final class InstalledNpmPackages {

	private static final Pattern VERSION = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");

	private InstalledNpmPackages() {
		// no instance
	}

	/**
	 * Returns the version of the top-level (hoisted) installation of {@code packageName}, i.e. of
	 * {@code <buildDir>/<node-server-dir>/node_modules/<packageName>/package.json}.
	 *
	 * @throws IllegalStateException if there is not exactly one such installation
	 */
	public static String versionInstalledBelow(File buildDir, String packageName) {
		final Path root = buildDir.toPath();
		final Path relativePackageJson = Path.of("node_modules", packageName, "package.json");
		final List<Path> candidates;
		try (Stream<Path> files = Files.walk(root)) {
			candidates = files.filter(path -> path.endsWith(relativePackageJson))
					.filter(path -> root.relativize(path).getNameCount() == relativePackageJson.getNameCount() + 1)
					.toList();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		if (candidates.size() != 1) {
			throw new IllegalStateException("Expected exactly one top-level installation of " + packageName + " below " + buildDir + ", but found " + candidates);
		}
		try {
			final Matcher version = VERSION.matcher(Files.readString(candidates.get(0)));
			if (!version.find()) {
				throw new IllegalStateException("No version in " + candidates.get(0));
			}
			return version.group(1);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
