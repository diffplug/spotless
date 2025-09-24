/*
 * Copyright 2023-2025 DiffPlug
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
package com.diffplug.spotless.maven.npm;

import static com.diffplug.spotless.maven.npm.AbstractNpmFormatterStepFactory.SPOTLESS_NPM_INSTALL_CACHE_DEFAULT_NAME;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ProcessRunner.Result;
import com.diffplug.spotless.maven.MavenIntegrationHarness;
import com.diffplug.spotless.tag.NpmTest;

@NpmTest
public class NpmStepsWithNpmInstallCacheTest extends MavenIntegrationHarness {

	// TODO implement tests without cache and with various cache paths
	// using only prettier is enough since the other cases are covered by gradle-side integration tests

	@Test
	void prettierTypescriptWithoutCache() throws Exception {
		String suffix = "ts";
		writePomWithPrettierSteps("**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <configFile>.prettierrc.yml</configFile>",
				"</prettier>");
		Result result = run("typescript", suffix);
		Assertions.assertThat(result.stdOutUtf8()).doesNotContain("Caching node_modules for").doesNotContain("Using cached node_modules for");
	}

	@Test
	void prettierTypescriptWithDefaultCache() throws Exception {
		String suffix = "ts";
		writePomWithPrettierSteps("**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <configFile>.prettierrc.yml</configFile>",
				"  <npmInstallCache>true</npmInstallCache>",
				"</prettier>");
		Result result = run("typescript", suffix);
		Assertions.assertThat(result.stdOutUtf8())
				.contains("Caching node_modules for")
				.contains(SPOTLESS_NPM_INSTALL_CACHE_DEFAULT_NAME)
				.doesNotContain("Using cached node_modules for");
	}

	@Disabled
	@Test
	void prettierTypescriptWithDefaultCacheIsReusedOnSecondRun() throws Exception {
		String suffix = "ts";
		writePomWithPrettierSteps("**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <configFile>.prettierrc.yml</configFile>",
				"  <npmInstallCache>true</npmInstallCache>",
				"</prettier>");
		Result result1 = run("typescript", suffix);
		Assertions.assertThat(result1.stdOutUtf8())
				.contains("Caching node_modules for")
				.contains(SPOTLESS_NPM_INSTALL_CACHE_DEFAULT_NAME)
				.doesNotContain("Using cached node_modules for");

		// recursively delete target folder to simulate a fresh run (except the default cache folder)
		recursiveDelete(Path.of(rootFolder().getAbsolutePath(), "target"), SPOTLESS_NPM_INSTALL_CACHE_DEFAULT_NAME);

		Result result2 = run("typescript", suffix);
		Assertions.assertThat(result2.stdOutUtf8())
				.doesNotContain("Caching node_modules for")
				.contains(SPOTLESS_NPM_INSTALL_CACHE_DEFAULT_NAME)
				.contains("Using cached node_modules for");
	}

	@Test
	void prettierTypescriptWithSpecificCache() throws Exception {
		String suffix = "ts";
		File cacheDir = newFolder("cache-prettier-1");
		writePomWithPrettierSteps("**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <configFile>.prettierrc.yml</configFile>",
				"  <npmInstallCache>" + cacheDir.getAbsolutePath() + "</npmInstallCache>",
				"</prettier>");
		Result result = run("typescript", suffix);
		Assertions.assertThat(result.stdOutUtf8())
				.contains("Caching node_modules for")
				.contains(Path.of(cacheDir.getAbsolutePath()).toAbsolutePath().toString())
				.doesNotContain("Using cached node_modules for");
	}

	@Disabled
	@Test
	void prettierTypescriptWithSpecificCacheIsUsedOnSecondRun() throws Exception {
		String suffix = "ts";
		File cacheDir = newFolder("cache-prettier-1");
		writePomWithPrettierSteps("**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <configFile>.prettierrc.yml</configFile>",
				"  <npmInstallCache>" + cacheDir.getAbsolutePath() + "</npmInstallCache>",
				"</prettier>");
		Result result1 = run("typescript", suffix);
		Assertions.assertThat(result1.stdOutUtf8())
				.contains("Caching node_modules for")
				.contains(Path.of(cacheDir.getAbsolutePath()).toAbsolutePath().toString())
				.doesNotContain("Using cached node_modules for");

		// recursively delete target folder to simulate a fresh run
		recursiveDelete(Path.of(rootFolder().getAbsolutePath(), "target"), null);

		Result result2 = run("typescript", suffix);
		Assertions.assertThat(result2.stdOutUtf8())
				.doesNotContain("Caching node_modules for")
				.contains(Path.of(cacheDir.getAbsolutePath()).toAbsolutePath().toString())
				.contains("Using cached node_modules for");
	}

	private void recursiveDelete(Path path, String exclusion) throws IOException {
		Files.walkFileTree(path, new RecursiveDelete(exclusion));
	}

	private Result run(String kind, String suffix) throws IOException, InterruptedException {
		String path = prepareRun(kind, suffix);
		Result result = mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("npm/prettier/filetypes/" + kind + "/" + kind + ".clean");
		return result;
	}

	private String prepareRun(String kind, String suffix) throws IOException {
		String configPath = ".prettierrc.yml";
		setFile(configPath).toResource("npm/prettier/filetypes/" + kind + "/" + ".prettierrc.yml");
		String path = "src/main/" + kind + "/test." + suffix;
		setFile(path).toResource("npm/prettier/filetypes/" + kind + "/" + kind + ".dirty");
		return path;
	}

	private static class RecursiveDelete extends SimpleFileVisitor<Path> {
		private final String exclusionDirectory;

		public RecursiveDelete(String exclusionDirectory) {
			this.exclusionDirectory = exclusionDirectory;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (exclusionDirectory != null && dir.toFile().getName().equals(exclusionDirectory)) {
				return FileVisitResult.SKIP_SUBTREE;
			}
			return super.preVisitDirectory(dir, attrs);
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Files.delete(file);
			return super.visitFile(file, attrs);
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (dir.toFile().listFiles().length != 0) {
				// skip non-empty dir
				return super.postVisitDirectory(dir, exc);
			}
			Files.delete(dir);
			return super.postVisitDirectory(dir, exc);
		}
	}
}
