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
package com.diffplug.spotless.java;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.TestProvisioner;

class PrinceOfSpaceStepTest extends ResourceHarness {

	private static String format(FormatterStep step, String input) {
		try (Formatter formatter = Formatter.builder()
				.steps(Collections.singletonList(step))
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.encoding(StandardCharsets.UTF_8)
				.build()) {
			return formatter.compute(LineEnding.toUnix(input), new File(""));
		}
	}

	@Test
	void behavior() throws Exception {
		FormatterStep step = PrinceOfSpaceStep.create(TestProvisioner.mavenCentral());
		String input = "class T { void m() { int x=1;} }";
		String formatted = format(step, input);
		assertThat(formatted).contains("int x = 1;");
		assertThat(format(step, formatted)).isEqualTo(formatted);
	}

	@Test
	void behaviorWithOptions() throws Exception {
		PrinceOfSpaceStep.Options options = new PrinceOfSpaceStep.Options();
		options.setIndentSize(2);
		FormatterStep step = PrinceOfSpaceStep.create(PrinceOfSpaceStep.defaultVersion(), TestProvisioner.mavenCentral(), options);
		String input = "class T { void m() { int x=1;} }";
		String formatted = format(step, input);
		assertThat(formatted).contains("int x = 1;").contains("\n  ");
		assertThat(format(step, formatted)).isEqualTo(formatted);
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			String version = PrinceOfSpaceStep.defaultVersion();
			Integer indentSize;

			@Override
			protected void setupTest(API api) {
				// same version == same
				api.areDifferentThan();

				// change the version, and it's different
				version = "2.1.1";
				api.areDifferentThan();
				version = PrinceOfSpaceStep.defaultVersion();

				// change the options, and it's different
				indentSize = 4;
				api.areDifferentThan();
				indentSize = null;
			}

			@Override
			protected FormatterStep create() {
				PrinceOfSpaceStep.Options options;
				if (indentSize == null) {
					options = null;
				} else {
					options = new PrinceOfSpaceStep.Options();
					options.setIndentSize(indentSize);
				}
				return PrinceOfSpaceStep.create(version, TestProvisioner.mavenCentral(), options);
			}
		}.testEquals();
	}
}
