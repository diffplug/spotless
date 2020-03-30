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
package com.diffplug.spotless.maven.typescript;

import static com.diffplug.common.base.Strings.isNullOrEmpty;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;
import com.diffplug.spotless.npm.TsConfigFileType;
import com.diffplug.spotless.npm.TsFmtFormatterStep;
import com.diffplug.spotless.npm.TypedTsFmtConfigFile;

public class Tsfmt implements FormatterStepFactory {

	@Parameter
	private String tslintFile;

	@Parameter
	private String tsconfigFile;

	@Parameter
	private String vscodeFile;

	@Parameter
	private String tsfmtFile;

	@Parameter
	private String typescriptFormatterVersion;

	@Parameter
	private String typescriptVersion;

	@Parameter
	private String tslintVersion;

	@Parameter
	private String npmExecutable;

	@Parameter
	private Map<String, String> config;

	private File locateFile(String path) {
		if (isNullOrEmpty(path)) {
			return null;
		}
		
		File exists = new File(path);
		if (exists.exists()) {
			return exists;
		}
		
		throw new RuntimeException("Unable to locate file with path: " + path);
	}
	
	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig stepConfig) {

		Map<String, String> devDependencies = TsFmtFormatterStep.defaultDevDependencies();
		if (typescriptFormatterVersion != null) {
			devDependencies.put("typescript-formatter", typescriptFormatterVersion);
		}
		if (typescriptVersion != null) {
			devDependencies.put("typescript", typescriptVersion);
		}
		if (tslintVersion != null) {
			devDependencies.put("tslint", tslintVersion);
		}

		File npm = npmExecutable != null ? stepConfig.getFileLocator().locateFile(npmExecutable) : null;

		TypedTsFmtConfigFile configFile;
		Map<String, Object> configInline;
		// check that there is only 1 config file or inline config
		if (this.tsconfigFile != null
				^ this.tsfmtFile != null
				^ this.tslintFile != null
				^ this.vscodeFile != null) {
			if (this.config != null) {
				throw onlyOneConfig();
			}
			configInline = null;
			if (this.tsconfigFile != null) {
				configFile = new TypedTsFmtConfigFile(TsConfigFileType.TSCONFIG, locateFile(tsconfigFile));
			} else if (this.tsfmtFile != null) {
				configFile = new TypedTsFmtConfigFile(TsConfigFileType.TSFMT, locateFile(tsfmtFile));
			} else if (this.tslintFile != null) {
				configFile = new TypedTsFmtConfigFile(TsConfigFileType.TSLINT, locateFile(tslintFile));
			} else if (this.vscodeFile != null) {
				configFile = new TypedTsFmtConfigFile(TsConfigFileType.VSCODE, locateFile(vscodeFile));
			} else {
				throw new Error("Programming error: the xors did not match the cases");
			}
		} else if (config != null) {
			configFile = null;
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
			throw onlyOneConfig();
		}

		File buildDir = stepConfig.getFileLocator().getBuildDir();
		return TsFmtFormatterStep.create(devDependencies, stepConfig.getProvisioner(), buildDir, npm, configFile, configInline);
	}

	private static IllegalArgumentException onlyOneConfig() {
		return new IllegalArgumentException("must specify exactly one configFile or config");
	}
}
