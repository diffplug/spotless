/*
 * Copyright 2016-2022 DiffPlug
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.gradle.api.Project;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.npm.EslintConfig;
import com.diffplug.spotless.npm.EslintFormatterStep;
import com.diffplug.spotless.npm.EslintFormatterStep.PopularStyleGuide;
import com.diffplug.spotless.npm.NpmPathResolver;

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

	// TODO: make the configs static so that they do not need to have a hierarchy symmetric to the extensions

	public static abstract class EslintBaseConfig<T extends EslintBaseConfig<?>> extends NpmStepConfig<EslintBaseConfig<T>> {
		Map<String, String> devDependencies = new LinkedHashMap<>();

		@Nullable
		Object configFilePath = null;

		@Nullable
		String configJs = null;

		public EslintBaseConfig(Project project, Consumer<FormatterStep> replaceStep, Map<String, String> devDependencies) {
			super(project, replaceStep);
			this.devDependencies.putAll(requireNonNull(devDependencies));
		}

		@SuppressWarnings("unchecked")
		public T devDependencies(Map<String, String> devDependencies) {
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

		@SuppressWarnings("unchecked")
		public T styleGuide(String styleGuide) {
			PopularStyleGuide popularStyleGuide = PopularStyleGuide.fromNameOrNull(styleGuide);

			verifyStyleGuideIsSupported(styleGuide, popularStyleGuide);
			devDependencies(popularStyleGuide.devDependencies());
			replaceStep();
			return (T) this;
		}

		protected abstract void verifyStyleGuideIsSupported(String styleGuideName, PopularStyleGuide popularStyleGuide);
	}

	public class JavascriptEslintConfig extends EslintBaseConfig<JavascriptEslintConfig> {

		public JavascriptEslintConfig(Map<String, String> devDependencies) {
			super(getProject(), JavascriptExtension.this::replaceStep, devDependencies);
		}

		public FormatterStep createStep() {
			final Project project = getProject();

			return EslintFormatterStep.create(
					devDependencies,
					provisioner(),
					project.getProjectDir(),
					project.getBuildDir(),
					new NpmPathResolver(npmFileOrNull(), npmrcFileOrNull(), project.getProjectDir(), project.getRootDir()),
					eslintConfig());
		}

		@Override
		protected void verifyStyleGuideIsSupported(String styleGuideName, PopularStyleGuide popularStyleGuide) {
			if (!isJsStyleGuide(popularStyleGuide)) {
				throw new IllegalArgumentException("Unknown style guide: " + styleGuideName + ". Known javascript style guides: "
						+ Arrays.stream(PopularStyleGuide.values())
								.filter(this::isJsStyleGuide)
								.map(PopularStyleGuide::getPopularStyleGuideName)
								.sorted()
								.collect(Collectors.joining(", ")));
			}
		}

		private boolean isJsStyleGuide(PopularStyleGuide popularStyleGuide) {
			return popularStyleGuide != null && popularStyleGuide.name().startsWith("JS_");
		}

		protected EslintConfig eslintConfig() {
			return new EslintConfig(configFilePath != null ? getProject().file(configFilePath) : null, configJs);
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
