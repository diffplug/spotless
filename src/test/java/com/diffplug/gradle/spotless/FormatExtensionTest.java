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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;

public abstract class FormatExtensionTest extends ResourceTest {
	/** Tests that the formatExtension causes the given change. */
	protected void assertTask(Consumer<FormatExtension> test, String before, String afterExpected) throws Exception {
		// create the task
		FormatTask task = createTask(test);
		// create the test file
		File testFile = folder.newFile();
		Files.write(testFile.toPath(), before.getBytes(StandardCharsets.UTF_8));
		// set the task to use this test file
		task.target = Collections.singleton(testFile);
		// run the task
		task.format();
		// check what the task did
		String afterActual = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
		Assert.assertEquals(afterExpected, afterActual);
	}

	/** Creates a FormatTask based on the given consumer. */
	private static FormatTask createTask(Consumer<FormatExtension> test) throws Exception {
		Project project = ProjectBuilder.builder().build();
		SpotlessPlugin plugin = project.getPlugins().apply(SpotlessPlugin.class);

		AtomicReference<FormatExtension> ref = new AtomicReference<>();
		plugin.getExtension().format("underTest", ext -> {
			ref.set(ext);
			test.accept(ext);
		});

		boolean check = false;
		return plugin.createTask("underTest", ref.get(), check);
	}
}
