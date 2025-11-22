/*
 * Copyright 2023-2025 DiffPlug
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
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;

import org.gradle.api.Project;

import com.diffplug.spotless.extra.EquoBasedStepBuilder;
import com.diffplug.spotless.extra.groovy.GrEclipseFormatterStep;
import com.diffplug.spotless.groovy.RemoveSemicolonsStep;
import com.diffplug.spotless.java.ImportOrderStep;

public abstract class BaseGroovyExtension extends FormatExtension {
	protected BaseGroovyExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	public void importOrder(String... importOrder) {
		addStep(ImportOrderStep.forGroovy().createFrom(importOrder));
	}

	public void importOrderFile(Object importOrderFile) {
		requireNonNull(importOrderFile);
		addStep(ImportOrderStep.forGroovy().createFrom(getProject().file(importOrderFile)));
	}

	public void removeSemicolons() {
		addStep(RemoveSemicolonsStep.create());
	}

	public GrEclipseConfig greclipse() {
		return greclipse(GrEclipseFormatterStep.defaultVersion());
	}

	public GrEclipseConfig greclipse(String version) {
		return new GrEclipseConfig(version, this);
	}

	public static final class GrEclipseConfig {
		private final EquoBasedStepBuilder builder;
		private final FormatExtension extension;

		private GrEclipseConfig(String version, FormatExtension extension) {
			this.extension = extension;
			builder = GrEclipseFormatterStep.createBuilder(extension.provisioner());
			builder.setVersion(version);
			extension.addStep(builder.build());
		}

		public GrEclipseConfig configFile(Object... configFiles) {
			requireElementsNonNull(configFiles);
			Project project = extension.getProject();
			builder.setPreferences(project.files(configFiles).getFiles());
			extension.replaceStep(builder.build());
			return this;
		}

		public GrEclipseConfig configProperties(String... configs) {
			requireElementsNonNull(configs);
			builder.setPropertyPreferences(List.of(configs));
			extension.replaceStep(builder.build());
			return this;
		}

		public GrEclipseConfig configXml(String... configs) {
			requireElementsNonNull(configs);
			builder.setXmlPreferences(List.of(configs));
			extension.replaceStep(builder.build());
			return this;
		}

		public GrEclipseConfig withP2Mirrors(Map<String, String> mirrors) {
			builder.setP2Mirrors(mirrors);
			extension.replaceStep(builder.build());
			return this;
		}
	}
}
