/*
 * Copyright 2016-2023 DiffPlug
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
import java.util.Properties;
import java.util.TreeMap;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.npm.AbstractNpmFormatterStepFactory;
import com.diffplug.spotless.npm.EslintConfig;
import com.diffplug.spotless.npm.EslintFormatterStep;
import com.diffplug.spotless.npm.NpmPathResolver;

public abstract class AbstractEslint extends AbstractNpmFormatterStepFactory {

	public static final String ERROR_MESSAGE_ONLY_ONE_CONFIG = "must specify exactly one eslintVersion, devDependencies or devDependencyProperties";

	@Parameter
	protected String configFile;

	@Parameter
	protected String configJs;

	@Parameter
	protected String eslintVersion;

	@Parameter
	protected Map<String, String> devDependencies;

	@Parameter
	protected Properties devDependencyProperties;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig stepConfig) {
		// check if config is only setup in one way
		if (moreThanOneNonNull(this.eslintVersion, this.devDependencies, this.devDependencyProperties)) {
			throw onlyOneConfig();
		}

		Map<String, String> devDependencies = new TreeMap<>();
		if (this.devDependencies != null) {
			devDependencies.putAll(this.devDependencies);
		} else if (this.devDependencyProperties != null) {
			devDependencies.putAll(propertiesAsMap(this.devDependencyProperties));
		} else {
			Map<String, String> defaultDependencies = createDefaultDependencies();
			devDependencies.putAll(defaultDependencies);
		}

		File buildDir = buildDir(stepConfig);
		File baseDir = baseDir(stepConfig);
		File cacheDir = cacheDir(stepConfig);
		NpmPathResolver npmPathResolver = npmPathResolver(stepConfig);
		return EslintFormatterStep.create(devDependencies, stepConfig.getProvisioner(), baseDir, buildDir, cacheDir, npmPathResolver, eslintConfig(stepConfig));
	}

	private static IllegalArgumentException onlyOneConfig() {
		return new IllegalArgumentException(ERROR_MESSAGE_ONLY_ONE_CONFIG);
	}

	protected abstract EslintConfig eslintConfig(FormatterStepConfig stepConfig);

	protected abstract Map<String, String> createDefaultDependencies();
}
