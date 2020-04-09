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
package com.diffplug.spotless.maven.generic;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;
import com.diffplug.spotless.npm.PrettierConfig;
import com.diffplug.spotless.npm.PrettierFormatterStep;

public class Prettier implements FormatterStepFactory {

	@Parameter
	private String prettierVersion;

	@Parameter
	private Map<String, String> devDependencies;

	@Parameter
	private Map<String, String> config;

	@Parameter
	private String configFile;

	@Parameter
	private String npmExecutable;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig stepConfig) {

		// check if config is only setup in one way
		if (this.prettierVersion != null && this.devDependencies != null) {
			throw onlyOneConfig();
		}

		// set dev dependencies
		if (devDependencies == null) {
			if (prettierVersion == null || prettierVersion.isEmpty()) {
				devDependencies = PrettierFormatterStep.defaultDevDependencies();
			} else {
				devDependencies = PrettierFormatterStep.defaultDevDependenciesWithPrettier(prettierVersion);
			}
		}

		File npm = npmExecutable != null ? stepConfig.getFileLocator().locateLocal(npmExecutable) : null;

		// process config file or inline config
		File configFileHandler;
		if (this.configFile != null) {
			configFileHandler = stepConfig.getFileLocator().locateLocal(this.configFile);
		} else {
			configFileHandler = null;
		}

		Map<String, Object> configInline;
		if (config != null) {
			configInline = new LinkedHashMap<>();
			// try to parse string values as integers or booleans
			for (Map.Entry<String, String> e : config.entrySet()) {
				try {
					configInline.put(e.getKey(), Integer.parseInt(e.getValue()));
				} catch (NumberFormatException ignore) {
					try {
						configInline.put(e.getKey(), Boolean.parseBoolean(e.getValue()));
					} catch (IllegalArgumentException ignore2) {
						configInline.put(e.getKey(), e.getValue());
					}
				}
			}
		} else {
			configInline = null;
		}

		// create the format step
		PrettierConfig prettierConfig = new PrettierConfig(configFileHandler, configInline);
		File buildDir = stepConfig.getFileLocator().getBuildDir();
		return PrettierFormatterStep.create(devDependencies, stepConfig.getProvisioner(), buildDir, npm, prettierConfig);
	}

	private static IllegalArgumentException onlyOneConfig() {
		return new IllegalArgumentException("must specify exactly one configFile or config");
	}
}
