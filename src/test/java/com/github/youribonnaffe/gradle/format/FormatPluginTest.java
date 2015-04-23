package com.github.youribonnaffe.gradle.format;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

public class FormatPluginTest extends ResourceTest {

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
	public void loadPropertiesSettings() throws Exception {
		// setting for the formatter
		task.eclipseFormatFile = getTestFile("formatter.properties");
		File sourceFile = getTestFile("JavaCodeUnformatted.test");
		task.files = project.files(sourceFile);
		task.format();
		assertFileContent("JavaCodeFormatted.test", sourceFile);
	}

	@Test
	public void loadXmlSettings() throws Exception {
		// setting for the formatter
		task.eclipseFormatFile = getTestFile("formatter.xml");
		File sourceFile = getTestFile("JavaCodeUnformatted.test");
		task.files = project.files(sourceFile);
		task.format();
		assertFileContent("JavaCodeFormatted.test", sourceFile);
	}

	@Test(expected = GradleException.class)
	public void loadUnknownSettings() throws Exception {
		task.eclipseFormatFile = new File("formatter.unknown");
		task.format();
	}

	@Test
	public void loadNullSettings() throws Exception {
		File sourceFile = getTestFile("JavaCodeUnformatted.test");
		task.files = project.files(sourceFile);
		task.format();
		assertFileContent("JavaCodeFormattedDefaultSettings.test", sourceFile);
	}

	@Test
	public void checkPasses() throws Exception {
		File sourceFile = getTestFile("JavaCodeFormattedDefaultSettings.test");
		task.files = project.files(sourceFile);
		task.justCheck = true;
		task.format();
	}

	@Test(expected = GradleException.class)
	public void checkFails() throws Exception {
		File sourceFile = getTestFile("JavaCodeUnformatted.test");
		task.files = project.files(sourceFile);
		task.justCheck = true;
		task.format();
	}

	@Test
	public void sortImports() throws Exception {
		File sourceFile = getTestFile("JavaCodeUnsortedImports.test");
		task.files = project.files(sourceFile);
		task.importsOrder = Arrays.asList("java", "javax", "org", "\\#com");
		task.format();
		assertFileContent("JavaCodeSortedImports.test", sourceFile);
	}

	@Test
	public void sortImportsReadingEclipseFile() throws Exception {
		File sourceFile = getTestFile("JavaCodeUnsortedImports.test");
		task.files = project.files(sourceFile);
		task.importsOrderFile = getTestFile("import.properties");
		task.format();
		assertFileContent("JavaCodeSortedImports.test", sourceFile);
	}
}
