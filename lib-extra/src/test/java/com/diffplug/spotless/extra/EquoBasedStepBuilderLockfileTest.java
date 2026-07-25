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
package com.diffplug.spotless.extra;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.Provisioner;

import dev.equo.solstice.p2.P2Model;

class EquoBasedStepBuilderLockfileTest {

	@Test
	void missingEmbeddedLockfileFallsBackToP2() throws Exception {
		P2Provisioner p2Provisioner = mock();
		Provisioner mavenProvisioner = mock();
		when(p2Provisioner.provisionP2Dependencies(any(), any(), any())).thenReturn(List.of());
		EquoBasedStepBuilder builder = builderWithLockfilePath("/com/diffplug/spotless/extra/missing.lockfile", p2Provisioner, mavenProvisioner);
		StepHarness.forStep(builder.build()).test("class T {}", "class T {}");
		verify(p2Provisioner).provisionP2Dependencies(any(), any(), any());
		verifyNoInteractions(mavenProvisioner);
	}

	@Test
	void lockfilePathMustBeAbsolute() {
		P2Provisioner p2Provisioner = mock();
		Provisioner mavenProvisioner = mock();
		EquoBasedStepBuilder builder = builderWithLockfilePath("com/diffplug/spotless/extra/empty.lockfile",
				p2Provisioner, mavenProvisioner);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> StepHarness.forStep(builder.build()).test("class T {}", "class T {}"));
		assertTrue(exception.getMessage().contains("must start with '/'"));
		verifyNoInteractions(p2Provisioner, mavenProvisioner);
	}

	@Test
	void emptyEmbeddedLockfileThrows() {
		P2Provisioner p2Provisioner = mock();
		Provisioner mavenProvisioner = mock();
		EquoBasedStepBuilder builder = builderWithLockfilePath("/com/diffplug/spotless/extra/empty.lockfile",
				p2Provisioner, mavenProvisioner);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> StepHarness.forStep(builder.build()).test("class T {}", "class T {}"));
		assertTrue(exception.getMessage().contains("No dependencies defined in lockfile"));
		verifyNoInteractions(p2Provisioner, mavenProvisioner);
	}

	private static EquoBasedStepBuilder builderWithLockfilePath(String lockfilePath, P2Provisioner p2Provisioner, Provisioner mavenProvisioner) {
		return new EquoBasedStepBuilder(
				"lockfile test formatter",
				mavenProvisioner,
				p2Provisioner,
				"4.40",
				state -> input -> input,
				ImmutableMap.builder()) {
			@Override
			protected P2Model model(String version) {
				return new P2Model();
			}

			@Override
			protected String lockfileResourcePath(String version) {
				return lockfilePath;
			}
		};
	}

}
