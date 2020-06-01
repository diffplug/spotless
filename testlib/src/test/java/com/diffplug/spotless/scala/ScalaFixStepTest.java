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
package com.diffplug.spotless.scala;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.diffplug.spotless.*;

/**
 * The rules used in these tests can run directly on source code without compilation.
 * Other tests that need compilation are written in ScalaExtensionTest.
 */
public class ScalaFixStepTest extends ResourceHarness {
	private ScalaCompiler scalaCompiler;
	private String originalDir;

	@Before
	public void before() throws IOException {
		scalaCompiler = new ScalaCompiler("2.13.2", "");
		originalDir = System.getProperty("user.dir");
		final File configFile = createTestFile("scala/scalafix/.scalafix.conf");
		// Scalafix uses "user.dir" as the working directory
		System.setProperty("user.dir", configFile.getParentFile().getAbsolutePath());
	}

	@After
	public void after() {
		System.setProperty("user.dir", originalDir);
	}

	@Test
	public void behaviorDefaultConfigFilename() throws Exception {
		StepHarness.forStep(ScalaFixStep.create("0.9.16", "2.12.11", TestProvisioner.mavenCentral(), scalaCompiler, null))
				.test(createTestFile("scala/scalafix/basic.dirty"), createTestFile("scala/scalafix/basic.clean"));
	}

	@Test
	public void behaviorCustomConfigFilename() throws Exception {
		StepHarness.forStep(ScalaFixStep.create("0.9.16", "2.12.11", TestProvisioner.mavenCentral(), scalaCompiler, createTestFile("scala/scalafix/.scalafix2.conf")))
				.test(createTestFile("scala/scalafix/basic.dirty"), createTestFile("scala/scalafix/basic.clean2"));
	}

	@Test
	public void behaviorDefaultConfigFilename_0_9_1() throws Exception {
		StepHarness.forStep(ScalaFixStep.create("0.9.1", "2.11.12", TestProvisioner.mavenCentral(), scalaCompiler, null))
				.test(createTestFile("scala/scalafix/basic.dirty"), createTestFile("scala/scalafix/basic.clean"));
	}

	@Test
	public void behaviorCustomConfigFilename_0_9_1() throws Exception {
		StepHarness.forStep(ScalaFixStep.create("0.9.1", "2.11.12", TestProvisioner.mavenCentral(), scalaCompiler, createTestFile("scala/scalafix/.scalafix2.conf")))
				.test(createTestFile("scala/scalafix/basic.dirty"), createTestFile("scala/scalafix/basic.clean2"));
	}
}
