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
package com.diffplug.gradle.spotless.java;

import org.junit.Test;

import org.gradle.api.GradleException;

import com.diffplug.gradle.spotless.FormattingOperation;
import com.diffplug.gradle.spotless.ResourceTest;

import java.io.File;

public class EclipseFormatterStepTest extends ResourceTest {
	@Test
	public void loadPropertiesSettings() throws Throwable {
		// setting for the formatter
		final EclipseFormatterStep step = EclipseFormatterStep.load(createTestFile("formatter.properties"));
		assertStep(new FormattingOperation() {
			@Override
			public String apply(String raw) throws Throwable {
				return step.format(raw);
			}
		}, "JavaCodeUnformatted.test", "JavaCodeFormatted.test");
	}

	@Test
	public void loadXmlSettings() throws Throwable {
		// setting for the formatter
		final EclipseFormatterStep step = EclipseFormatterStep.load(createTestFile("formatter.xml"));
		assertStep(new FormattingOperation() {
			@Override
			public String apply(String raw) throws Throwable {
				return step.format(raw);
			}
		}, "JavaCodeUnformatted.test", "JavaCodeFormatted.test");
	}

	@Test(expected = GradleException.class)
	public void loadUnknownSettings() throws Exception {
		EclipseFormatterStep.load(new File("formatter.unknown"));
	}
}
