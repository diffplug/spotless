package com.diffplug.gradle.spotless.java;

import java.io.File;

import org.gradle.api.GradleException;
import org.junit.Test;

import com.diffplug.gradle.spotless.ResourceTest;

public class EclipseFormatterStepTest extends ResourceTest {
	@Test
	public void loadPropertiesSettings() throws Exception {
		// setting for the formatter
		EclipseFormatterStep step = EclipseFormatterStep.load(getTestFile("formatter.properties"));
		assertStep(step, "JavaCodeUnformatted.test", "JavaCodeFormatted.test");
	}

	@Test
	public void loadXmlSettings() throws Exception {
		// setting for the formatter
		EclipseFormatterStep step = EclipseFormatterStep.load(getTestFile("formatter.xml"));
		assertStep(step, "JavaCodeUnformatted.test", "JavaCodeFormatted.test");
	}

	@Test(expected = GradleException.class)
	public void loadUnknownSettings() throws Exception {
		EclipseFormatterStep.load(new File("formatter.unknown"));
	}
}
