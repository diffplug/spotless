/*
 * Copyright 2016-2023 DiffPlug
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ProvisionerTest {
	@Test
	void testManipulation() {
		Provisioner provisioner = (withTransitives, deps) -> deps.stream().map(Object::toString).map(File::new).collect(Collectors.toSet());
		Assertions.assertThat(provisioner.provisionWithTransitives(true, Collections.singleton("a")))
				.containsExactlyInAnyOrder(new File("a"));
		Assertions.assertThat(provisioner.provisionWithTransitives(true, List.of("a", "a")))
				.containsExactlyInAnyOrder(new File("a"));
	}
}
