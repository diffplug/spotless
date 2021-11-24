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

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildServiceRegistry;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.build.event.BuildEventsListenerRegistry;

import com.diffplug.common.base.Preconditions;

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
public abstract class RegisterDependenciesTask extends DefaultTask {
	static final String TASK_NAME = "spotlessInternalRegisterDependencies";

	void hookSubprojectTask(SpotlessTask task) {
		// TODO: in the future, we might use this hook to implement #984
		// spotlessSetup {
		//    java { googleJavaFormat('1.2') }
		//    ...etc
		// }
		// it's also needed to make sure that jvmLocalCache gets set
		// in the SpotlessTaskService before any spotless tasks run
		task.dependsOn(this);
	}

	void setup() {
		Preconditions.checkArgument(getProject().getRootProject() == getProject(), "Can only be used on the root project");

		BuildServiceRegistry buildServices = getProject().getGradle().getSharedServices();
		getTaskService().set(buildServices.registerIfAbsent("SpotlessTaskService" +
				(getProject().getSubprojects().size() > 1 ? System.identityHashCode(RegisterDependenciesTask.class.getClassLoader()) : ""), SpotlessTaskService.class, spec -> {}));
		getBuildEventsListenerRegistry().onTaskCompletion(getTaskService());
	}

	@TaskAction
	public void trivialFunction() {
		// nothing to do :)
	}

	@Internal
	abstract Property<SpotlessTaskService> getTaskService();

	@Inject
	protected abstract BuildEventsListenerRegistry getBuildEventsListenerRegistry();
}
