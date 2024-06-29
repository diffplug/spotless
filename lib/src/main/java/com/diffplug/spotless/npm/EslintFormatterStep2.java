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
package com.diffplug.spotless.npm;

import static com.diffplug.spotless.npm.PackageJsonUtil.replaceDevDependencies;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.npm.EslintRestService.FormatOption;

public class EslintFormatterStep2 extends NpmServerBasedFormatterStep {

	private static final Logger logger = LoggerFactory.getLogger(EslintFormatterStep2.class);

	public static final String NAME = "eslint-format";

	public static final String DEFAULT_ESLINT_VERSION = "^8.45.0";
	private final EslintConfig origEslintConfig;
	private EslintConfig eslintConfigInUse;

	public static Map<String, String> defaultDevDependenciesForTypescript() {
		return defaultDevDependenciesTypescriptWithEslint(DEFAULT_ESLINT_VERSION);
	}

	public static Map<String, String> defaultDevDependenciesTypescriptWithEslint(String eslintVersion) {
		Map<String, String> dependencies = new LinkedHashMap<>();
		dependencies.put("@typescript-eslint/eslint-plugin", "^6.1.0");
		dependencies.put("@typescript-eslint/parser", "^6.1.0");
		dependencies.put("typescript", "^5.1.6");
		dependencies.put("eslint", requireNonNull(eslintVersion));
		return dependencies;
	}

	public static Map<String, String> defaultDevDependencies() {
		return defaultDevDependenciesWithEslint(DEFAULT_ESLINT_VERSION);
	}

	public static Map<String, String> defaultDevDependenciesWithEslint(String version) {
		return Collections.singletonMap("eslint", version);
	}

	public EslintFormatterStep2(@Nonnull Map<String, String> devDependencies, @Nonnull File projectDir, @Nonnull File buildDir, @Nonnull File cacheDir, @Nonnull NpmPathResolver npmPathResolver, @Nonnull EslintConfig eslintConfig) {
		super(NAME,
				new NpmConfig(
						replaceDevDependencies(
								NpmResourceHelper.readUtf8StringFromClasspath(EslintFormatterStep.class, "/com/diffplug/spotless/npm/eslint-package.json"),
								new TreeMap<>(devDependencies)),
						NpmResourceHelper.readUtf8StringFromClasspath(EslintFormatterStep.class,
								"/com/diffplug/spotless/npm/common-serve.js",
								"/com/diffplug/spotless/npm/eslint-serve.js"),
						npmPathResolver.resolveNpmrcContent()),
				new NpmFormatterStepLocations(
						projectDir,
						buildDir,
						cacheDir,
						npmPathResolver));
		this.origEslintConfig = requireNonNull(eslintConfig.verify());
		this.eslintConfigInUse = eslintConfig;
	}

	@Override
	protected void doPrepareNodeServerLayout(NodeServerLayout nodeServerLayout) throws IOException {
		if (origEslintConfig.getEslintConfigPath() != null) {
			// If any config files are provided, we need to make sure they are at the same location as the node modules
			// as eslint will try to resolve plugin/config names relatively to the config file location and some
			// eslint configs contain relative paths to additional config files (such as tsconfig.json e.g.)
			logger.debug("Copying config file <{}> to <{}> and using the copy", origEslintConfig.getEslintConfigPath(), nodeServerLayout.nodeModulesDir());
			File configFileCopy = NpmResourceHelper.copyFileToDir(origEslintConfig.getEslintConfigPath(), nodeServerLayout.nodeModulesDir());
			this.eslintConfigInUse = this.origEslintConfig.withEslintConfigPath(configFileCopy).verify();
		}
	}

	@Override
	protected String formatWithServer(NpmServerProcessInfo serverProcessInfo, String rawUnix, File file) {
		EslintRestService restService = new EslintRestService(serverProcessInfo.getBaseUrl());
		Map<FormatOption, Object> eslintCallOptions = new HashMap<>();
		setConfigToCallOptions(eslintCallOptions);
		setFilePathToCallOptions(eslintCallOptions, file);
		return restService.format(rawUnix, eslintCallOptions);
	}

	private void setFilePathToCallOptions(Map<FormatOption, Object> eslintCallOptions, File fileToBeFormatted) {
		eslintCallOptions.put(FormatOption.FILE_PATH, fileToBeFormatted.getAbsolutePath());
	}

	private void setConfigToCallOptions(Map<FormatOption, Object> eslintCallOptions) {
		if (this.eslintConfigInUse.getEslintConfigPath() != null) {
			eslintCallOptions.put(FormatOption.ESLINT_OVERRIDE_CONFIG_FILE, this.eslintConfigInUse.getEslintConfigPath().getAbsolutePath());
		}
		if (this.eslintConfigInUse.getEslintConfigJs() != null) {
			eslintCallOptions.put(FormatOption.ESLINT_OVERRIDE_CONFIG, this.eslintConfigInUse.getEslintConfigJs());
		}
		if (this.eslintConfigInUse instanceof EslintTypescriptConfig) {
			// if we are a ts config, see if we need to use specific paths or use default projectDir
			File tsConfigFilePath = ((EslintTypescriptConfig) this.eslintConfigInUse).getTypescriptConfigPath();
			File tsConfigRootDir = tsConfigFilePath != null ? tsConfigFilePath.getParentFile() : this.locations.projectDir();
			eslintCallOptions.put(FormatOption.TS_CONFIG_ROOT_DIR, this.nodeServerLayout.nodeModulesDir().getAbsoluteFile().toPath().relativize(tsConfigRootDir.getAbsoluteFile().toPath()).toString());
		}
	}
}
