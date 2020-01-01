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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import com.diffplug.common.collect.Sets;
import com.diffplug.common.io.Files;
import com.diffplug.spotless.FormatterStep;

/**
 * NOT AN END-USER TASK, DO NOT USE FOR ANYTHING!
 * 
 * The minimal task required to force all SpotlessTasks in the root
 * project to trigger their dependency resolution, so that they will
 * be cached for subproject tasks to slurp from.  See {@link RegisterDependenciesInRoot}
 * for the bigger picture.
 */
public class RegisterDependenciesTask extends DefaultTask {
	@Input
	public List<FormatterStep> getSteps() {
		List<FormatterStep> allSteps = new ArrayList<>();
		Set<SpotlessTask> alreadyAdded = Sets.newIdentityHashSet();
		for (Object dependsOn : getDependsOn()) {
			// in Gradle 2.14, we can have a single task listed as a dep twice,
			// and we can also have non-tasks listed as a dep
			if (dependsOn instanceof SpotlessTask) {
				SpotlessTask task = (SpotlessTask) dependsOn;
				if (alreadyAdded.add(task)) {
					allSteps.addAll(task.getSteps());
				}
			}
		}
		return allSteps;
	}

	File unitOutput;

	@OutputFile
	public File getUnitOutput() {
		return unitOutput;
	}

	RegisterDependenciesInRoot.RootProvisioner rootProvisioner;

	@Internal
	public RegisterDependenciesInRoot.RootProvisioner getRootProvisioner() {
		return rootProvisioner;
	}

	void setup() {
		unitOutput = new File(getProject().getBuildDir(), "tmp/spotless-register-dependencies");
		rootProvisioner = new RegisterDependenciesInRoot.RootProvisioner(getProject());
	}

	@TaskAction
	public void trivialFunction() throws IOException {
		Files.createParentDirs(unitOutput);
		Files.write("unit", unitOutput, StandardCharsets.UTF_8);
	}
}
