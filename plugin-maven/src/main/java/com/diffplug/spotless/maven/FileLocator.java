/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless.maven;

import static com.diffplug.common.base.Strings.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;
import org.codehaus.plexus.util.FileUtils;

public class FileLocator {

	static final String TMP_RESOURCE_FILE_PREFIX = "spotless-resource-";

	private final ResourceManager resourceManager;
	private final File baseDir;
	private final File buildDir;
	private final File dataDir;

	public FileLocator(ResourceManager resourceManager, File baseDir, File buildDir) {
		this.resourceManager = requireNonNull(resourceManager);
		this.baseDir = requireNonNull(baseDir);
		this.buildDir = requireNonNull(buildDir);
		this.dataDir = findDataDir();
	}

	/**
	 * If the given path is a local file returns it as such unchanged, otherwise
	 * extracts the given resource to a randomly-named file in the build folder.
	 */
	public File locateFile(String path) {
		if (isNullOrEmpty(path)) {
			return null;
		}

		File localFile = new File(path);
		if (localFile.exists() && localFile.isFile()) {
			return localFile;
		}

		String outputFile = tmpOutputFileName(path);
		try {
			return resourceManager.getResourceAsFile(path, outputFile);
		} catch (ResourceNotFoundException e) {
			throw new RuntimeException("Unable to locate file with path: " + path, e);
		} catch (FileResourceCreationException e) {
			throw new RuntimeException("Unable to create temporary file '" + outputFile + "' in the output directory",
					e);
		}
	}

	/**
	 * Finds the base directory of the Maven or Gradle project on which spotless is
	 * currently being executed.
	 *
	 * @return The base directory of the current Maven or Gradel project.
	 */
	public File getBaseDir() {
		return baseDir;
	}

	/**
	 * Finds the build directory (e.g. <code>/target</code>) of the Maven or Gradle
	 * project on which spotless is currently being executed.
	 *
	 * @return The project build directory of the current Maven or Gradle project.
	 */
	public File getBuildDir() {
		return buildDir;
	}

	/**
	 * Finds the data directory that can be used for storing shared data such as
	 * downloaded files globally. This is a directory in the local repository, e.g.
	 * <code>~/.m2/repository/com/diffplus/spotless/spotless-data<code>.
	 *
	 * @return The directory for storing shared data.
	 */
	public File getDataDir() {
		return dataDir;
	}

	private static String tmpOutputFileName(String path) {
		String extension = FileUtils.extension(path);
		byte[] pathHash = hash(path);
		String pathBase64 = Base64.getEncoder().encodeToString(pathHash);
		return TMP_RESOURCE_FILE_PREFIX + pathBase64 + '.' + extension;
	}

	private static byte[] hash(String value) {
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 digest algorithm not available", e);
		}
		messageDigest.update(value.getBytes(UTF_8));
		return messageDigest.digest();
	}

	private static File findDataDir() {
		try {
			// JAR path is e.g.
			// ~/.m2/repository/com/diffplug/spotless/spotless-plugin-maven/1.2.3/spotless-plugin-maven-1.2.3.jar
			var codeSource = FileLocator.class.getProtectionDomain().getCodeSource();
			var location = codeSource != null ? codeSource.getLocation() : null;
			var locationUri = location != null ? location.toURI() : null;
			var jarPath = locationUri != null && "file".equals(locationUri.getScheme()) ? Path.of(locationUri) : null;
			var parent1 = jarPath != null ? jarPath.getParent() : null;
			var parent2 = parent1 != null ? parent1.getParent() : null;
			var base = parent2 != null ? parent2.getParent() : null;
			var sub = base != null ? base.resolve("spotless-data") : null;
			if (sub != null) {
				return sub.toAbsolutePath().toFile();
			} else {
				return findUserHome();
			}
		} catch (final SecurityException e) {
			return findUserHome();
		} catch (final URISyntaxException | FileSystemNotFoundException | IllegalArgumentException e) {
			throw new RuntimeException("Unable to determine data directory in local Maven repository", e);
		}
	}

	private static File findUserHome() {
		var home = Path.of(System.getenv("user.home"));
		return home.resolve(".rome").toAbsolutePath().toFile();
	}
}
