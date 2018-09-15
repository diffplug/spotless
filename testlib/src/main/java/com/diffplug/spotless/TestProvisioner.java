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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.testfixtures.ProjectBuilder;

import com.diffplug.common.base.Box;
import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.common.base.Suppliers;
import com.diffplug.common.collect.ImmutableSet;
import com.diffplug.common.io.Files;

public class TestProvisioner {
	/**
	 * Creates a Provisioner for the given repositories.
	 *
	 * The first time a project is created, there are ~7 seconds of configuration
	 * which will go away for all subsequent runs.
	 *
	 * Every call to resolve will take about 1 second, even when all artifacts are resolved.
	 */
	private static Supplier<Provisioner> createLazyWithRepositories(Consumer<RepositoryHandler> repoConfig) {
		// Running this takes ~3 seconds the first time it is called. Probably because of classloading.
		return Suppliers.memoize(() -> {
			Project project = ProjectBuilder.builder().build();
			repoConfig.accept(project.getRepositories());
			return (withTransitives, mavenCoords) -> {
				Dependency[] deps = mavenCoords.stream()
						.map(project.getDependencies()::create)
						.toArray(Dependency[]::new);
				Configuration config = project.getConfigurations().detachedConfiguration(deps);
				config.setTransitive(withTransitives);
				config.setDescription(mavenCoords.toString());
				try {
					return config.resolve();
				} catch (ResolveException e) {
					/* Provide Maven coordinates in exception message instead of static string 'detachedConfiguration' */
					throw new ResolveException(config.getDescription(), e);
				}
			};
		});
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
		}
		return (withTransitives, mavenCoords) -> {
			Box<Boolean> wasChanged = Box.of(false);
			ImmutableSet<File> result = cached.computeIfAbsent(ImmutableSet.copyOf(mavenCoords), coords -> {
				wasChanged.set(true);
				return ImmutableSet.copyOf(input.get().provisionWithTransitives(withTransitives, coords));
			});
			if (wasChanged.get()) {
				try (ObjectOutputStream outputStream = new ObjectOutputStream(Files.asByteSink(cacheFile).openBufferedStream())) {
					outputStream.writeObject(cached);
				} catch (IOException e) {
					throw Errors.asRuntime(e);
				}
			}
			return result;
		};
	}

	/** Creates a Provisioner for the jcenter repo. */
	public static Provisioner jcenter() {
		return jcenter.get();
	}

	private static final Supplier<Provisioner> jcenter = Suppliers.memoize(() -> {
		return caching("jcenter", createLazyWithRepositories(repo -> repo.jcenter()));
	});

	/** Creates a Provisioner for the mavenCentral repo. */
	public static Provisioner mavenCentral() {
		return mavenCentral.get();
	}

	private static final Supplier<Provisioner> mavenCentral = Suppliers.memoize(() -> {
		return caching("mavenCentral", createLazyWithRepositories(repo -> repo.mavenCentral()));
	});

	/** Creates a Provisioner for the local maven repo for development purpose. */
	public static Provisioner mavenLocal() {
		return mavenLocal.get();
	}

	private static final Supplier<Provisioner> mavenLocal = createLazyWithRepositories(repo -> repo.mavenLocal());

	/** Creates a Provisioner for the Sonatype snapshots maven repo for development purpose. */
	public static Provisioner snapshots() {
		return snapshots.get();
	}

	private static final Supplier<Provisioner> snapshots = createLazyWithRepositories(repo -> {
		repo.maven(setup -> {
			setup.setUrl("https://oss.sonatype.org/content/repositories/snapshots");
		});
	});
}
