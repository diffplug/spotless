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

import static com.diffplug.gradle.spotless.PluginGradlePreconditions.requireElementsNonNull;

import java.util.Arrays;

import org.gradle.api.Project;

import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.cpp.EclipseCdtFormatterStep;

public class CppExtension extends FormatExtension {
	static final String NAME = "cpp";

	public CppExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
	}

	public EclipseConfig eclipse() {
		return new EclipseConfig(EclipseCdtFormatterStep.defaultVersion());
	}

	public EclipseConfig eclipse(String version) {
		return new EclipseConfig(version);
	}

	public class EclipseConfig {
		private final EclipseBasedStepBuilder builder;

		EclipseConfig(String version) {
			builder = EclipseCdtFormatterStep.createBuilder(GradleProvisioner.fromProject(getProject()));
			builder.setVersion(version);
			addStep(builder.build());
		}

		public void configFile(Object... configFiles) {
			requireElementsNonNull(configFiles);
			Project project = getProject();
			builder.setPreferences(project.files(configFiles).getFiles());
			replaceStep(builder.build());
		}

	}

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the C/C++ files. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			/*
			 * The org.gradle.language.c and org.gradle.language.cpp source sets are seldom used.
			 * Most Gradle C/C++ use external CMake builds (so the source location is unknown to Gradle).
			 * Hence file extension based filtering is used in line with the org.eclipse.core.contenttype.contentTypes
			 * defined by the CDT plugin.
			 */
			target(
					Arrays.asList("c", "h", "C", "cpp", "cxx", "cc", "c\\+\\+", "h", "hpp", "hh", "hxx", "inc")
							.stream().map(s -> {
								return "**/*." + s;
							}).toArray());
		}
		super.setupTask(task);
	}
}
