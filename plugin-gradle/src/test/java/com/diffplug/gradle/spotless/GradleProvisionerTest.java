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
package com.diffplug.gradle.spotless;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.P2ModelWrapper;
import com.diffplug.spotless.extra.P2Provisioner;

import dev.equo.solstice.p2.P2Model;

class GradleProvisionerTest {

	@Nested
	class DedupingProvisionerTest {
		@Test
		void cacheHitReturnsCachedResult() {
			AtomicInteger callCount = new AtomicInteger(0);
			Provisioner underlying = mockProvisioner(callCount);
			GradleProvisioner.DedupingProvisioner deduping = new GradleProvisioner.DedupingProvisioner(underlying);

			// First call
			Set<File> result1 = deduping.provisionWithTransitives(true, List.of("com.google:guava:32.0.0-jre"));
			// Second call with same parameters
			Set<File> result2 = deduping.provisionWithTransitives(true, List.of("com.google:guava:32.0.0-jre"));

			assertThat(result1).isSameAs(result2);
			assertThat(callCount.get()).as("Only called once").isEqualTo(1);
		}

		@ParameterizedTest
		@MethodSource("cacheMissScenarios")
		void cacheMissTriggersNewResolution(String scenario, boolean withTransitives1, List<String> coords1, boolean withTransitives2, List<String> coords2) {
			AtomicInteger callCount = new AtomicInteger(0);
			Provisioner underlying = mockProvisioner(callCount);
			GradleProvisioner.DedupingProvisioner deduping = new GradleProvisioner.DedupingProvisioner(underlying);

			Set<File> result1 = deduping.provisionWithTransitives(withTransitives1, coords1);
			Set<File> result2 = deduping.provisionWithTransitives(withTransitives2, coords2);

			assertThat(result1).isNotSameAs(result2);
			assertThat(callCount.get()).as("Called twice").isEqualTo(2);
		}

		static Stream<Arguments> cacheMissScenarios() {
			return Stream.of(
					Arguments.of("different coordinates",
							true, List.of("com.google:guava:32.0.0-jre"),
							true, List.of("org.slf4j:slf4j-api:2.0.0")),
					Arguments.of("different transitivity",
							true, List.of("com.google:guava:32.0.0-jre"),
							false, List.of("com.google:guava:32.0.0-jre")),
					Arguments.of("different order",
							true, List.of("com.google:guava:32.0.0-jre", "org.slf4j:slf4j-api:2.0.0"),
							true, List.of("org.slf4j:slf4j-api:2.0.0", "com.google:guava:32.0.0-jre")));
		}

		@Test
		void cachedOnlyCacheHitReturnsResult() {
			Provisioner underlying = mockProvisioner(new AtomicInteger(0));
			GradleProvisioner.DedupingProvisioner deduping = new GradleProvisioner.DedupingProvisioner(underlying);

			// Populate cache
			deduping.provisionWithTransitives(true, List.of("com.google:guava:32.0.0-jre"));

			// cachedOnly should return cached result
			Set<File> result = deduping.cachedOnly.provisionWithTransitives(true, List.of("com.google:guava:32.0.0-jre"));

			assertThat(result).isNotEmpty();
		}

		@Test
		void cachedOnlyCacheMissThrowsException() {
			Provisioner underlying = mockProvisioner(new AtomicInteger(0));
			GradleProvisioner.DedupingProvisioner deduping = new GradleProvisioner.DedupingProvisioner(underlying);

			// cachedOnly should throw when not cached
			assertThatThrownBy(() -> deduping.cachedOnly.provisionWithTransitives(true, List.of("com.google:guava:32.0.0-jre")))
					.isInstanceOf(GradleException.class)
					.hasMessageContaining("spotlessPredeclare");
		}

		private Provisioner mockProvisioner(AtomicInteger callCount) {
			return (withTransitives, mavenCoordinates) -> {
				callCount.incrementAndGet();
				// Return a unique set based on coordinates
				return Set.of(new File("/mock/" + String.join("-", mavenCoordinates) + ".jar"));
			};
		}
	}

