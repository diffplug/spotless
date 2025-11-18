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
package com.diffplug.spotless;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.java.TargetJvmEnvironment;
import org.gradle.testfixtures.ProjectBuilder;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.common.base.Suppliers;
import com.diffplug.common.collect.ImmutableSet;
import com.diffplug.common.io.Files;

public class TestProvisioner {
	public static Project gradleProject(File dir) {
		File userHome = new File(StandardSystemProperty.USER_HOME.value());
		return ProjectBuilder.builder()
				.withGradleUserHomeDir(new File(userHome, ".gradle"))
				.withProjectDir(dir)
				.build();
	}

	/**
	 * Creates a Provisioner for the given repositories.
	 * <p>
	 * The first time a project is created, there are ~7 seconds of configuration
	 * which will go away for all subsequent runs.
	 * <p>
	 * Every call to resolve will take about 1 second, even when all artifacts are resolved.
	 */
	private static Provisioner createWithRepositories(Consumer<RepositoryHandler> repoConfig) {
		// Running this takes ~3 seconds the first time it is called. Probably because of classloading.
		File tempDir = Files.createTempDir();
		Project project = TestProvisioner.gradleProject(tempDir);
		repoConfig.accept(project.getRepositories());
		return (withTransitives, mavenCoords) -> {
			Dependency[] deps = mavenCoords.stream()
					.map(project.getDependencies()::create)
					.toArray(Dependency[]::new);
			Configuration config = project.getConfigurations().detachedConfiguration(deps);
			config.setTransitive(withTransitives);
			config.setDescription(mavenCoords.toString());
			config.attributes(attr -> {
				attr.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
				attr.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
				// Add this attribute for resolving Guava dependency, see https://github.com/google/guava/issues/6801.
				attr.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.getObjects().named(TargetJvmEnvironment.class, TargetJvmEnvironment.STANDARD_JVM));
			});
			try {
				return config.resolve();
			} catch (ResolveException e) {
				/* Provide Maven coordinates in exception message instead of static string 'detachedConfiguration' */
				throw new RuntimeException("Error resolving configuration: " + config.getDescription(), e);
			} finally {
				// delete the temp dir
				try {
					java.nio.file.Files.walk(tempDir.toPath())
							.sorted(Comparator.reverseOrder())
							.map(Path::toFile)
							.forEach(File::delete);
				} catch (IOException e) {
					throw Errors.asRuntime(e);
				}
			}
		};
	}

	/** Creates a Provisioner which will cache the result of previous calls. */
	@SuppressWarnings("unchecked")
	private static Provisioner caching(String name, Supplier<Provisioner> input) {
		File spotlessDir = new File(StandardSystemProperty.USER_DIR.value()).getParentFile();
		File testlib = new File(spotlessDir, "testlib");
		File cacheFile = new File(testlib, "build/tmp/testprovisioner." + name + ".cache");

		Map<ImmutableSet<String>, ImmutableSet<File>> cached;
		if (cacheFile.exists()) {
			try (ObjectInputStream inputStream = new ObjectInputStream(Files.asByteSource(cacheFile).openBufferedStream())) {
				cached = (Map<ImmutableSet<String>, ImmutableSet<File>>) inputStream.readObject();
			} catch (IOException | ClassNotFoundException e) {
				throw Errors.asRuntime(e);
			}
		} else {
			cached = new HashMap<>();
			try {
				Files.createParentDirs(cacheFile);
			} catch (IOException e) {
				throw Errors.asRuntime(e);
			}
		}
		return (withTransitives, mavenCoordsRaw) -> {
			ImmutableSet<String> mavenCoords = ImmutableSet.copyOf(mavenCoordsRaw);
			synchronized (TestProvisioner.class) {
				ImmutableSet<File> result = cached.get(mavenCoords);
				// double-check that depcache pruning hasn't removed them since our cache cached them
				boolean needsToBeSet = result == null || !result.stream().allMatch(file -> file.exists() && file.isFile() && file.length() > 0);
				if (needsToBeSet) {
					result = ImmutableSet.copyOf(input.get().provisionWithTransitives(withTransitives, mavenCoords));
					cached.put(mavenCoords, result);
					try (ObjectOutputStream outputStream = new ObjectOutputStream(Files.asByteSink(cacheFile).openBufferedStream())) {
						outputStream.writeObject(cached);
					} catch (IOException e) {
						throw Errors.asRuntime(e);
					}
				}
				return result;
			}
		};
	}

	/** Creates a Provisioner for the mavenCentral repo. */
	public static Provisioner mavenCentral() {
		return MAVEN_CENTRAL.get();
	}

	private static final Supplier<Provisioner> MAVEN_CENTRAL = Suppliers.memoize(() -> caching("mavenCentral", () -> createWithRepositories(RepositoryHandler::mavenCentral)));

	/** Creates a Provisioner for the local maven repo for development purpose. */
	public static Provisioner mavenLocal() {
		return createWithRepositories(RepositoryHandler::mavenLocal);
	}

	/** Creates a Provisioner for the Sonatype snapshots maven repo for development purpose. */
	public static Provisioner snapshots() {
		return createWithRepositories(repo -> repo.maven(setup -> setup.setUrl("https://oss.sonatype.org/content/repositories/snapshots")));
	}

}
