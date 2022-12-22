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
package com.diffplug.spotless.maven.javascript;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.npm.AbstractNpmFormatterStepFactory;
import com.diffplug.spotless.npm.EslintConfig;
import com.diffplug.spotless.npm.EslintFormatterStep;
import com.diffplug.spotless.npm.NpmPathResolver;

public class EslintJs extends AbstractNpmFormatterStepFactory {

	@Parameter
	private String configFile;

	@Parameter
	private String configJs;

	@Parameter
	private String styleGuide;

	@Parameter
	protected String eslintVersion;

	@Parameter
	private Map<String, String> devDependencies;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig stepConfig) {
		Map<String, String> devDependencies = new TreeMap<>();
		if (this.devDependencies != null) {
			devDependencies.putAll(this.devDependencies);
		} else {
			Map<String, String> defaultDependencies = createDefaultDependencies();
			devDependencies.putAll(defaultDependencies);
		}

		addStyleGuideDevDependencies(devDependencies);

		File buildDir = buildDir(stepConfig);
		File baseDir = baseDir(stepConfig);
		NpmPathResolver npmPathResolver = npmPathResolver(stepConfig);
		return EslintFormatterStep.create(devDependencies, stepConfig.getProvisioner(), baseDir, buildDir, npmPathResolver, eslintConfig(stepConfig));
	}

	protected EslintConfig eslintConfig(FormatterStepConfig stepConfig) {
		return new EslintConfig(this.configFile != null ? stepConfig.getFileLocator().locateFile(this.configFile) : null, this.configJs);
	}

	private void addStyleGuideDevDependencies(Map<String, String> devDependencies) {
		if (this.styleGuide != null) {
			EslintFormatterStep.PopularStyleGuide styleGuide = EslintFormatterStep.PopularStyleGuide.fromNameOrNull(this.styleGuide);
			validateStyleGuide(styleGuide);
			devDependencies.putAll(styleGuide.devDependencies());
		}
	}

	private void validateStyleGuide(EslintFormatterStep.PopularStyleGuide styleGuide) {
		if (styleGuide == null) {
			throw new IllegalArgumentException("StyleGuide '" + this.styleGuide + "' is not supported. Supported style guides: " + supportedStyleGuides());
		}
		if (!isValidStyleGuide(styleGuide)) {
			throw new IllegalArgumentException("StyleGuide must be of correct type but is: " + styleGuide.getPopularStyleGuideName() + ". Use one of the following: " + supportedStyleGuides());
		}
	}

	private String supportedStyleGuides() {
		return EslintFormatterStep.PopularStyleGuide.getPopularStyleGuideNames(this::isValidStyleGuide);
	}

	protected boolean isValidStyleGuide(EslintFormatterStep.PopularStyleGuide styleGuide) {
		return styleGuide.name().startsWith("JS_");
	}

	protected Map<String, String> createDefaultDependencies() {
		return eslintVersion == null ? EslintFormatterStep.defaultDevDependencies() : EslintFormatterStep.defaultDevDependenciesWithEslint(eslintVersion);
	}
}
