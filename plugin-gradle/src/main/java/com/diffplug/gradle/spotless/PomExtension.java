/*
 * Copyright 2024 DiffPlug
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

import java.util.Objects;

import javax.inject.Inject;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.pom.SortPomCfg;
import com.diffplug.spotless.pom.SortPomStep;

public class PomExtension extends FormatExtension {
	private static final String POM_FILE = "pom.xml";

	static final String NAME = "pom";

	@Inject
	public PomExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			target = parseTarget(POM_FILE);
		}
		super.setupTask(task);
	}

	public SortPomGradleConfig sortPom() {
		return new SortPomGradleConfig();
	}

	public SortPomGradleConfig sortPom(String version) {
		Objects.requireNonNull(version);
		return new SortPomGradleConfig(version);
	}

	public class SortPomGradleConfig {
		private final SortPomCfg cfg = new SortPomCfg();

		SortPomGradleConfig() {
			addStep(createStep());
		}

		SortPomGradleConfig(String version) {
			this();
			cfg.version = Objects.requireNonNull(version);
		}

		public SortPomGradleConfig encoding(String encoding) {
			cfg.encoding = encoding;
			return this;
		}

		public SortPomGradleConfig lineSeparator(String lineSeparator) {
			cfg.lineSeparator = lineSeparator;
			return this;
		}

		public SortPomGradleConfig expandEmptyElements(boolean expandEmptyElements) {
			cfg.expandEmptyElements = expandEmptyElements;
			return this;
		}

		public SortPomGradleConfig spaceBeforeCloseEmptyElement(boolean spaceBeforeCloseEmptyElement) {
			cfg.spaceBeforeCloseEmptyElement = spaceBeforeCloseEmptyElement;
			return this;
		}

		public SortPomGradleConfig keepBlankLines(boolean keepBlankLines) {
			cfg.keepBlankLines = keepBlankLines;
			return this;
		}

		public SortPomGradleConfig endWithNewline(boolean endWithNewline) {
			cfg.endWithNewline = endWithNewline;
			return this;
		}

		public SortPomGradleConfig nrOfIndentSpace(int nrOfIndentSpace) {
			cfg.nrOfIndentSpace = nrOfIndentSpace;
			return this;
		}

		public SortPomGradleConfig indentBlankLines(boolean indentBlankLines) {
			cfg.indentBlankLines = indentBlankLines;
			return this;
		}

		public SortPomGradleConfig indentSchemaLocation(boolean indentSchemaLocation) {
			cfg.indentSchemaLocation = indentSchemaLocation;
			return this;
		}

		public SortPomGradleConfig indentAttribute(String indentAttribute) {
			cfg.indentAttribute = indentAttribute;
			return this;
		}

		public SortPomGradleConfig predefinedSortOrder(String predefinedSortOrder) {
			cfg.predefinedSortOrder = predefinedSortOrder;
			return this;
		}

		public SortPomGradleConfig sortOrderFile(String sortOrderFile) {
			cfg.sortOrderFile = sortOrderFile;
			return this;
		}

		public SortPomGradleConfig sortDependencies(String sortDependencies) {
			cfg.sortDependencies = sortDependencies;
			return this;
		}

		public SortPomGradleConfig sortDependencyManagement(String sortDependencyManagement) {
			cfg.sortDependencyManagement = sortDependencyManagement;
			return this;
		}

		public SortPomGradleConfig sortDependencyExclusions(String sortDependencyExclusions) {
			cfg.sortDependencyExclusions = sortDependencyExclusions;
			return this;
		}

		public SortPomGradleConfig sortPlugins(String sortPlugins) {
			cfg.sortPlugins = sortPlugins;
			return this;
		}

		public SortPomGradleConfig sortProperties(boolean sortProperties) {
			cfg.sortProperties = sortProperties;
			return this;
		}

		public SortPomGradleConfig sortModules(boolean sortModules) {
			cfg.sortModules = sortModules;
			return this;
		}

		public SortPomGradleConfig sortExecutions(boolean sortExecutions) {
			cfg.sortExecutions = sortExecutions;
			return this;
		}

		private FormatterStep createStep() {
			return SortPomStep.create(cfg, provisioner());
		}
	}
}
