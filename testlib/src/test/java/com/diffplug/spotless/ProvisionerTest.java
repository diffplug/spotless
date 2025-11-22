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

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ProvisionerTest {
	@Test
	void testManipulation() {
		Provisioner provisioner = (withTransitives, deps) -> deps.stream().map(File::new).collect(toSet());
		assertThat(provisioner.provisionWithTransitives(true, "a"))
				.containsExactlyInAnyOrder(new File("a"));
		assertThat(provisioner.provisionWithTransitives(true, "a", "a"))
				.containsExactlyInAnyOrder(new File("a"));
		assertThat(provisioner.provisionWithTransitives(true, Arrays.asList("a", "a")))
				.containsExactlyInAnyOrder(new File("a"));
	}
}
