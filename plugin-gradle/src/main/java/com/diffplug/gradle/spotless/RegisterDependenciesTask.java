/*
 * Copyright 2016-2023 DiffPlug
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

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceRegistry;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.build.event.BuildEventsListenerRegistry;
import org.gradle.work.DisableCachingByDefault;

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
@DisableCachingByDefault(because = "I/O bound task not worth caching")
public abstract class RegisterDependenciesTask extends DefaultTask {
	static final String TASK_NAME = "spotlessInternalRegisterDependencies";

	void hookSubprojectTask(SpotlessTask task) {
		// this ensures that if a user is using predeclared dependencies,
		// those predeclared deps will be resolved before they are needed
		// by the child tasks
		//
		// it's also needed to make sure that jvmLocalCache gets set
		// in the SpotlessTaskService before any spotless tasks run
		task.dependsOn(this);
	}

	void setup() {
		Preconditions.checkArgument(getProject().getRootProject() == getProject(), "Can only be used on the root project");
		String compositeBuildSuffix = getName().substring(TASK_NAME.length()); // see https://github.com/diffplug/spotless/pull/1001
		BuildServiceRegistry buildServices = getProject().getGradle().getSharedServices();
		taskService = buildServices.registerIfAbsent("SpotlessTaskService" + compositeBuildSuffix, SpotlessTaskService.class, spec -> {});
		usesService(taskService);
		getBuildEventsListenerRegistry().onTaskCompletion(taskService);
		unitOutput = new File(getProject().getBuildDir(), "tmp/spotless-register-dependencies");
	}

	List<FormatterStep> steps = new ArrayList<>();

	@Input
	public List<FormatterStep> getSteps() {
		return steps;
	}

	File unitOutput;

	@OutputFile
	public File getUnitOutput() {
		return unitOutput;
	}

	@TaskAction
	public void trivialFunction() throws IOException {
		Files.createParentDirs(unitOutput);
		Files.write(Integer.toString(1), unitOutput, StandardCharsets.UTF_8);
	}

	// this field is stupid, but we need it, see https://github.com/diffplug/spotless/issues/1260
	private Provider<SpotlessTaskService> taskService;

	@Internal
	public Provider<SpotlessTaskService> getTaskService() {
		return taskService;
	}

	@Inject
	protected abstract BuildEventsListenerRegistry getBuildEventsListenerRegistry();
}
