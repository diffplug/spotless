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

import java.util.function.BiConsumer;

import org.gradle.api.Project;

interface SpotlessTaskSetup extends BiConsumer<Project, SpotlessExtension> {
	static SpotlessTaskSetup detectAppropriateImplementation() {
		try {
			Class.forName("org.gradle.api.tasks.TaskProvider");
			// Instantiate SpotlessTaskSetupConfigAvoidance by its fully qualified name, so that
			// classloaders don't attempt to load it when Spotless is running on a relatively
			// young version of Gradle where org.gradle.api.tasks.TaskProvider doesn't exist.
			return new com.diffplug.gradle.spotless.SpotlessTaskSetupConfigAvoidance();
		} catch (ClassNotFoundException e) {
			return new com.diffplug.gradle.spotless.SpotlessTaskSetupLegacy();
		}
	}
}
