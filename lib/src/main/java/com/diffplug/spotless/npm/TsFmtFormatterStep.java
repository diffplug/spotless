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
package com.diffplug.spotless.npm;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterFunc.Closeable;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

public class TsFmtFormatterStep {

	private static final Logger logger = LoggerFactory.getLogger(TsFmtFormatterStep.class);

	public static final String NAME = "tsfmt-format";

	public static FormatterStep create(Map<String, String> versions, Provisioner provisioner, File projectDir, File buildDir, File cacheDir, NpmPathResolver npmPathResolver, @Nullable TypedTsFmtConfigFile configFile, @Nullable Map<String, Object> inlineTsFmtSettings) {
		requireNonNull(provisioner);
		requireNonNull(buildDir);
		return FormatterStep.createLazy(NAME,
				() -> new State(NAME, versions, projectDir, buildDir, cacheDir, npmPathResolver, configFile, inlineTsFmtSettings),
				State::createFormatterFunc);
	}

	public static Map<String, String> defaultDevDependencies() {
		return defaultDevDependenciesWithTsFmt("7.2.2");
	}

	public static Map<String, String> defaultDevDependenciesWithTsFmt(String typescriptFormatter) {
		TreeMap<String, String> defaults = new TreeMap<>();
		defaults.put("typescript-formatter", typescriptFormatter);
		defaults.put("typescript", "3.9.5");
		defaults.put("tslint", "6.1.2");
		return defaults;
	}

	public static class State extends NpmFormatterStepStateBase implements Serializable {

		private static final long serialVersionUID = -3789035117345809383L;

		private final TreeMap<String, Object> inlineTsFmtSettings;

		private final File buildDir;

		@Nullable
		private final TypedTsFmtConfigFile configFile;

		public State(String stepName, Map<String, String> versions, File projectDir, File buildDir, File cacheDir, NpmPathResolver npmPathResolver, @Nullable TypedTsFmtConfigFile configFile, @Nullable Map<String, Object> inlineTsFmtSettings) throws IOException {
			super(stepName,
					new NpmConfig(
							replaceDevDependencies(NpmResourceHelper.readUtf8StringFromClasspath(TsFmtFormatterStep.class, "/com/diffplug/spotless/npm/tsfmt-package.json"), new TreeMap<>(versions)),
							NpmResourceHelper.readUtf8StringFromClasspath(PrettierFormatterStep.class,
									"/com/diffplug/spotless/npm/common-serve.js",
									"/com/diffplug/spotless/npm/tsfmt-serve.js"),
							npmPathResolver.resolveNpmrcContent()),
					new NpmFormatterStepLocations(
							projectDir,
							buildDir,
							cacheDir,
							npmPathResolver::resolveNpmExecutable,
							npmPathResolver::resolveNodeExecutable));
			this.buildDir = requireNonNull(buildDir);
			this.configFile = configFile;
			this.inlineTsFmtSettings = inlineTsFmtSettings == null ? new TreeMap<>() : new TreeMap<>(inlineTsFmtSettings);
		}

		@Override
		@Nonnull
		public FormatterFunc createFormatterFunc() {
			try {
				Map<String, Object> tsFmtOptions = unifyOptions();
				ServerProcessInfo tsfmtRestServer = npmRunServer();
				TsFmtRestService restService = new TsFmtRestService(tsfmtRestServer.getBaseUrl());
				return Closeable.ofDangerous(() -> endServer(restService, tsfmtRestServer), input -> restService.format(input, tsFmtOptions));
			} catch (IOException e) {
				throw ThrowingEx.asRuntime(e);
			}
		}

		private Map<String, Object> unifyOptions() {
			Map<String, Object> unified = new HashMap<>();
			if (!this.inlineTsFmtSettings.isEmpty()) {
				File targetFile = new File(this.buildDir, "inline-tsfmt.json");
				SimpleJsonWriter.of(this.inlineTsFmtSettings).toJsonFile(targetFile);
				unified.put("tsfmt", true);
				unified.put("tsfmtFile", targetFile.getAbsolutePath());
			} else if (this.configFile != null) {
				unified.put(this.configFile.configFileEnabledOptionName(), Boolean.TRUE);
				unified.put(this.configFile.configFileOptionName(), this.configFile.absolutePath());
			}
			return unified;
		}

		private void endServer(TsFmtRestService restService, ServerProcessInfo restServer) throws Exception {
			try {
				restService.shutdown();
			} catch (Throwable t) {
				logger.info("Failed to request shutdown of rest service via api. Trying via process.", t);
			}
			restServer.close();
		}
	}
}
