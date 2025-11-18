/*
 * Copyright 2025 DiffPlug
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

import javax.annotation.Nullable;

import org.gradle.api.Project;

public final class GradleCompat {
	private GradleCompat() {}

	@Nullable public static String findOptionalProperty(Project project, String propertyName) {
		@Nullable String value = project.getProviders().gradleProperty(propertyName).getOrNull();
		if (value != null) {
			return value;
		}
		@Nullable Object property = project.findProperty(propertyName);
		if (property != null) {
			return property.toString();
		}
		return null;
	}

	public static boolean isPropertyPresent(Project project, String propertyName) {
		return project.getProviders().gradleProperty(propertyName).isPresent() ||
				project.hasProperty(propertyName);
	}
}
