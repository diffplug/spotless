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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.util.Collections;

import org.gradle.api.Project;
import org.gradle.api.services.BuildServiceParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.TestProvisioner;

class FormatTaskTest extends ResourceHarness {
	private SpotlessTaskImpl spotlessTask;

	@BeforeEach
	void createTask() {
		Project project = TestProvisioner.gradleProject(rootFolder());
		spotlessTask = project.getTasks().create("spotlessTaskUnderTest", SpotlessTaskImpl.class);
		spotlessTask.setLineEndingsPolicy(LineEnding.UNIX.createPolicy());
		spotlessTask.init(GradleIntegrationHarness.providerOf(new SpotlessTaskService() {
			@Override
			public BuildServiceParameters.None getParameters() {
				return null;
			}
		}));
	}

	@Test
	void testLineEndings() throws Exception {
		File testFile = setFile("testFile").toContent("\r\n");
		var outputFile = new File(spotlessTask.getOutputDirectory(), "testFile");

		spotlessTask.setTarget(Collections.singleton(testFile));
		Tasks.execute(spotlessTask);

		assertFile(outputFile).hasContent("\n");
	}

	@Test
	void testStep() throws Exception {
		File testFile = setFile("testFile").toContent("apple");
		var outputFile = new File(spotlessTask.getOutputDirectory(), "testFile");
		spotlessTask.setTarget(Collections.singleton(testFile));

		spotlessTask.addStep(FormatterStep.createNeverUpToDate("double-p", content -> content.replace("pp", "p")));
		Tasks.execute(spotlessTask);

		assertFile(outputFile).hasContent("aple");
	}
}
