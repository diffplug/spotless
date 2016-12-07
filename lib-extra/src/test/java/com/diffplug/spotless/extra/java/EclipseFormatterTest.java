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
package com.diffplug.spotless.extra.java;

import java.io.File;

import org.junit.Test;

import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

public class EclipseFormatterTest extends ResourceHarness {
	@Test
	public void loadPropertiesSettings() throws Throwable {
		File eclipseFormatFile = createTestFile("java/eclipse/format/formatter.properties");
		StepHarness.forStep(EclipseFormatterStep.create(eclipseFormatFile, TestProvisioner.mavenCentral()))
			.testResource("java/eclipse/format/JavaCodeUnformatted.test", "java/eclipse/format/JavaCodeFormatted.test");
	}

	@Test
	public void loadXmlSettings() throws Throwable {
		File eclipseFormatFile = createTestFile("java/eclipse/format/formatter.xml");
		StepHarness.forStep(EclipseFormatterStep.create(eclipseFormatFile, TestProvisioner.mavenCentral()))
				.testResource("java/eclipse/format/JavaCodeUnformatted.test", "java/eclipse/format/JavaCodeFormatted.test");
	}

	@Test
	public void longLiteralProblem() throws Throwable {
		String folder = "java/eclipse/format/long_literals/";
		File eclipseFormatFile = createTestFile(folder + "spotless.eclipseformat.xml");
		StepHarness.forStep(EclipseFormatterStep.create(eclipseFormatFile, TestProvisioner.mavenCentral()))
				.testResourceUnaffected(folder + "Example1.test")
				.testResourceUnaffected(folder + "Example2.test");
	}
}
