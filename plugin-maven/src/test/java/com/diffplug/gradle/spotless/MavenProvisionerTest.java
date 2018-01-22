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
package com.diffplug.gradle.spotless;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Set;

import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.Test;

import com.diffplug.spotless.Provisioner;

public class MavenProvisionerTest {

	@Test
	public void testProvisionWithDependencies() throws Exception {
		ArtifactResolver resolver = mock(ArtifactResolver.class);
		when(resolver.resolve("foo")).thenReturn(new File("foo"));
		when(resolver.resolve("bar")).thenReturn(new File("bar"));
		when(resolver.resolve("baz")).thenReturn(new File("baz"));
		Provisioner provisioner = MavenProvisioner.create(resolver);

		Set<File> files = provisioner.provisionWithDependencies("foo", "bar", "baz");

		assertThat(files).containsOnly(new File("foo"), new File("bar"), new File("baz"));
	}

	@Test
	public void testProvisionWithDependenciesThrowsCheckedException() throws Exception {
		ArtifactResolver resolver = mock(ArtifactResolver.class);
		when(resolver.resolve("foo")).thenThrow(new ArtifactResolutionException(emptyList(), "Error!"));
		Provisioner provisioner = MavenProvisioner.create(resolver);

		try {
			provisioner.provisionWithDependencies("foo");
			fail("Exception expected");
		} catch (RuntimeException e) {
			assertThat(e.getCause()).isInstanceOf(ArtifactResolutionException.class);
			assertThat(e.getCause().getMessage()).isEqualTo("Error!");
		}
	}

	@Test
	public void testProvisionWithDependenciesThrowsUncheckedException() throws Exception {
		ArtifactResolver resolver = mock(ArtifactResolver.class);
		when(resolver.resolve("foo")).thenThrow(new RuntimeException("Wrong coordinate!"));

		Provisioner provisioner = MavenProvisioner.create(resolver);

		try {
			provisioner.provisionWithDependencies("foo");
			fail("Exception expected");
		} catch (RuntimeException e) {
			assertThat(e.getMessage()).isEqualTo("Wrong coordinate!");
		}
	}
}
