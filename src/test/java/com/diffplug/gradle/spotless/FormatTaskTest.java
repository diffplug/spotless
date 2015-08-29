/*
 * Copyright 2015 DiffPlug
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

import org.junit.Before;
import org.junit.Test;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class FormatTaskTest extends ResourceTest {
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
		task.lineEndings = LineEnding.UNIX;
		task.target = Collections.singleton(createTestFile("testFile", "\r\n"));
		task.execute();
	}

	@Test
	public void testLineEndingsCheckPass() throws IOException {
		task.check = true;
		task.lineEndings = LineEnding.UNIX;
		task.target = Collections.singleton(createTestFile("testFile", "\n"));
		task.execute();
	}

	@Test
	public void testLineEndingsApply() throws IOException {
		File testFile = createTestFile("testFile", "\r\n");

		task.check = false;
		task.lineEndings = LineEnding.UNIX;
		task.target = Collections.singleton(testFile);
		task.execute();

		assertFileContent("\n", testFile);
	}

	@Test(expected = GradleException.class)
	public void testStepCheckFail() throws IOException {
		File testFile = createTestFile("testFile", "apple");
		task.target = Collections.singleton(testFile);

		task.check = true;
		task.steps.add(FormatterStep.create("double-p", new FormattingOperation() {
			@Override
			public String apply(String raw) throws Throwable {
				return raw.replace("pp", "p");
			}
		}));
		task.execute();

		assertFileContent("\n", testFile);
	}

	@Test
	public void testStepCheckPass() throws IOException {
		File testFile = createTestFile("testFile", "aple");
		task.target = Collections.singleton(testFile);

		task.check = true;
		task.steps.add(FormatterStep.create("double-p", new FormattingOperation() {
			@Override
			public String apply(String raw) throws Throwable {
				return raw.replace("pp", "p");
			}
		}));
		task.execute();
	}

	@Test
	public void testStepApply() throws IOException {
		File testFile = createTestFile("testFile", "apple");
		task.target = Collections.singleton(testFile);

		task.check = false;
		task.steps.add(FormatterStep.create("double-p", new FormattingOperation() {
			@Override
			public String apply(String raw) throws Throwable {
				return raw.replace("pp", "p");
			}
		}));
		task.execute();

		super.assertFileContent("aple", testFile);
	}
}
