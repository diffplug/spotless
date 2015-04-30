/**
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
package com.diffplug.gradle.spotless.java;

import java.io.File;

import org.gradle.api.GradleException;
import org.junit.Test;

import com.diffplug.gradle.spotless.ResourceTest;

public class EclipseFormatterStepTest extends ResourceTest {
	@Test
	public void loadPropertiesSettings() throws Throwable {
		// setting for the formatter
		EclipseFormatterStep step = EclipseFormatterStep.load(createTestFile("formatter.properties"));
		assertStep(step::format, "JavaCodeUnformatted.test", "JavaCodeFormatted.test");
	}

	@Test
	public void loadXmlSettings() throws Throwable {
		// setting for the formatter
		EclipseFormatterStep step = EclipseFormatterStep.load(createTestFile("formatter.xml"));
		assertStep(step::format, "JavaCodeUnformatted.test", "JavaCodeFormatted.test");
	}

	@Test(expected = GradleException.class)
	public void loadUnknownSettings() throws Exception {
		EclipseFormatterStep.load(new File("formatter.unknown"));
	}
}
