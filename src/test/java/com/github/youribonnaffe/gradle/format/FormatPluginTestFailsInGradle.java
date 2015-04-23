package com.github.youribonnaffe.gradle.format;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

public class FormatPluginTestFailsInGradle extends ResourceTest {
	private Project project;
	private FormatTask task;

	@Before
	public void createProject() {
		project = ProjectBuilder.builder().build();
		task = (FormatTask) project.getTasks().create("underTest", FormatTask.class);
	}

	@Test
	public void formatTaskIsCreated() {
		assertTrue(project.getTasks().getByName("underTest") instanceof FormatTask);
	}

	@Test
	public void sortImportsAndFormatCode() throws Exception {
		File sourceFile = getTestFile("JavaUnsortedImportsAndCodeUnformatted.test");
		task.importsOrder = Arrays.asList("java", "javax", "org", "\\#com");
		task.eclipseFormatFile = getTestFile("formatter.properties");
		task.files = project.files(sourceFile);

		task.format();

		assertFileContent("JavaCodeSortedImportsCodeFormatted.test", sourceFile);
	}

	@Test
	public void formatJava8code() throws Exception {
		File sourceFile = getTestFile("Java8CodeUnformatted.test");
		task.eclipseFormatFile = getTestFile("formatter.properties");
		task.files = project.files(sourceFile);

		task.format();

		assertFileContent("Java8CodeFormatted.test", sourceFile);
	}
}