	@Nested
	class DedupingP2ProvisionerTest {
		@Test
		void cacheHitReturnsCachedResult() throws IOException {
			AtomicInteger callCount = new AtomicInteger(0);
			P2Provisioner underlying = mockP2Provisioner(callCount);
			GradleProvisioner.DedupingP2Provisioner deduping = new GradleProvisioner.DedupingP2Provisioner(underlying);

			P2ModelWrapper model = createMockModel(
					List.of("https://download.eclipse.org/eclipse/updates/4.26/"),
					List.of("org.eclipse.jdt.core"),
					Set.of(),
					List.of(),
					true,
					null);

			// First call
			List<File> result1 = deduping.provisionP2Dependencies(model, mockProvisioner(), null);
			// Second call with same parameters
			List<File> result2 = deduping.provisionP2Dependencies(model, mockProvisioner(), null);

			assertThat(result1).isSameAs(result2);
			assertThat(callCount.get()).as("Only called once").isEqualTo(1);
		}

		@ParameterizedTest
		@MethodSource("cacheMissScenarios")
		void cacheMissTriggersNewResolution(String scenario, Function<P2ModelWrapper, P2ModelWrapper> modelModifier, File cacheDir2) throws IOException {
			AtomicInteger callCount = new AtomicInteger(0);
			P2Provisioner underlying = mockP2Provisioner(callCount);
			GradleProvisioner.DedupingP2Provisioner deduping = new GradleProvisioner.DedupingP2Provisioner(underlying);

			P2ModelWrapper model1 = createMockModel(
					List.of("https://download.eclipse.org/eclipse/updates/4.26/"),
					List.of("org.eclipse.jdt.core"),
					Set.of(),
					List.of(),
					true,
					null);

			P2ModelWrapper model2 = modelModifier.apply(model1);

			List<File> result1 = deduping.provisionP2Dependencies(model1, mockProvisioner(), null);
			List<File> result2 = deduping.provisionP2Dependencies(model2, mockProvisioner(), cacheDir2);

			assertThat(result1).isNotSameAs(result2);
			assertThat(callCount.get()).as("Called twice").isEqualTo(2);
		}

		static Stream<Arguments> cacheMissScenarios() {
			return Stream.of(
					Arguments.of("different P2 repo", (Function<P2ModelWrapper, P2ModelWrapper>) (m) -> createMockModel(
							List.of("https://download.eclipse.org/eclipse/updates/4.27/"),
							List.of("org.eclipse.jdt.core"),
							Set.of(),
							List.of(),
							true,
							null), null),
					Arguments.of("different install list", (Function<P2ModelWrapper, P2ModelWrapper>) (m) -> createMockModel(
							List.of("https://download.eclipse.org/eclipse/updates/4.26/"),
							List.of("org.eclipse.jdt.core", "org.eclipse.jdt.ui"),
							Set.of(),
							List.of(),
							true,
							null), null),
					Arguments.of("different filters", (Function<P2ModelWrapper, P2ModelWrapper>) (m) -> createMockModel(
							List.of("https://download.eclipse.org/eclipse/updates/4.26/"),
							List.of("org.eclipse.jdt.core"),
							Set.of("osgiFilter1"),
							List.of(),
							true,
							null), null),
					Arguments.of("different pure maven", (Function<P2ModelWrapper, P2ModelWrapper>) (m) -> createMockModel(
							List.of("https://download.eclipse.org/eclipse/updates/4.26/"),
							List.of("org.eclipse.jdt.core"),
							Set.of(),
							List.of("com.google:guava:32.0.0-jre"),
							true,
							null), null),
					Arguments.of("different useMavenCentral", (Function<P2ModelWrapper, P2ModelWrapper>) (m) -> createMockModel(
							List.of("https://download.eclipse.org/eclipse/updates/4.26/"),
							List.of("org.eclipse.jdt.core"),
							Set.of(),
							List.of(),
							false,
							null), null),
					Arguments.of("different cache directory", Function.<P2ModelWrapper> identity(), new File("/tmp/cache")));
		}

