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

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.tasks.ScalaRuntime;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.scala.ScalaCompile;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.scala.ScalaCompiler;
import com.diffplug.spotless.scala.ScalaFixStep;
import com.diffplug.spotless.scala.ScalaFmtStep;

public class ScalaExtension extends FormatExtension {
	static final String NAME = "scala";
	private static final String SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME = "spotlessScalaCompilerPlugins";

	private final ScalaCompiler scalaCompiler;

	public ScalaExtension(SpotlessExtensionBase rootExtension) {
		super(rootExtension);

		final PluginManager pluginManager = getProject().getPluginManager();
		if (!pluginManager.hasPlugin("scala")) {
			pluginManager.apply("scala");
		}

		List<File> destinationDirs = new ArrayList<>();
		getProject().getTasks().withType(ScalaCompile.class).forEach(scalaCompile -> destinationDirs.add(scalaCompile.getDestinationDir()));

		ScalaRuntime scalaRuntime = getProject().getExtensions().findByType(ScalaRuntime.class);
		ScalaCompile scalaCompileTask = getProject().getTasks().withType(ScalaCompile.class).findByName("compileScala");
		File scalaJar = scalaRuntime.findScalaJar(scalaCompileTask.getClasspath(), "library");
		if (scalaJar == null) {
			throw new GradleException("You must add a dependency of scala compiler.");
		}
		scalaCompiler = new ScalaCompiler(scalaRuntime.getScalaVersion(scalaJar), getProject().files(destinationDirs).getAsPath());
	}

	public ScalaFmtConfig scalafmt() {
		return scalafmt(ScalaFmtStep.defaultVersion());
	}

	public ScalaFmtConfig scalafmt(String version) {
		return new ScalaFmtConfig(version);
	}

	public ScalaFixConfig scalafix() {
		return scalafix(ScalaFixStep.defaultVersion());
	}

	public ScalaFixConfig scalafix(String version) {
		return scalafix(version, ScalaFixStep.defaultScalaVersion());
	}

	public ScalaFixConfig scalafix(String version, String scalaVersion) {
		return new ScalaFixConfig(version, scalaVersion);
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
			this.configFile = Objects.requireNonNull(configFile);
			replaceStep(createStep());
		}

		private FormatterStep createStep() {
			File resolvedConfigFile = configFile == null ? null : getProject().file(configFile);
			return ScalaFmtStep.create(version, provisioner(), resolvedConfigFile);
		}
	}

	public class ScalaFixConfig {
		final String version;
		final String scalaVersion;
		@Nullable
		Object configFile;

		ScalaFixConfig(String version, String scalaVersion) {
			this.version = Objects.requireNonNull(version);
			this.scalaVersion = Objects.requireNonNull(scalaVersion);
			ScalaFixStep.init(scalaCompiler, getProject().getProjectDir());
			addStep(createStep());
		}

		public void configFile(Object configFile) {
			this.configFile = Objects.requireNonNull(configFile);
			replaceStep(createStep());
		}

		private FormatterStep createStep() {
			File resolvedConfigFile = configFile == null ? null : getProject().file(configFile);
			return ScalaFixStep.create(version, scalaVersion, GradleProvisioner.fromProject(getProject()), scalaCompiler, resolvedConfigFile);
		}
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			JavaPluginConvention javaPlugin = getProject().getConvention().findPlugin(JavaPluginConvention.class);
			if (javaPlugin == null) {
				throw new GradleException("You must either specify 'target' manually or apply the 'scala' plugin.");
			}
			FileCollection union = getProject().files();
			for (SourceSet sourceSet : javaPlugin.getSourceSets()) {
				union = union.plus(sourceSet.getAllSource().filter(file -> {
					String name = file.getName();
					return name.endsWith(".scala") || name.endsWith(".sc");
				}));
			}
			target = union;
		}

		if (scalaCompiler.isEnabled()) {
			// scalaCompilerPlugins is not available on Gradle < 6.4
			final Configuration scalacPlugins = getProject().getConfigurations().create(SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME);
			scalacPlugins.setTransitive(false);
			scalacPlugins.setCanBeConsumed(false);
			scalaCompiler.getPlugins().forEach(plugin -> getProject().getDependencies().add(SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME, plugin));
			scalaCompiler.addCompilerOptions(scalacPlugins.getFiles().stream().map(file -> "-Xplugin:" + file).collect(Collectors.toList()));
			getProject().getTasks().withType(ScalaCompile.class).forEach(scalaCompile -> scalaCompile.getScalaCompileOptions().setAdditionalParameters(new ArrayList<String>(scalaCompiler.getCompilerOptions())));

			task.dependsOn("compileTestScala");
		}

		super.setupTask(task);
	}
}
