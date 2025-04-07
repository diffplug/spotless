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
package com.diffplug.spotless.npm;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterFunc.Closeable;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.npm.EslintRestService.FormatOption;

public class EslintFormatterStep {

	private static final Logger logger = LoggerFactory.getLogger(EslintFormatterStep.class);

	public static final String NAME = "eslint-format";

	public static final String DEFAULT_ESLINT_VERSION = "^8.45.0";

	public static Map<String, String> defaultDevDependenciesForTypescript() {
		return defaultDevDependenciesTypescriptWithEslint(DEFAULT_ESLINT_VERSION);
	}

	public static Map<String, String> defaultDevDependenciesTypescriptWithEslint(String eslintVersion) {
		Map<String, String> dependencies = new LinkedHashMap<>();
		dependencies.put("@typescript-eslint/eslint-plugin", "^6.1.0");
		dependencies.put("@typescript-eslint/parser", "^6.1.0");
		dependencies.put("typescript", "^5.1.6");
		dependencies.put("eslint", Objects.requireNonNull(eslintVersion));
		return dependencies;
	}

	public static Map<String, String> defaultDevDependencies() {
		return defaultDevDependenciesWithEslint(DEFAULT_ESLINT_VERSION);
	}

	public static Map<String, String> defaultDevDependenciesWithEslint(String version) {
		return Collections.singletonMap("eslint", version);
	}

	public static FormatterStep create(String formatName, Map<String, String> devDependencies, Provisioner provisioner, File projectDir, File buildDir, File cacheDir, NpmPathResolver npmPathResolver, EslintConfig eslintConfig) {
		requireNonNull(formatName);
		requireNonNull(devDependencies);
		requireNonNull(provisioner);
		requireNonNull(projectDir);
		requireNonNull(buildDir);
		final String prefixedName = String.format("%s-%s", formatName, NAME);
		return FormatterStep.createLazy(prefixedName,
				() -> new State(prefixedName, devDependencies, projectDir, buildDir, cacheDir, npmPathResolver, eslintConfig),
				State::createFormatterFunc);
	}

	private static class State extends NpmFormatterStepStateBase implements Serializable {
		private static final long serialVersionUID = 1L;

		private final EslintConfig origEslintConfig;
		private EslintConfig eslintConfigInUse;

		State(String stepName, Map<String, String> devDependencies, File projectDir, File buildDir, File cacheDir, NpmPathResolver npmPathResolver, EslintConfig eslintConfig) throws IOException {
			super(stepName,
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
		protected void prepareNodeServerLayout(NodeServerLayout nodeServerLayout) throws IOException {
			super.prepareNodeServerLayout(nodeServerLayout);
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
		@Nonnull
		public FormatterFunc createFormatterFunc() {
			try {
				logger.info("Creating formatter function (starting server)");
				Runtime runtime = toRuntime();
				ServerProcessInfo eslintRestServer = runtime.npmRunServer();
				EslintRestService restService = new EslintRestService(eslintRestServer.getBaseUrl());
				return Closeable.ofDangerous(() -> endServer(restService, eslintRestServer), new EslintFilePathPassingFormatterFunc(locations.projectDir(), runtime.nodeServerLayout().nodeModulesDir(), eslintConfigInUse, restService));
			} catch (IOException e) {
				throw ThrowingEx.asRuntime(e);
			}
		}

		private void endServer(BaseNpmRestService restService, ServerProcessInfo restServer) throws Exception {
			logger.info("Closing formatting function (ending server).");
			try {
				restService.shutdown();
			} catch (Throwable t) {
				logger.info("Failed to request shutdown of rest service via api. Trying via process.", t);
			}
			restServer.close();
		}

	}

	private static class EslintFilePathPassingFormatterFunc implements FormatterFunc.NeedsFile {
		private final File projectDir;
		private final File nodeModulesDir;
		private final EslintConfig eslintConfig;
		private final EslintRestService restService;

		public EslintFilePathPassingFormatterFunc(File projectDir, File nodeModulesDir, EslintConfig eslintConfig, EslintRestService restService) {
			this.projectDir = requireNonNull(projectDir);
			this.nodeModulesDir = requireNonNull(nodeModulesDir);
			this.eslintConfig = requireNonNull(eslintConfig);
			this.restService = requireNonNull(restService);
		}

		@Override
		public String applyWithFile(String unix, File file) throws Exception {
			Map<FormatOption, Object> eslintCallOptions = new HashMap<>();
			setConfigToCallOptions(eslintCallOptions);
			setFilePathToCallOptions(eslintCallOptions, file);
			return restService.format(unix, eslintCallOptions);
		}

		private void setFilePathToCallOptions(Map<FormatOption, Object> eslintCallOptions, File fileToBeFormatted) {
			eslintCallOptions.put(FormatOption.FILE_PATH, fileToBeFormatted.getAbsolutePath());
		}

		private void setConfigToCallOptions(Map<FormatOption, Object> eslintCallOptions) {
			if (eslintConfig.getEslintConfigPath() != null) {
				eslintCallOptions.put(FormatOption.ESLINT_OVERRIDE_CONFIG_FILE, eslintConfig.getEslintConfigPath().getAbsolutePath());
			}
			if (eslintConfig.getEslintConfigJs() != null) {
				eslintCallOptions.put(FormatOption.ESLINT_OVERRIDE_CONFIG, eslintConfig.getEslintConfigJs());
			}
			if (eslintConfig instanceof EslintTypescriptConfig) {
				// if we are a ts config, see if we need to use specific paths or use default projectDir
				File tsConfigFilePath = ((EslintTypescriptConfig) eslintConfig).getTypescriptConfigPath();
				File tsConfigRootDir = tsConfigFilePath != null ? tsConfigFilePath.getParentFile() : projectDir;
				eslintCallOptions.put(FormatOption.TS_CONFIG_ROOT_DIR, nodeModulesDir.getAbsoluteFile().toPath().relativize(tsConfigRootDir.getAbsoluteFile().toPath()).toString());
			}
		}
	}
}
