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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.util.function.Consumer;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Running spotless on ourselves yields the following error:
 * 
 * Module version com.diffplug.gradle.spotless:spotless:1.0-SNAPSHOT,
 * configuration 'classpath' declares a dependency on configuration
 * 'default' which is not declared in the module descriptor for
 * com.diffplug.gradle.spotless:spotless:1.0-SNAPSHOT
 * 
 * Tried all kinds of things to fix it, no luck so far.
 * 
 * So, we'll just run it from inside of ourselves.
 */
public class SelfTest {
	/** Runs the check (which we want to happen in the test suite). */
	@Test @Ignore("The test passes in real life and Eclipse, but fails in Gradle test runner...")
	public void check() throws Exception {
		try {
			runTasksWithCheck(true);
		} catch (Exception e) {
			throw new Exception("There are formatting errors in spotless' source code.\n" + "Ideally, you could just run 'spotlessApply', but because of an unresolved bootstrapping issue, you'll have to manually run the " + "main() method in com.diffplug.gradle.spotless.SelfTest", e);
		}
	}

	/** Applies the format (which should be manual). */
	public static void main(String[] args) throws Exception {
		runTasksWithCheck(false);
	}

	/** Returns all of the FormatTasks which have check equal to the given value. */
	private static void runTasksWithCheck(boolean check) throws Exception {
		Project project = createProject(extension -> {
			extension.java(java -> {
				java.target("**/*.java");
				java.licenseHeaderFile("spotless.license.java");
				java.importOrderFile("spotless.importorder.properties");
				java.eclipseFormatFile("spotless.eclipseformat.xml");
				java.customLazy("Lambda fix", () -> raw -> {
					if (!raw.contains("public class SelfTest ")) {
						// don't format this line away, lol
						return raw.replace("} )", "})").replace("} ,", "},");
					} else {
						return raw;
					}
				} );
			} );
			extension.format("misc", misc -> {
				misc.target("**/*.gradle", "**/*.md", "**/*.gitignore");
				misc.indentWithTabs();
				misc.trimTrailingWhitespace();
				misc.endWithNewline();
			} );
		} );
		project.getTasks().stream()
				.filter(task -> task instanceof FormatTask)
				.map(task -> (FormatTask) task)
				.filter(task -> task.check == check)
				.forEach(task -> {
					try {
						task.format();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				} );
	}

	/** Creates a Project which has had the SpotlessExtension setup. */
	private static Project createProject(Consumer<SpotlessExtension> test) throws Exception {
		Project project = ProjectBuilder.builder().withProjectDir(new File("").getAbsoluteFile()).build();
		// create the spotless plugin
		SpotlessPlugin plugin = project.getPlugins().apply(SpotlessPlugin.class);
		// setup the plugin
		test.accept(plugin.getExtension());
		plugin.createTasks();
		// return the configured plugin
		return project;
	}
}
