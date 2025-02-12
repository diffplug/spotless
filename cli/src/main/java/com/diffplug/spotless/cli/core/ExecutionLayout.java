/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.cli.core;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.diffplug.spotless.cli.steps.BuildDirGloballyReusable;
import com.diffplug.spotless.cli.steps.SpotlessCLIFormatterStep;

public class ExecutionLayout {

	private final FileResolver fileResolver;
	private final SpotlessCommandLineStream commandLineStream;
	private final ChecksumCalculator checksumCalculator;

	private ExecutionLayout(@Nonnull FileResolver fileResolver, @Nonnull SpotlessCommandLineStream commandLineStream) {
		this.fileResolver = Objects.requireNonNull(fileResolver);
		this.commandLineStream = Objects.requireNonNull(commandLineStream);
		this.checksumCalculator = new ChecksumCalculator();
	}

	public static ExecutionLayout create(@Nonnull FileResolver fileResolver, @Nonnull SpotlessCommandLineStream commandLineStream) {
		return new ExecutionLayout(fileResolver, commandLineStream);
	}

	public Optional<Path> find(@Nullable Path searchPath) {
		if (searchPath == null) {
			return Optional.empty();
		}
		Path found = fileResolver.resolvePath(searchPath);
		if (found.toFile().canRead()) {
			return Optional.of(found);
		}
		if (searchPath.toFile().canRead()) {
			return Optional.of(searchPath);
		}
		return Optional.empty();
	}

	public Path baseDir() {
		return fileResolver.baseDir();
	}

	public Path buildDir() {
		// gradle?
		if (isGradleDirectory()) {
			return gradleBuildDir();
		}
		if (isMavenDirectory()) {
			return mavenBuildDir();
		}
		return tempBuildDir();
	}

	private boolean isGradleDirectory() {
		return List.of("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts").stream()
				.map(Paths::get)
				.map(this::find)
				.anyMatch(Optional::isPresent);
	}

	private Path gradleBuildDir() {
		return fileResolver.resolvePath(Paths.get("build", "spotless-cli"));
	}

	private boolean isMavenDirectory() {
		return List.of("pom.xml").stream()
				.map(Paths::get)
				.map(this::find)
				.anyMatch(Optional::isPresent);
	}

	private Path mavenBuildDir() {
		return fileResolver.resolvePath(Paths.get("target", "spotless-cli"));
	}

	private Path tempBuildDir() {
		String tmpDir = System.getProperty("java.io.tmpdir");
		return Path.of(tmpDir, "spotless-cli");
	}

	public Path buildDirFor(@Nonnull SpotlessCLIFormatterStep step) {
		Objects.requireNonNull(step);
		Path buildDir = buildDir();
		String checksum = checksumCalculator.calculateChecksum(step);
		if (step instanceof BuildDirGloballyReusable) {
			return buildDir.resolve(checksum);
		}
		String commandLineChecksum = checksumCalculator.calculateChecksum(commandLineStream);
		return buildDir.resolve(checksum + "-" + commandLineChecksum);
	}
}
