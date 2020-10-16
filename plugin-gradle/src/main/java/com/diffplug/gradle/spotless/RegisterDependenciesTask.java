/*
 * Copyright 2016-2020 DiffPlug
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.io.Files;
import com.diffplug.spotless.FormatterStep;

/**
 * NOT AN END-USER TASK, DO NOT USE FOR ANYTHING!
 *
 * - When a user asks for a formatter, we need to download the jars for that formatter
 * - Gradle wants us to resolve all our dependencies in the root project - no new dependencies in subprojects
 * - So, whenever a SpotlessTask in a subproject gets configured, we call {@link #hookSubprojectTask(SpotlessTask)},
 *   which makes this task a dependency of the SpotlessTask
 * - When this "registerDependencies" task does its up-to-date check, it queries the task execution graph to see which
 *   SpotlessTasks are at risk of being executed, and causes them all to be evaluated safely in the root buildscript.
 */
public class RegisterDependenciesTask extends DefaultTask {
	static final String TASK_NAME = "spotlessInternalRegisterDependencies";

	@Input
	public List<FormatterStep> getSteps() {
		List<FormatterStep> allSteps = new ArrayList<>();
		TaskExecutionGraph taskGraph = getProject().getGradle().getTaskGraph();
		for (SpotlessTask task : tasks) {
			if (taskGraph.hasTask(task)) {
				allSteps.addAll(task.getSteps());
			}
		}
		return allSteps;
	}

	private List<SpotlessTask> tasks = new ArrayList<>();

	@Internal
	public List<SpotlessTask> getTasks() {
		return tasks;
	}

	void hookSubprojectTask(SpotlessTask task) {
		tasks.add(task);
		task.dependsOn(this);
	}

	File unitOutput;

	@OutputFile
	public File getUnitOutput() {
		return unitOutput;
	}

	GradleProvisioner.RootProvisioner rootProvisioner;

	@Internal
	public GradleProvisioner.RootProvisioner getRootProvisioner() {
		return rootProvisioner;
	}

	void setup() {
		Preconditions.checkArgument(getProject().getRootProject() == getProject(), "Can only be used on the root project");
		unitOutput = new File(getProject().getBuildDir(), "tmp/spotless-register-dependencies");
		rootProvisioner = new GradleProvisioner.RootProvisioner(getProject());
		gitRatchet = getProject().getGradle().getSharedServices().registerIfAbsent("GitRatchetGradle", GitRatchetGradle.class, unused -> {}).get();
	}

	@TaskAction
	public void trivialFunction() throws IOException {
		Files.createParentDirs(unitOutput);
		Files.write(Integer.toString(getSteps().size()), unitOutput, StandardCharsets.UTF_8);
	}

	GitRatchetGradle gitRatchet;

	@Internal
	GitRatchetGradle getGitRatchet() {
		return gitRatchet;
	}
}
