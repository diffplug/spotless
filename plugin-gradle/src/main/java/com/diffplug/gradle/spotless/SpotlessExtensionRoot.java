/*
 * Copyright 2021 DiffPlug
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

import java.util.SortedMap;
import java.util.TreeMap;

import org.gradle.api.Action;
import org.gradle.api.Project;

public class SpotlessExtensionRoot extends SpotlessExtension {
	private final SortedMap<String, FormatExtension> toSetup = new TreeMap<>();

	public SpotlessExtensionRoot(Project project, GradleProvisioner.Policy policy) {
		super(project);
		getRegisterDependenciesTask().getTaskService().get().rootProvisioner = policy.dedupingProvisioner(project);
		project.afterEvaluate(unused -> {
			toSetup.forEach((name, formatExtension) -> {
				for (Action<FormatExtension> lazyAction : formatExtension.lazyActions) {
					lazyAction.execute(formatExtension);
				}
				getRegisterDependenciesTask().steps.addAll(formatExtension.steps);
			});
		});
	}

	@Override
	protected void createFormatTasks(String name, FormatExtension formatExtension) {
		toSetup.put(name, formatExtension);
	}

	@Override
	protected void predeclare(GradleProvisioner.Policy policy) {
		throw new UnsupportedOperationException("predeclare can't be called from within `" + EXTENSION_PREDECLARE + "`");
	}
}
