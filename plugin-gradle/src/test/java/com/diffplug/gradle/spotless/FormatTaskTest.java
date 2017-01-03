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

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ResourceHarness;

public class FormatTaskTest extends ResourceHarness {
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
	public void testLineEndingsCheckFail() throws IOException {
		checkTask.setLineEndingsPolicy(LineEnding.UNIX.createPolicy());
		checkTask.setTarget(Collections.singleton(createTestFile("testFile", "\r\n")));
		checkTask.execute();
	}

	@Test
	public void testLineEndingsCheckPass() throws IOException {
		checkTask.setLineEndingsPolicy(LineEnding.UNIX.createPolicy());
		checkTask.setTarget(Collections.singleton(createTestFile("testFile", "\n")));
		checkTask.execute();
	}

	@Test
	public void testLineEndingsApply() throws IOException {
		File testFile = createTestFile("testFile", "\r\n");

		applyTask.setLineEndingsPolicy(LineEnding.UNIX.createPolicy());
		applyTask.setTarget(Collections.singleton(testFile));
		applyTask.execute();

		assertFileContent("\n", testFile);
	}

	@Test
	public void testStepCheckFail() throws IOException {
		File testFile = createTestFile("testFile", "apple");
		checkTask.setTarget(Collections.singleton(testFile));

		checkTask.addStep(FormatterStep.createNeverUpToDate("double-p", content -> content.replace("pp", "p")));

		String diff = String.join("\n",
				"        @@ -1 +1 @@",
				"        -apple",
				"        +aple");
		Assertions.assertThatThrownBy(() -> checkTask.execute()).hasStackTraceContaining(diff);

		assertFileContent("apple", testFile);
	}

	@Test
	public void testStepCheckPass() throws IOException {
		File testFile = createTestFile("testFile", "aple");
		checkTask.setTarget(Collections.singleton(testFile));

		checkTask.addStep(FormatterStep.createNeverUpToDate("double-p", content -> content.replace("pp", "p")));
		checkTask.execute();

		assertFileContent("aple", testFile);
	}

	@Test
	public void testStepApply() throws IOException {
		File testFile = createTestFile("testFile", "apple");
		applyTask.setTarget(Collections.singleton(testFile));

		applyTask.addStep(FormatterStep.createNeverUpToDate("double-p", content -> content.replace("pp", "p")));
		applyTask.execute();

		assertFileContent("aple", testFile);
	}
}
