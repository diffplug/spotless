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
import java.util.function.Consumer;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Ignore;
import org.junit.Test;

import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.common.base.Unhandled;

/**
 * If you'd like to step through the full spotless plugin,
 * these tests make that easier. Uncomment ignore to do it.
 */
@Ignore
public class SelfTest {
	public enum Type {
		CHECK, APPLY;

		public <T> T checkApply(T check, T apply) {
			switch (this) {
			case CHECK:
				return check;
			case APPLY:
				return apply;
			default:
				throw Unhandled.enumException(this);
			}
		}
	}

	@Test
	public void spotlessApply() throws Exception {
		runTasksManually(Type.APPLY);
	}

	@Test
	public void spotlessCheck() throws Exception {
		runTasksManually(Type.CHECK);
	}

	/** Runs a full task manually, so you can step through all the logic. */
	static void runTasksManually(Type type) throws Exception {
		Project project = createProject(extension -> {
			extension.java(java -> {
				java.target("**/*.java");
				java.licenseHeaderFile("spotless.license.java");
				java.importOrderFile("spotless.importorder");
				java.eclipseFormatFile("spotless.eclipseformat.xml");
				java.trimTrailingWhitespace();
				java.customLazy("Lambda fix", () -> raw -> {
					if (!raw.contains("public class SelfTest ")) {
						// don't format this line away, lol
						return raw.replace("} )", "})").replace("} ,", "},");
					} else {
						return raw;
					}
				});
			});
			extension.format("misc", misc -> {
				misc.target("**/*.gradle", "**/*.md", "**/*.gitignore");
				misc.indentWithTabs();
				misc.trimTrailingWhitespace();
				misc.endWithNewline();
			});
		});
		project.getTasks().stream()
				.filter(task -> task instanceof FormatTask)
				.map(task -> (FormatTask) task)
				.filter(task -> task.check == type.checkApply(true, false))
				.forEach(task -> {
					try {
						task.format();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
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

	/** Runs against the `spotlessSelfApply.gradle` file. */
	static void runWithTestKit(Type type) throws Exception {
		GradleRunner.create()
				.withPluginClasspath()
				.withProjectDir(new File(StandardSystemProperty.USER_DIR.value()))
				.withArguments("-b", "spotlessSelf.gradle", "spotless" + type.checkApply("Check", "Apply"), "--stacktrace")
				.forwardOutput()
				.build();
	}
}
