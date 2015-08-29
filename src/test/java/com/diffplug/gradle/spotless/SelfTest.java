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
package com.diffplug.gradle.spotless;

import com.diffplug.gradle.spotless.java.JavaExtension;
import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

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
	/** Applies the format (which should be manual). */
	public static void main(String[] args) throws Exception {
		runTasksWithCheck(false);
	}

	/** Returns all of the FormatTasks which have check equal to the given value. */
	private static void runTasksWithCheck(boolean check) throws Exception {
		Project project = createProject(new SimpleConsumer<SpotlessExtension>() {
			@Override
			public void accept(SpotlessExtension extension) {
				extension.java(new Closure<JavaExtension>(this) {
					@Override
					public JavaExtension call() {
						final JavaExtension javaExt = (JavaExtension) getDelegate();
						javaExt.target("**/*.java");
						javaExt.licenseHeaderFile("spotless.license.java");
						javaExt.importOrderFile("spotless.importorder");
						javaExt.eclipseFormatFile("spotless.eclipseformat.xml");
						javaExt.customLazy("Lambda fix", new FormattingOperationSupplier(new FormattingOperation() {
							@Override
							public String apply(String raw) throws Throwable {
								if (!raw.contains("public class SelfTest ")) {
									// don't format this line away, lol
									return raw.replace("} )", "})").replace("} ,", "},");
								} else {
									return raw;
								}
							}
						}));
						return javaExt;
					}
				});
				extension.format("misc", new Closure<FormatExtension>(this) {
					@Override
					public FormatExtension call() {
						FormatExtension misc = (FormatExtension) getDelegate();
						misc.target("**/*.gradle", "**/*.md", "**/*.gitignore");
						misc.indentWithTabs();
						misc.trimTrailingWhitespace();
						misc.endWithNewline();
						return misc;
					}
				});
			}
		});

		for (Task task : project.getTasks()) {
			if (task instanceof FormatTask) {
				FormatTask formatTask = (FormatTask) task;
				if (formatTask.check == check) {
					try {
						formatTask.format();
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	/** Creates a Project which has had the SpotlessExtension setup. */
	private static Project createProject(SimpleConsumer<SpotlessExtension> test) throws Exception {
		Project project = ProjectBuilder.builder().withProjectDir(new File("").getAbsoluteFile()).build();
		// create the spotless plugin
		SpotlessPlugin plugin = project.getPlugins().apply(SpotlessPlugin.class);
		// setup the plugin
		test.accept(plugin.getExtension());
		plugin.createTasks();
		// return the configured plugin
		return project;
	}

	/** Runs the check (which we want to happen in the test suite). */
	@Test
	@Ignore("The test passes in real life and Eclipse, but fails in Gradle test runner...")
	public void check() throws Exception {
		try {
			runTasksWithCheck(true);
		}
		catch (Exception e) {
			throw new Exception("There are formatting errors in spotless' source code.\n" + "Ideally, you could just run 'spotlessApply', but because of an unresolved bootstrapping issue, you'll have to manually run the " + "main() method in com.diffplug.gradle.spotless.SelfTest", e);
		}
	}
}