		@Test
		void identicalModelsDifferentInstancesUsesCache() throws IOException {
			AtomicInteger callCount = new AtomicInteger(0);
			P2Provisioner underlying = mockP2Provisioner(callCount);
			GradleProvisioner.DedupingP2Provisioner deduping = new GradleProvisioner.DedupingP2Provisioner(underlying);

			// Create two different instances with identical data
			P2ModelWrapper model1 = createMockModel(
					List.of("https://download.eclipse.org/eclipse/updates/4.26/"),
					List.of("org.eclipse.jdt.core"),
					Set.of(),
					List.of(),
					true,
					null);

			P2ModelWrapper model2 = createMockModel(
					List.of("https://download.eclipse.org/eclipse/updates/4.26/"),
					List.of("org.eclipse.jdt.core"),
					Set.of(),
					List.of(),
					true,
					null);

			List<File> result1 = deduping.provisionP2Dependencies(model1, mockProvisioner(), null);
			List<File> result2 = deduping.provisionP2Dependencies(model2, mockProvisioner(), null);

			assertThat(result1).as("Cache hit").isSameAs(result2);
			assertThat(callCount.get()).as("Only called once").isEqualTo(1);
		}

		@Test
		void cachedOnlyCacheHitReturnsResult() throws IOException {
			P2Provisioner underlying = mockP2Provisioner(new AtomicInteger(0));
			GradleProvisioner.DedupingP2Provisioner deduping = new GradleProvisioner.DedupingP2Provisioner(underlying);

			P2ModelWrapper model = createMockModel(
					List.of("https://download.eclipse.org/eclipse/updates/4.26/"),
					List.of("org.eclipse.jdt.core"),
					Set.of(),
					List.of(),
					true,
					null);

			// Populate cache
			deduping.provisionP2Dependencies(model, mockProvisioner(), null);

			// cachedOnly should return cached result
			List<File> result = deduping.cachedOnly.provisionP2Dependencies(model, mockProvisioner(), null);

			assertThat(result).isNotEmpty();
		}

		@Test
		void cachedOnlyCacheMissThrowsException() {
			P2Provisioner underlying = mockP2Provisioner(new AtomicInteger(0));
			GradleProvisioner.DedupingP2Provisioner deduping = new GradleProvisioner.DedupingP2Provisioner(underlying);

			P2ModelWrapper model = createMockModel(
					List.of("https://download.eclipse.org/eclipse/updates/4.26/"),
					List.of("org.eclipse.jdt.core"),
					Set.of(),
					List.of(),
					true,
					null);

			// cachedOnly should throw when not cached
			assertThatThrownBy(() -> deduping.cachedOnly.provisionP2Dependencies(model, mockProvisioner(), null))
					.isInstanceOf(GradleException.class)
					.hasMessageContaining("spotlessPredeclare");
		}

		private P2Provisioner mockP2Provisioner(AtomicInteger callCount) {
			return (modelWrapper, mavenProvisioner, cacheDirectory) -> {
				callCount.incrementAndGet();
				// Return a unique list based on model
				String id = String.join("-", modelWrapper.getP2Repos()) + "-" + String.join("-", modelWrapper.getInstallList());
				return List.of(new File("/mock/p2-" + id.hashCode() + ".jar"));
			};
		}

		private Provisioner mockProvisioner() {
			return (withTransitives, mavenCoordinates) -> Set.of(new File("/mock/maven.jar"));
		}

		private static P2ModelWrapper createMockModel(
				List<String> p2Repos,
				List<String> installList,
				Set<String> filterNames,
				List<String> pureMaven,
				boolean useMavenCentral,
				@Nullable File cacheDirectory) {
			P2Model model = new P2Model();
			p2Repos.forEach(model.getP2repo()::add);
			installList.forEach(model.getInstall()::add);
			filterNames.forEach(name -> model.getFilters().put(name, null)); // Filter value doesn't matter for cache key
			pureMaven.forEach(model.getPureMaven()::add);
			model.useMavenCentral = useMavenCentral;
			return P2ModelWrapper.wrap(model);
		}
	}
}
