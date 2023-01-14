/*
 * Copyright 2022-2023 DiffPlug
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
package com.diffplug.spotless.maven.typescript;

import java.util.Map;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.javascript.AbstractEslint;
import com.diffplug.spotless.npm.EslintConfig;
import com.diffplug.spotless.npm.EslintFormatterStep;
import com.diffplug.spotless.npm.EslintTypescriptConfig;

public class EslintTs extends AbstractEslint {

	@Parameter
	private String tsconfigFile;

	@Override
	protected EslintConfig eslintConfig(FormatterStepConfig stepConfig) {
		return new EslintTypescriptConfig(
				configFile != null ? stepConfig.getFileLocator().locateFile(configFile) : null,
				configJs,
				tsconfigFile != null ? stepConfig.getFileLocator().locateFile(tsconfigFile) : null);
	}

	@Override
	protected Map<String, String> createDefaultDependencies() {
		return this.eslintVersion == null ? EslintFormatterStep.defaultDevDependenciesForTypescript() : EslintFormatterStep.defaultDevDependenciesTypescriptWithEslint(this.eslintVersion);
	}
}
