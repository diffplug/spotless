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
package com.diffplug.spotless.maven;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.maven.cpp.EclipseCdt;
import com.diffplug.spotless.maven.groovy.GrEclipse;
import com.diffplug.spotless.maven.java.Eclipse;

class FormatterStepFactoryTest {
	@TempDir
	Path tempDir;

	@Test
	void defaultP2CacheDirectoryUsesMavenLocalRepository() {
		File localRepository = tempDir.resolve("local-repo").toFile();

		assertThat(FormatterStepFactory.defaultP2CacheDirectory(repositorySystemSession(localRepository)))
				.isEqualTo(new File(localRepository, "dev/equo/p2-data"));
	}

	@Test
	void grEclipseUsesConfiguredCacheDirectory() throws Exception {
		assertUsesConfiguredCacheDirectory(new GrEclipse());
	}

	@Test
	void eclipseCdtUsesConfiguredCacheDirectory() throws Exception {
		assertUsesConfiguredCacheDirectory(new EclipseCdt());
	}

	@Test
	void grEclipseUsesDefaultCacheDirectory() throws Exception {
		assertUsesDefaultCacheDirectory(new GrEclipse());
	}

	@Test
	void eclipseCdtUsesDefaultCacheDirectory() throws Exception {
		assertUsesDefaultCacheDirectory(new EclipseCdt());
	}

	/**
	 * Eclipse JDT ships an embedded lockfile for its default version, so it resolves straight from
	 * Maven and never runs a P2 query -- which also means its P2 cache directory goes unused. The
	 * P2 cache directory behavior is covered by the grEclipse and eclipseCdt cases above, neither
	 * of which has an embedded lockfile.
	 */
	@Test
	void eclipseResolvesFromEmbeddedLockfileInsteadOfP2() throws Exception {
		Eclipse factory = new Eclipse();
		factory.init(repositorySystemSession(tempDir.resolve("local-repo").toFile()));

		AtomicReference<Collection<String>> actualCoordinates = new AtomicReference<>();
		AtomicBoolean p2WasQueried = new AtomicBoolean();
		File fakeJar = tempDir.resolve("fake.jar").toFile();
		Files.write(fakeJar.toPath(), new byte[]{0});

		FormatterStep step = factory.newFormatterStep(new FormatterStepConfig(
				UTF_8,
				"",
				Optional.empty(),
				(withTransitives, mavenCoordinates) -> {
					actualCoordinates.set(mavenCoordinates);
					return Set.of(fakeJar);
				},
				(modelWrapper, mavenProvisioner, cacheDirectory) -> {
					p2WasQueried.set(true);
					return List.of(fakeJar);
				},
				null,
				Optional.empty(),
				Optional.empty()));

		int unused = step.hashCode();

		assertThat(p2WasQueried).isFalse();
		assertThat(actualCoordinates.get()).anyMatch(coordinate -> coordinate.startsWith("org.eclipse.jdt:org.eclipse.jdt.core:"));
	}

	private void assertUsesConfiguredCacheDirectory(FormatterStepFactory factory) throws Exception {
		File configuredCacheDirectory = tempDir.resolve("configured-p2-cache").toFile();
		Field cacheDirectory = cacheDirectoryField(factory);
		cacheDirectory.set(factory, configuredCacheDirectory);

		factory.init(repositorySystemSession(tempDir.resolve("local-repo").toFile()));

		assertP2ProvisionerReceives(factory, configuredCacheDirectory);
	}

	private void assertUsesDefaultCacheDirectory(FormatterStepFactory factory) throws Exception {
		File localRepository = tempDir.resolve("local-repo").toFile();
		File defaultCacheDirectory = new File(localRepository, "dev/equo/p2-data");

		factory.init(repositorySystemSession(localRepository));

		assertP2ProvisionerReceives(factory, defaultCacheDirectory);
	}

	private void assertP2ProvisionerReceives(FormatterStepFactory factory, File expectedCacheDirectory) throws Exception {
		AtomicReference<File> actualCacheDirectory = new AtomicReference<>();
		File fakeJar = tempDir.resolve("fake.jar").toFile();
		Files.write(fakeJar.toPath(), new byte[]{0});

		FormatterStep step = factory.newFormatterStep(new FormatterStepConfig(
				UTF_8,
				"",
				Optional.empty(),
				(withTransitives, mavenCoordinates) -> Set.of(),
				(modelWrapper, mavenProvisioner, cacheDirectory) -> {
					actualCacheDirectory.set(cacheDirectory);
					return List.of(fakeJar);
				},
				null,
				Optional.empty(),
				Optional.empty()));

		int unused = step.hashCode();

		assertThat(actualCacheDirectory.get()).isEqualTo(expectedCacheDirectory);
	}

	private Field cacheDirectoryField(FormatterStepFactory factory) throws NoSuchFieldException {
		Field cacheDirectory = factory.getClass().getDeclaredField("cacheDirectory");
		cacheDirectory.setAccessible(true);
		return cacheDirectory;
	}

	private RepositorySystemSession repositorySystemSession(File localRepository) {
		RepositorySystemSession repositorySystemSession = mock();
		when(repositorySystemSession.getLocalRepository()).thenReturn(new LocalRepository(localRepository));
		return repositorySystemSession;
	}
}
