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

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

public class FormatTaskTest extends ResourceHarness {
	private Project project;
	private FormatTask task;

	@Before
	public void createTask() {
		project = ProjectBuilder.builder().build();
		task = project.getTasks().create("underTest", FormatTask.class);
	}

	@Test(expected = GradleException.class)
	public void testLineEndingsCheckFail() throws IOException {
		task.check = true;
		task.lineEndingsPolicy = LineEnding.UNIX.createPolicy();
		task.target = Collections.singleton(createTestFile("testFile", "\r\n"));
		task.execute();
	}

	@Test
	public void testLineEndingsCheckPass() throws IOException {
		task.check = true;
		task.lineEndingsPolicy = LineEnding.UNIX.createPolicy();
		task.target = Collections.singleton(createTestFile("testFile", "\n"));
		task.execute();
	}

	@Test
	public void testLineEndingsApply() throws IOException {
		File testFile = createTestFile("testFile", "\r\n");

		task.check = false;
		task.lineEndingsPolicy = LineEnding.UNIX.createPolicy();
		task.target = Collections.singleton(testFile);
		task.execute();

		assertFileContent("\n", testFile);
	}

	@Test
	public void testStepCheckFail() throws IOException {
		File testFile = createTestFile("testFile", "apple");
		task.target = Collections.singleton(testFile);

		task.check = true;
		task.steps.add(FormatterStep.create("double-p", content -> content.replace("pp", "p")));

		String diff = String.join("\n",
				"    @@ -1 +1 @@",
				"    -apple",
				"    +aple");
		Assertions.assertThatThrownBy(() -> task.execute()).hasStackTraceContaining(diff);

		assertFileContent("apple", testFile);
	}

	@Test
	public void testStepCheckPass() throws IOException {
		File testFile = createTestFile("testFile", "aple");
		task.target = Collections.singleton(testFile);

		task.check = true;
		task.steps.add(FormatterStep.create("double-p", content -> content.replace("pp", "p")));
		task.execute();

		assertFileContent("aple", testFile);
	}

	@Test
	public void testStepApply() throws IOException {
		File testFile = createTestFile("testFile", "apple");
		task.target = Collections.singleton(testFile);

		task.check = false;
		task.steps.add(FormatterStep.create("double-p", content -> content.replace("pp", "p")));
		task.execute();

		assertFileContent("aple", testFile);
	}
}
