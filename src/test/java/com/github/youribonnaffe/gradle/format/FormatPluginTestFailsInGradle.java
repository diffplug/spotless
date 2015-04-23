package com.github.youribonnaffe.gradle.format;

import static org.junit.Assert.assertTrue;

import com.google.common.base.Joiner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FormatPluginTestFailsInGradle {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Project project;
	private FormatTask task;

	@Before
	public void createProject() {
		project = ProjectBuilder.builder().build();
		new FormatPlugin().apply(project);

		task = (FormatTask) project.getTasks().getByName("format");
	}

	@Test
	public void formatTaskIsCreated() {
		assertTrue(project.getTasks().getByName("format") instanceof FormatTask);
	}

	private String getTestResource(String filename) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream inputStream = getClass().getResourceAsStream("/" + filename);
		System.out.println("filename=" + filename + " inputstream=" + inputStream);
		byte[] buffer = new byte[1024];
		int length = 0;
		while ((length = inputStream.read(buffer)) != -1) {
			baos.write(buffer, 0, length);
		}
		return new String(baos.toByteArray(), StandardCharsets.UTF_8);
	}

	private File getTestFile(String filename) throws IOException {
		File file = folder.newFile(filename);
		Files.write(file.toPath(), getTestResource(filename).getBytes(StandardCharsets.UTF_8));
		return file;
	}

	private void assertFileContent(File file, String expectedFile) throws IOException {
		// This line thing is necessary for the tests to pass when Windows git screws up the line-endings
		List<String> actualLines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
		List<String> expectedLines = Arrays.asList(getTestResource(expectedFile).replace("\r", "").split("\n"));
		Assert.assertEquals(Joiner.on("\n").join(expectedLines), Joiner.on("\n").join(actualLines));
	}

	@Test
	public void sortImportsAndFormatCode() throws Exception {
		File sourceFile = getTestFile("JavaUnsortedImportsAndCodeUnformatted.test");
		task.importsOrder = Arrays.asList("java", "javax", "org", "\\#com");
		task.configurationFile = getTestFile("formatter.properties");
		task.files = project.files(sourceFile);

		task.format();

		assertFileContent(sourceFile, "JavaCodeSortedImportsCodeFormatted.test");
	}

	@Test
	public void formatJava8code() throws Exception {
		File sourceFile = getTestFile("Java8CodeUnformatted.test");
		task.configurationFile = getTestFile("formatter.properties");
		task.files = project.files(sourceFile);

		task.format();

		assertFileContent(sourceFile, "Java8CodeFormatted.test");
	}
}
