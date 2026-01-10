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
package com.diffplug.spotless;

import static com.diffplug.common.base.Errors.asRuntime;
import static com.diffplug.common.base.StandardSystemProperty.USER_DIR;
import static com.diffplug.common.base.StandardSystemProperty.USER_HOME;
import static com.diffplug.common.base.Suppliers.memoize;
import static com.diffplug.common.collect.ImmutableSet.copyOf;
import static com.diffplug.common.io.Files.asByteSink;
import static com.diffplug.common.io.Files.asByteSource;
import static com.diffplug.common.io.Files.createParentDirs;
import static com.diffplug.common.io.Files.createTempDir;
import static java.nio.file.Files.walk;
import static java.nio.file.Path.of;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.requireNonNull;
import static org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE;
import static org.gradle.api.attributes.Bundling.EXTERNAL;
import static org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE;
import static org.gradle.api.attributes.Category.LIBRARY;
import static org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM;
import static org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE;
import static org.gradle.testfixtures.ProjectBuilder.builder;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.Collection;
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

import com.diffplug.common.collect.ImmutableSet;

public final class TestProvisioner {

	private static final String MAVEN_CENTRAL_CACHE = "build/tmp/testprovisioner.mavenCentral.cache";
	private static final String TEST_LIB = "testlib";
	private static final Provisioner PROVISIONER = memoize(
			() -> caching(() -> createWithRepositories(RepositoryHandler::mavenCentral))).get();

	private TestProvisioner() {}

	public static Project gradleProject(File projectDir) {
		return builder()
				.withGradleUserHomeDir(new File(requireNonNull(USER_HOME.value()), ".gradle"))
				.withProjectDir(projectDir)
				.build();
	}

	/**
	 * Creates a Provisioner for the mavenCentral repo.
	 */
	public static Provisioner mavenCentral() {
		return PROVISIONER;
	}

	/**
	 * Creates a Provisioner which will cache the result of previous calls.
	 */
	private static Provisioner caching(Supplier<Provisioner> delegate) {
		var cacheFile = new File(new File(new File(requireNonNull(USER_DIR.value())).getParentFile(), TEST_LIB), MAVEN_CENTRAL_CACHE);
		return (withTransitives, rawCoords) -> resolveAndCache(
				delegate,
				withTransitives,
				loadCache(cacheFile),
				copyOf(rawCoords),
				cacheFile);
	}

	private static ImmutableSet<File> resolveAndCache(Supplier<Provisioner> delegate,
			boolean withTransitives,
			Map<ImmutableSet<String>, ImmutableSet<File>> artifactCache,
			ImmutableSet<String> mavenCoords,
			File cacheFile) {
		synchronized (TestProvisioner.class) {
			var cachedFiles = artifactCache.get(mavenCoords);
			var cacheValid = cachedFiles != null &&
					cachedFiles.stream().allMatch(
							f -> f.exists() && f.isFile() && f.length() > 0);
			if (!cacheValid) {
				cachedFiles = copyOf(
						delegate.get()
								.provisionWithTransitives(withTransitives, mavenCoords));
				artifactCache.put(mavenCoords, cachedFiles);
				try (var out = new ObjectOutputStream(asByteSink(cacheFile).openBufferedStream())) {
					out.writeObject(artifactCache);
				} catch (IOException e) {
					throw asRuntime(e);
				}
			}
			return cachedFiles;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<ImmutableSet<String>, ImmutableSet<File>> loadCache(File cacheFile) {
		if (cacheFile.exists()) {
			try (var in = new ObjectInputStream(asByteSource(cacheFile).openBufferedStream())) {
				return (Map<ImmutableSet<String>, ImmutableSet<File>>) in.readObject();
			} catch (IOException | ClassNotFoundException e) {
				throw asRuntime(e);
			}
		}
		try {
			createParentDirs(cacheFile);
			return new HashMap<>();
		} catch (IOException e) {
			throw asRuntime(e);
		}
	}

	/**
	 * Creates a Provisioner for the given repositories.
	 */
	private static Provisioner createWithRepositories(Consumer<RepositoryHandler> repositoryConfigurer) {
		return createProvisioner(repositoryConfigurer, gradleProject(createTempDir()));
	}

	private static Provisioner createProvisioner(Consumer<RepositoryHandler> repositoryConfigurer, Project project) {
		repositoryConfigurer.accept(project.getRepositories());
		return (withTransitives, mavenCoords) -> {
			var configuration = applyAttributes(project
					.getConfigurations()
					.detachedConfiguration(mavenCoords
							.stream()
							.map(project.getDependencies()::create)
							.toArray(Dependency[]::new)),
					withTransitives,
					mavenCoords,
					project);
			try {
				return configuration.resolve();
			} catch (ResolveException e) {
				throw new RuntimeException("Error resolving configuration: " + configuration.getDescription(), e);
			} finally {
				try (var paths = walk(of(project.getPath()))) {
					paths.sorted(reverseOrder())
							.map(Path::toFile)
							.forEach(File::delete);
				} catch (IOException ignored) {}
			}
		};
	}

	private static Configuration applyAttributes(Configuration configuration,
			boolean withTransitives,
			Collection<String> mavenCoords,
			Project project) {
		configuration.setTransitive(withTransitives);
		configuration.setDescription(mavenCoords.toString());
		configuration.attributes(attrs -> {
			attrs.attribute(
					CATEGORY_ATTRIBUTE,
					project.getObjects().named(Category.class, LIBRARY));
			attrs.attribute(
					BUNDLING_ATTRIBUTE,
					project.getObjects().named(Bundling.class, EXTERNAL));
			attrs.attribute(
					TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
					project.getObjects().named(TargetJvmEnvironment.class, STANDARD_JVM));
		});
		return configuration;
	}
}
