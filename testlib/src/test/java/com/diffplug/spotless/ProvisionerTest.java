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
import java.util.Arrays;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ProvisionerTest {
	@Test
	@Deprecated
	public void testManipulationDeprecated() {
		Provisioner provisioner = deps -> deps.stream().map(File::new).collect(Collectors.toSet());
		Assertions.assertThat(provisioner.provisionWithDependencies("a"))
				.containsExactlyInAnyOrder(new File("a"));
		Assertions.assertThat(provisioner.provisionWithDependencies("a", "a"))
				.containsExactlyInAnyOrder(new File("a"));
		Assertions.assertThat(provisioner.provisionWithDependencies(Arrays.asList("a", "a")))
				.containsExactlyInAnyOrder(new File("a"));
	}

	@Test
	public void testManipulation() {
		Provisioner provisioner = deps -> deps.stream().map(File::new).collect(Collectors.toSet());
		Assertions.assertThat(provisioner.provide(true, "a"))
				.containsExactlyInAnyOrder(new File("a"));
		Assertions.assertThat(provisioner.provide(true, "a", "a"))
				.containsExactlyInAnyOrder(new File("a"));
		Assertions.assertThat(provisioner.provide(true, Arrays.asList("a", "a")))
				.containsExactlyInAnyOrder(new File("a"));
	}
}
