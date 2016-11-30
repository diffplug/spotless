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
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Ignore;
import org.junit.Test;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StandardSystemProperty;

/**
 * If you'd like to step through the full spotless plugin,
 * these tests make that easier. Uncomment ignore to do it.
 */
@Ignore
public class SelfTest {
	enum Type {
		CHECK {
			@Override
			public void runAllTasks(Project project) {
				project.getTasks().stream()
						.filter(task -> task instanceof CheckFormatTask)
						.map(task -> (CheckFormatTask) task)
						.forEach(task -> Errors.rethrow().run(() -> {
							IncrementalTaskInputs inputs = Mocks.mockIncrementalTaskInputs(task.getTarget());
							task.check(inputs);
						}));
			}

			@Override
			public <T> T checkApply(T check, T apply) {
				return check;
			}
		},
		APPLY {
			@Override
			public void runAllTasks(Project project) {
				project.getTasks().stream()
						.filter(task -> task instanceof ApplyFormatTask)
						.map(task -> (ApplyFormatTask) task)
						.forEach(task -> Errors.rethrow().run(task::apply));
			}

			@Override
			public <T> T checkApply(T check, T apply) {
				return apply;
			}
		};

		public abstract void runAllTasks(Project project);

		public abstract <T> T checkApply(T check, T apply);
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
	private static void runTasksManually(Type type) throws Exception {
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
		type.runAllTasks(project);
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
				.withProjectDir(new File(StandardSystemProperty.USER_DIR.value()).getParentFile())
				.withArguments("-b", "spotlessSelf.gradle", "spotless" + type.checkApply("Check", "Apply"), "--stacktrace")
				.forwardOutput()
				.build();
	}
}
