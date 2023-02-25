/*
 * Copyright 2016-2021 DiffPlug
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
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testkit.runner.GradleRunner;

import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.common.collect.MoreCollectors;
import com.diffplug.spotless.TestProvisioner;

/**
 * If you'd like to step through the full spotless plugin,
 * these tests make that easier. Uncomment ignore to do it.
 */
public class SelfTest {
	public static void main(String[] args) throws Exception {
		runTaskManually();
		//runWithTestKit("spotlessApply");
	}

	/** Runs a full task manually, so you can step through all the logic. */
	private static void runTaskManually() throws Exception {
		Project project = createProject(extension -> {
			extension.ratchetFrom("origin/main");
			extension.java(java -> {
				java.target("src/*/java/**/*.java");
				java.licenseHeaderFile("../gradle/spotless.license");
				java.importOrderFile("../gradle/spotless.importorder");
				java.eclipse().configFile("../gradle/spotless.eclipseformat.xml");
				java.trimTrailingWhitespace();
				java.removeUnusedImports();
			});
		});
		project.getBuildscript().getRepositories().mavenCentral();
		SpotlessTaskImpl onlyTask = project.getTasks().stream()
				.filter(task -> task instanceof SpotlessTaskImpl)
				.map(task -> (SpotlessTaskImpl) task)
				.collect(MoreCollectors.singleOrEmpty()).get();
		Tasks.execute(onlyTask);
		// it will run forever with empty threads, so we have to kill it
		System.exit(0);
	}

	/** Creates a Project which has had the SpotlessExtension setup. */
	private static Project createProject(Consumer<SpotlessExtensionImpl> test) throws Exception {
		//Project project = Mocks.mockProject(TestProvisioner.gradleProject(new File("").getAbsoluteFile()), afterEvaluate);
		Project project = TestProvisioner.gradleProject(new File("").getAbsoluteFile());
		// create the spotless plugin
		project.getPlugins().apply(SpotlessPlugin.class);
		// setup the plugin
		test.accept(project.getExtensions().getByType(SpotlessExtensionImpl.class));
		// run the afterEvaluate section
		((ProjectInternal) project).getProjectEvaluationBroadcaster().afterEvaluate(project, project.getState());
		// return the configured plugin
		return project;
	}

	/** Runs against the {@code spotlessSelfApply.gradle} file. */
	static void runWithTestKit(String taskType) throws Exception {
		GradleRunner.create()
				.withPluginClasspath()
				.withProjectDir(new File(StandardSystemProperty.USER_DIR.value()).getParentFile())
				.withArguments(
						"--project-cache-dir", ".gradle-selfapply",
						taskType,
						"--stacktrace")
				.forwardOutput()
				.build();
	}
}
