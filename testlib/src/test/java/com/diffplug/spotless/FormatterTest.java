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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.spotless.generic.EndWithNewlineStep;

public class FormatterTest {
	// Formatter normally needs to be closed, but no resources will be leaked in this special case
	@Test
	public void equality() {
		new SerializableEqualityTester() {
			private LineEnding.Policy lineEndingsPolicy = LineEnding.UNIX.createPolicy();
			private Charset encoding = StandardCharsets.UTF_8;
			private Path rootDir = Paths.get(StandardSystemProperty.USER_DIR.value());
			private List<FormatterStep> steps = new ArrayList<>();
			private FormatExceptionPolicy exceptionPolicy = FormatExceptionPolicy.failOnlyOnError();

			@Override
			protected void setupTest(API api) throws Exception {
				api.areDifferentThan();

				lineEndingsPolicy = LineEnding.WINDOWS.createPolicy();
				api.areDifferentThan();

				encoding = StandardCharsets.UTF_16;
				api.areDifferentThan();

				rootDir = rootDir.getParent();
				api.areDifferentThan();

				steps.add(EndWithNewlineStep.create());
				api.areDifferentThan();

				{
					FormatExceptionPolicyStrict standard = new FormatExceptionPolicyStrict();
					standard.excludePath("path");
					exceptionPolicy = standard;
					api.areDifferentThan();
				}

				{
					FormatExceptionPolicyStrict standard = new FormatExceptionPolicyStrict();
					standard.excludeStep("step");
					exceptionPolicy = standard;
					api.areDifferentThan();
				}
			}

			@Override
			protected Formatter create() {
				return Formatter.builder()
						.lineEndingsPolicy(lineEndingsPolicy)
						.encoding(encoding)
						.rootDir(rootDir)
						.steps(steps)
						.exceptionPolicy(exceptionPolicy)
						.build();
			}
		}.testEquals();
	}
}
