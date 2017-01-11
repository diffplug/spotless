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
import java.util.Objects;

import javax.annotation.Nullable;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.scala.ScalaFmtStep;

public class ScalaExtension extends FormatExtension {
	static final String NAME = "scala";

	public ScalaExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
	}

	public ScalaFmtConfig scalafmt() {
		return scalafmt(ScalaFmtStep.defaultVersion());
	}

	public ScalaFmtConfig scalafmt(String version) {
		return new ScalaFmtConfig(version);
	}

	public class ScalaFmtConfig {
		final String version;
		@Nullable
		Object configFile;

		ScalaFmtConfig(String version) {
			this.version = Objects.requireNonNull(version);
			addStep(createStep());
		}

		public void configFile(Object configFile) {
			this.configFile = configFile;
			replaceStep(createStep());
		}

		private FormatterStep createStep() {
			File resolvedConfigFile = configFile == null ? null : getProject().file(configFile);
			return ScalaFmtStep.create(version, GradleProvisioner.fromProject(getProject()), resolvedConfigFile);
		}
	}

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the kotlin files. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			JavaPluginConvention javaPlugin = getProject().getConvention().findPlugin(JavaPluginConvention.class);
			if (javaPlugin == null) {
				throw new GradleException("You must either specify 'target' manually or apply the 'scala' plugin.");
			}
			UnionFileCollection union = new UnionFileCollection();
			for (SourceSet sourceSet : javaPlugin.getSourceSets()) {
				union.add((FileCollection) sourceSet.getAllSource().include(fileTreeElement -> fileTreeElement.getName().endsWith(".scala")));
			}
			target = union;
		}
		super.setupTask(task);
	}
}
