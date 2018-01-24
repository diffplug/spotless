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
package com.diffplug.maven.spotless;

import com.diffplug.spotless.Provisioner;
import org.junit.Test;

import java.io.File;
import java.util.Set;

import static com.diffplug.common.collect.Sets.newHashSet;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MavenProvisionerTest {

	@Test
	public void testProvisionWithDependenciesWhenNothingResolved() throws Exception {
		ArtifactResolver resolver = mock(ArtifactResolver.class);
		when(resolver.resolve(anyString())).thenReturn(emptySet());
		Provisioner provisioner = MavenProvisioner.create(resolver);

		Set<File> files = provisioner.provisionWithDependencies("foo", "bar", "baz");

		assertThat(files).isEmpty();
	}

	@Test
	public void testProvisionWithDependencies() throws Exception {
		ArtifactResolver resolver = mock(ArtifactResolver.class);
		when(resolver.resolve("foo")).thenReturn(newHashSet(new File("foo-1"), new File("foo-2")));
		when(resolver.resolve("bar")).thenReturn(newHashSet(new File("bar-1")));
		when(resolver.resolve("baz")).thenReturn(newHashSet(new File("baz-1"), new File("baz-2")));
		Provisioner provisioner = MavenProvisioner.create(resolver);

		Set<File> files = provisioner.provisionWithDependencies("foo", "bar", "baz");

		assertThat(files).containsOnly(new File("foo-1"), new File("foo-2"), new File("bar-1"), new File("baz-1"), new File("baz-2"));
	}

	@Test
	public void testProvisionWithDependenciesWithDuplicates() throws Exception {
		ArtifactResolver resolver = mock(ArtifactResolver.class);
		when(resolver.resolve("foo")).thenReturn(newHashSet(new File("foo-1"), new File("foo-2")));
		when(resolver.resolve("bar")).thenReturn(newHashSet(new File("foo-2")));
		when(resolver.resolve("baz")).thenReturn(newHashSet(new File("foo-1"), new File("baz-2")));
		Provisioner provisioner = MavenProvisioner.create(resolver);

		Set<File> files = provisioner.provisionWithDependencies("foo", "bar", "baz");

		assertThat(files).containsOnly(new File("foo-1"), new File("foo-2"), new File("baz-2"));
	}
}
