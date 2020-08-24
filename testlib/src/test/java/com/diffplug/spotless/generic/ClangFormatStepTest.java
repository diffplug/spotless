/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.spotless.generic;

import java.io.File;
import java.util.Arrays;

import org.junit.Test;

import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.cpp.ClangFormatStep;

public class ClangFormatStepTest {
	@Test
	public void test() throws Exception {
		try (StepHarnessWithFile harness = StepHarnessWithFile.forStep(ClangFormatStep.withVersion(ClangFormatStep.defaultVersion()).create())) {
			// can't be named java or it gets compiled into .class file
			harness.testResource(new File("example.java"), "clang/example.java.dirty", "clang/example.java.clean");
			// test every other language clang supports
			for (String ext : Arrays.asList("c", "cs", "js", "m", "proto")) {
				String filename = "example." + ext;
				String root = "clang/" + filename;
				harness.testResource(new File(filename), root, root + ".clean");
			}
		}
	}
}
