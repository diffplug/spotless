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

import org.gradle.api.Project;

public class SpotlessExtensionModern extends SpotlessExtensionBase {
	public SpotlessExtensionModern(Project project) {
		super(project);
	}

	@Override
	protected void createFormatTasks(String name, FormatExtension formatExtension) {
		// TODO level 1: implement SpotlessExtension::createFormatTasks, but using config avoidance
		// TODO level 2: override configure(String name, Class<T> clazz, Action<T> configure) so that it is lazy
	}
}
