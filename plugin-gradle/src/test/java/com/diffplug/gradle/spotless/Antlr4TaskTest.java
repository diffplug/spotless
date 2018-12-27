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

import static com.diffplug.gradle.spotless.Tasks.execute;

import java.util.Collections;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.extra.antlr4.Antlr4FormatterStep;

public class Antlr4TaskTest extends ResourceHarness {
	private SpotlessTask checkTask;
	private SpotlessTask applyTask;

	@Before
	public void createTask() {
		Project project = ProjectBuilder.builder().build();
		checkTask = project.getTasks().create("checkTaskUnderTest", SpotlessTask.class);
		checkTask.setCheck();
		applyTask = project.getTasks().create("applyTaskUnderTest", SpotlessTask.class);
		applyTask.setApply();
	}

	@Test(expected = GradleException.class)
	public void testFormatCheckFail() throws Exception {
		String unformatted = getTestResource("antlr4/Hello.unformatted.g4");

		checkTask.addStep(Antlr4FormatterStep.create());
		checkTask.setTarget(Collections.singleton(setFile("testFile.g4").toContent(unformatted)));
		execute(checkTask);
	}

	@Test
	public void testFormatedCheckPass() throws Exception {
		String formatted = getTestResource("antlr4/Hello.formatted.g4");

		checkTask.addStep(Antlr4FormatterStep.create());
		checkTask.setTarget(Collections.singleton(setFile("testFile.g4").toContent(formatted)));
		execute(checkTask);
	}

	@Test
	public void testFormatApplyPass() throws Exception {
		String unformatted = getTestResource("antlr4/Hello.unformatted.g4");
		String formatted = getTestResource("antlr4/Hello.formatted.g4");
		String testFile = "testFile.g4";

		applyTask.addStep(Antlr4FormatterStep.create());
		applyTask.setTarget(Collections.singleton(setFile(testFile).toContent(unformatted)));
		execute(applyTask);

		assertFile(testFile).hasContent(formatted);
	}
}
