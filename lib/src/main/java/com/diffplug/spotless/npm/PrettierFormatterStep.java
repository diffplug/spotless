/*
 * Copyright 2016-2020 DiffPlug
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterFunc.Closeable;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

public class PrettierFormatterStep {

	private static final Logger logger = Logger.getLogger(PrettierFormatterStep.class.getName());

	public static final String NAME = "prettier-format";

	public static final Map<String, String> defaultDevDependencies() {
		return defaultDevDependenciesWithPrettier("2.0.5");
	}

	public static final Map<String, String> defaultDevDependenciesWithPrettier(String version) {
		return Collections.singletonMap("prettier", version);
	}

	@Deprecated
	public static FormatterStep create(Provisioner provisioner, File buildDir, @Nullable File npm, PrettierConfig prettierConfig) {
		return create(defaultDevDependencies(), provisioner, buildDir, npm, prettierConfig);
	}

	public static FormatterStep create(Map<String, String> devDependencies, Provisioner provisioner, File buildDir, @Nullable File npm, PrettierConfig prettierConfig) {
		requireNonNull(devDependencies);
		requireNonNull(provisioner);
		requireNonNull(buildDir);
		return FormatterStep.createLazy(NAME,
				() -> new State(NAME, devDependencies, buildDir, npm, prettierConfig),
				State::createFormatterFunc);
	}

	public static class State extends NpmFormatterStepStateBase implements Serializable {

		private static final long serialVersionUID = -539537027004745812L;
		private final PrettierConfig prettierConfig;

		State(String stepName, Map<String, String> devDependencies, File buildDir, @Nullable File npm, PrettierConfig prettierConfig) throws IOException {
			super(stepName,
					new NpmConfig(
							replaceDevDependencies(
									NpmResourceHelper.readUtf8StringFromClasspath(PrettierFormatterStep.class, "/com/diffplug/spotless/npm/prettier-package.json"),
									new TreeMap<>(devDependencies)),
							"prettier",
							NpmResourceHelper.readUtf8StringFromClasspath(PrettierFormatterStep.class, "/com/diffplug/spotless/npm/prettier-serve.js")),
					buildDir,
					npm);
			this.prettierConfig = requireNonNull(prettierConfig);
		}

		@Override
		@Nonnull
		public FormatterFunc createFormatterFunc() {
			try {
				ServerProcessInfo prettierRestServer = npmRunServer();
				PrettierRestService restService = new PrettierRestService(prettierRestServer.getBaseUrl());
				String prettierConfigOptions = restService.resolveConfig(this.prettierConfig.getPrettierConfigPath(), this.prettierConfig.getOptions());
				return Closeable.of(() -> endServer(restService, prettierRestServer), input -> restService.format(input, prettierConfigOptions));
			} catch (Exception e) {
				throw ThrowingEx.asRuntime(e);
			}
		}

		private void endServer(PrettierRestService restService, ServerProcessInfo restServer) throws Exception {
			try {
				restService.shutdown();
			} catch (Throwable t) {
				logger.log(Level.INFO, "Failed to request shutdown of rest service via api. Trying via process.", t);
			}
			restServer.close();
		}

	}
}
