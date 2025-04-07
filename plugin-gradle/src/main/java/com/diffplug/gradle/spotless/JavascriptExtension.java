/*
 * Copyright 2016-2025 DiffPlug
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

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.gradle.api.Project;

import com.diffplug.common.collect.ImmutableList;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.biome.BiomeFlavor;
import com.diffplug.spotless.npm.EslintConfig;
import com.diffplug.spotless.npm.EslintFormatterStep;
import com.diffplug.spotless.npm.NpmPathResolver;
import com.diffplug.spotless.npm.PrettierFormatterStep;

public class JavascriptExtension extends FormatExtension {

	static final String NAME = "javascript";

	@Inject
	public JavascriptExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	public JavascriptEslintConfig eslint() {
		return eslint(EslintFormatterStep.defaultDevDependenciesForTypescript());
	}

	public JavascriptEslintConfig eslint(String version) {
		return eslint(EslintFormatterStep.defaultDevDependenciesTypescriptWithEslint(version));
	}

	public JavascriptEslintConfig eslint(Map<String, String> devDependencies) {
		JavascriptEslintConfig eslint = new JavascriptEslintConfig(devDependencies);
		addStep(eslint.createStep());
		return eslint;
	}

	public static abstract class EslintBaseConfig<T extends EslintBaseConfig<?>>
			extends NpmStepConfig<EslintBaseConfig<T>> {
		Map<String, String> devDependencies = new LinkedHashMap<>();

		@Nullable
		Object configFilePath = null;

		@Nullable
		String configJs = null;

		public EslintBaseConfig(Project project, Consumer<FormatterStep> replaceStep,
				Map<String, String> devDependencies) {
			super(project, replaceStep);
			this.devDependencies.putAll(requireNonNull(devDependencies));
		}

		@SuppressWarnings("unchecked")
		protected T devDependencies(Map<String, String> devDependencies) {
			this.devDependencies.putAll(devDependencies);
			replaceStep();
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T configJs(String configJs) {
			this.configJs = requireNonNull(configJs);
			replaceStep();
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T configFile(Object configFilePath) {
			this.configFilePath = requireNonNull(configFilePath);
			replaceStep();
			return (T) this;
		}
	}

	public class JavascriptEslintConfig extends EslintBaseConfig<JavascriptEslintConfig> {

		public JavascriptEslintConfig(Map<String, String> devDependencies) {
			super(getProject(), JavascriptExtension.this::replaceStep, devDependencies);
		}

		public FormatterStep createStep() {
			final Project project = getProject();

			return EslintFormatterStep.create(NAME, devDependencies, provisioner(), project.getProjectDir(),
					project.getLayout().getBuildDirectory().getAsFile().get(), npmModulesCacheOrNull(),
					new NpmPathResolver(npmFileOrNull(), nodeFileOrNull(), npmrcFileOrNull(),
							Arrays.asList(project.getProjectDir(), project.getRootDir())),
					eslintConfig());
		}

		protected EslintConfig eslintConfig() {
			return new EslintConfig(configFilePath != null ? getProject().file(configFilePath) : null, configJs);
		}
	}

	/** Uses the default version of prettier. */
	@Override
	public PrettierConfig prettier() {
		return prettier(PrettierFormatterStep.defaultDevDependencies());
	}

	/** Uses the specified version of prettier. */
	@Override
	public PrettierConfig prettier(String version) {
		return prettier(PrettierFormatterStep.defaultDevDependenciesWithPrettier(version));
	}

	/** Uses exactly the npm packages specified in the map. */
	@Override
	public PrettierConfig prettier(Map<String, String> devDependencies) {
		PrettierConfig prettierConfig = new JavascriptPrettierConfig(devDependencies);
		addStep(prettierConfig.createStep());
		return prettierConfig;
	}

	/**
	 * Defaults to downloading the default Biome version from the network. To work
	 * offline, you can specify the path to the Biome executable via
	 * {@code biome().pathToExe(...)}.
	 */
	public BiomeJs biome() {
		return biome(null);
	}

	/** Downloads the given Biome version from the network. */
	public BiomeJs biome(String version) {
		var biomeConfig = new BiomeJs(version);
		addStep(biomeConfig.createStep());
		return biomeConfig;
	}

	private static final String DEFAULT_PRETTIER_JS_PARSER = "babel";
	private static final ImmutableList<String> PRETTIER_JS_PARSERS = ImmutableList.of(DEFAULT_PRETTIER_JS_PARSER,
			"babel-flow", "flow");

	/**
	 * Biome formatter step for JavaScript.
	 */
	public class BiomeJs extends BiomeStepConfig<BiomeJs> {
		/**
		 * Creates a new Biome formatter step config for formatting JavaScript files.
		 * Unless overwritten, the given Biome version is downloaded from the network.
		 *
		 * @param version Biome version to use.
		 */
		public BiomeJs(String version) {
			super(getProject(), JavascriptExtension.this::replaceStep, BiomeFlavor.BIOME, version);
		}

		@Override
		protected String getLanguage() {
			return "js?";
		}

		@Override
		protected BiomeJs getThis() {
			return this;
		}
	}

	/**
	 * Overrides the parser to be set to a js parser.
	 */
	public class JavascriptPrettierConfig extends PrettierConfig {

		JavascriptPrettierConfig(Map<String, String> devDependencies) {
			super(devDependencies);
		}

		@Override
		protected FormatterStep createStep() {
			fixParserToJavascript();
			return super.createStep();
		}

		private void fixParserToJavascript() {
			if (this.prettierConfig == null) {
				this.prettierConfig = Collections.singletonMap("parser", DEFAULT_PRETTIER_JS_PARSER);
			} else {
				final Object currentParser = this.prettierConfig.get("parser");
				if (PRETTIER_JS_PARSERS.contains(String.valueOf(currentParser))) {
					getProject().getLogger().debug("Already javascript parser set, not overriding.");
				} else {
					this.prettierConfig.put("parser", DEFAULT_PRETTIER_JS_PARSER);
					if (currentParser != null) {
						getProject().getLogger().warn(
								"Overriding parser option to '{}'. (Was set to '{}'.) Set it to another js parser if you have problems with '{}'.",
								DEFAULT_PRETTIER_JS_PARSER, currentParser, DEFAULT_PRETTIER_JS_PARSER);
					}
				}

			}
		}
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			throw noDefaultTargetException();
		}
		super.setupTask(task);
	}
}
