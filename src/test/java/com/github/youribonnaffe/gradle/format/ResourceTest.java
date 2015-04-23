package com.github.youribonnaffe.gradle.format;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class ResourceTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	protected String getTestResource(String filename) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream inputStream = getClass().getResourceAsStream("/" + filename);
		byte[] buffer = new byte[1024];
		int length = 0;
		while ((length = inputStream.read(buffer)) != -1) {
			baos.write(buffer, 0, length);
		}
		return new String(baos.toByteArray(), StandardCharsets.UTF_8);
	}

	protected File getTestFile(String filename) throws IOException {
		File file = folder.newFile(filename);
		Files.write(file.toPath(), getTestResource(filename).getBytes(StandardCharsets.UTF_8));
		return file;
	}

	protected void assertFileContent(String expectedFile, File actual) throws IOException {
		// This line thing is necessary for the tests to pass when Windows git screws up the line-endings
		String expectedContent = getTestResource(expectedFile).replace("\r", "");
		String actualContent = new String(Files.readAllBytes(actual.toPath()), StandardCharsets.UTF_8).replace("\r",
				"");
		Assert.assertEquals(expectedContent, actualContent);
	}

	protected void assertContent(String key, String actualContent) throws IOException {
		// This line thing is necessary for the tests to pass when Windows git screws up the line-endings
		Assert.assertEquals(getTestResource(key).replace("\r", ""), actualContent.replace("\r", ""));
	}
}
