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
package com.diffplug.gradle.spotless;

import java.io.File;

import javax.annotation.Nullable;

import org.gradle.api.Project;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;

import com.diffplug.spotless.extra.GitRatchet;

/** Gradle implementation of GitRatchet. */
public abstract class GitRatchetGradle extends GitRatchet<Project> implements BuildService<BuildServiceParameters.None>, OperationCompletionListener {
	@Override
	protected File getDir(Project project) {
		return project.getProjectDir();
	}

	@Override
	protected @Nullable Project getParent(Project project) {
		return project.getParent();
	}

	@Override
	public void onFinish(FinishEvent finishEvent) {
		// NOOP
	}
}
