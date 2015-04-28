package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

public class FormatTaskTest extends ResourceTest {
	private Project project;
	private FormatTask task;

	@Before
	public void createTask() {
		project = ProjectBuilder.builder().build();
		task = (FormatTask) project.getTasks().create("underTest", FormatTask.class);
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
		task.steps.add(FormatterStep.create("double-p", content -> content.replace("pp", "p")));
		task.execute();

		assertFileContent("\n", testFile);
	}

	@Test
	public void testStepCheckPass() throws IOException {
		File testFile = createTestFile("testFile", "aple");
		task.target = Collections.singleton(testFile);

		task.check = true;
		task.steps.add(FormatterStep.create("double-p", content -> content.replace("pp", "p")));
		task.execute();
	}

	@Test
	public void testStepApply() throws IOException {
		File testFile = createTestFile("testFile", "apple");
		task.target = Collections.singleton(testFile);

		task.check = false;
		task.steps.add(FormatterStep.create("double-p", content -> content.replace("pp", "p")));
		task.execute();

		super.assertFileContent("aple", testFile);
	}
}
