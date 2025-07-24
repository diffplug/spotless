/*
 * Copyright 2016-2024 DiffPlug
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
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterFunc.Closeable;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

public class PrettierFormatterStep {

	private static final Logger logger = LoggerFactory.getLogger(PrettierFormatterStep.class);

	public static final String NAME = "prettier-format";

	public static final String DEFAULT_VERSION = "2.8.8";

	public static final Map<String, String> defaultDevDependencies() {
		return defaultDevDependenciesWithPrettier(DEFAULT_VERSION);
	}

	public static final Map<String, String> defaultDevDependenciesWithPrettier(String version) {
		return Collections.singletonMap("prettier", version);
	}

	public static FormatterStep create(Map<String, String> devDependencies, Provisioner provisioner, File projectDir, File buildDir, File cacheDir, NpmPathResolver npmPathResolver, PrettierConfig prettierConfig) {
		requireNonNull(devDependencies);
		requireNonNull(provisioner);
		requireNonNull(buildDir);
		return FormatterStep.createLazy(NAME,
				() -> new State(NAME, devDependencies, projectDir, buildDir, cacheDir, npmPathResolver, prettierConfig),
				State::createFormatterFunc);
	}

	private static class State extends NpmFormatterStepStateBase implements Serializable {

		private static final long serialVersionUID = -539537027004745812L;
		private final PrettierConfig prettierConfig;

		State(String stepName, Map<String, String> devDependencies, File projectDir, File buildDir, File cacheDir, NpmPathResolver npmPathResolver, PrettierConfig prettierConfig) throws IOException {
			super(stepName,
					new NpmConfig(
							replaceDevDependencies(
									NpmResourceHelper.readUtf8StringFromClasspath(PrettierFormatterStep.class, "/com/diffplug/spotless/npm/prettier-package.json"),
									new TreeMap<>(devDependencies)),
							NpmResourceHelper.readUtf8StringFromClasspath(PrettierFormatterStep.class,
									"/com/diffplug/spotless/npm/common-serve.js",
									"/com/diffplug/spotless/npm/prettier-serve.js"),
							npmPathResolver.resolveNpmrcContent()),
					new NpmFormatterStepLocations(
							projectDir,
							buildDir,
							cacheDir,
							npmPathResolver));
			this.prettierConfig = requireNonNull(prettierConfig);
		}

		@Override
		@Nonnull
		public FormatterFunc createFormatterFunc() {
			try {
				logger.info("creating formatter function (starting server)");
				ServerProcessInfo prettierRestServer = toRuntime().npmRunServer();
				PrettierRestService restService = new PrettierRestService(prettierRestServer.getBaseUrl());
				String prettierConfigOptions = restService.resolveConfig(this.prettierConfig.getPrettierConfigPath(), this.prettierConfig.getOptions());
				return Closeable.ofDangerous(() -> endServer(restService, prettierRestServer), new PrettierFilePathPassingFormatterFunc(prettierConfigOptions, restService));
			} catch (IOException e) {
				throw ThrowingEx.asRuntime(e);
			}
		}

		private void endServer(PrettierRestService restService, ServerProcessInfo restServer) throws Exception {
			logger.info("Closing formatting function (ending server).");
			try {
				restService.shutdown();
			} catch (Throwable t) {
				logger.info("Failed to request shutdown of rest service via api. Trying via process.", t);
			}
			restServer.close();
		}

	}

	private static class PrettierFilePathPassingFormatterFunc implements FormatterFunc.NeedsFile {
		private final String prettierConfigOptions;
		private final PrettierRestService restService;

		public PrettierFilePathPassingFormatterFunc(String prettierConfigOptions, PrettierRestService restService) {
			this.prettierConfigOptions = requireNonNull(prettierConfigOptions);
			this.restService = requireNonNull(restService);
		}

		@Override
		public String applyWithFile(String unix, File file) throws Exception {
			final String prettierConfigOptionsWithFilepath = assertFilepathInConfigOptions(file);
			try {
				return restService.format(unix, prettierConfigOptionsWithFilepath);
			} catch (SimpleRestClient.SimpleRestResponseException e) {
				if (e.getStatusCode() != 200 && e.getResponseMessage().contains("No parser could be inferred")) {
					throw new PrettierMissingParserException(file, e);
				}
				throw e;
			}
		}

		private String assertFilepathInConfigOptions(File file) {
			// if it is already in the options, we do nothing
			if (prettierConfigOptions.contains("\"filepath\"") || prettierConfigOptions.contains("\"parser\"")) {
				return prettierConfigOptions;
			}
			// if the file has no name, we  cannot use it
			if (file.getName().trim().isEmpty()) {
				return prettierConfigOptions;
			}
			// if it is not there, we add it at the beginning of the Options
			final int startOfConfigOption = prettierConfigOptions.indexOf('{');
			final boolean hasAnyConfigOption = prettierConfigOptions.indexOf(':', startOfConfigOption + 1) != -1;
			final String filePathOption = "\"filepath\":\"" + file.getName() + "\"";
			return "{" + filePathOption + (hasAnyConfigOption ? "," : "") + prettierConfigOptions.substring(startOfConfigOption + 1);
		}
	}

}
