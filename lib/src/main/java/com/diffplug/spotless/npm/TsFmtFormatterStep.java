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
package com.diffplug.spotless.npm;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

public class TsFmtFormatterStep {
	public static final String NAME = "tsfmt-format";

	@Deprecated
	public static FormatterStep create(Provisioner provisioner, File buildDir, @Nullable File npm, File baseDir, @Nullable TypedTsFmtConfigFile configFile, @Nullable Map<String, Object> inlineTsFmtSettings) {
		return create(defaultDevDependencies(), provisioner, buildDir, npm, configFile, inlineTsFmtSettings);
	}

	public static FormatterStep create(Map<String, String> versions, Provisioner provisioner, File buildDir, @Nullable File npm, @Nullable TypedTsFmtConfigFile configFile, @Nullable Map<String, Object> inlineTsFmtSettings) {
		requireNonNull(provisioner);
		requireNonNull(buildDir);
		return FormatterStep.createLazy(NAME,
				() -> new State(NAME, versions, provisioner, buildDir, npm, configFile, inlineTsFmtSettings),
				State::createFormatterFunc);
	}

	public static Map<String, String> defaultDevDependencies() {
		return defaultDevDependenciesWithTsFmt("7.2.2");
	}

	public static Map<String, String> defaultDevDependenciesWithTsFmt(String typescriptFormatter) {
		TreeMap<String, String> defaults = new TreeMap<>();
		defaults.put("typescript-formatter", typescriptFormatter);
		defaults.put("typescript", "3.3.3");
		defaults.put("tslint", "5.12.1");
		return defaults;
	}

	public static class State extends NpmFormatterStepStateBase implements Serializable {

		private static final long serialVersionUID = -3811104513825329168L;

		private final TreeMap<String, Object> inlineTsFmtSettings;

		private final File buildDir;

		@Nullable
		private final TypedTsFmtConfigFile configFile;

		@Deprecated
		public State(String stepName, Provisioner provisioner, File buildDir, @Nullable File npm, @Nullable TypedTsFmtConfigFile configFile, @Nullable Map<String, Object> inlineTsFmtSettings) throws IOException {
			this(stepName, defaultDevDependencies(), provisioner, buildDir, npm, configFile, inlineTsFmtSettings);
		}

		public State(String stepName, Map<String, String> versions, Provisioner provisioner, File buildDir, @Nullable File npm, @Nullable TypedTsFmtConfigFile configFile, @Nullable Map<String, Object> inlineTsFmtSettings) throws IOException {
			super(stepName,
					provisioner,
					new NpmConfig(
							replaceDevDependencies(readFileFromClasspath(TsFmtFormatterStep.class, "/com/diffplug/spotless/npm/tsfmt-package.json"), new TreeMap<>(versions)),
							"typescript-formatter"),
					buildDir,
					npm);
			this.buildDir = requireNonNull(buildDir);
			this.configFile = configFile;
			this.inlineTsFmtSettings = inlineTsFmtSettings == null ? new TreeMap<>() : new TreeMap<>(inlineTsFmtSettings);
		}

		@Override
		@Nonnull
		public FormatterFunc createFormatterFunc() {

			Map<String, Object> tsFmtOptions = unifyOptions();

			final NodeJSWrapper nodeJSWrapper = nodeJSWrapper();
			final V8ObjectWrapper tsFmt = nodeJSWrapper.require(nodeModulePath());
			final V8ObjectWrapper formatterOptions = nodeJSWrapper.createNewObject(tsFmtOptions);

			final TsFmtResult[] tsFmtResult = new TsFmtResult[1];
			final Exception[] toThrow = new Exception[1];

			V8FunctionWrapper formatResultCallback = createFormatResultCallback(nodeJSWrapper, tsFmtResult, toThrow);

			/* var result = {
			fileName: fileName,
			settings: formatSettings,
			message: message, <-- string
			error: error, <-- boolean
			src: content,
			dest: formattedCode, <-- result
			}
			*/
			return FormatterFunc.Closeable.of(() -> {
				asList(formatResultCallback, formatterOptions, tsFmt, nodeJSWrapper).forEach(ReflectiveObjectWrapper::release);
			}, input -> {
				tsFmtResult[0] = null;

				// function processString(fileName: string, content: string, opts: Options): Promise<Result> {

				try (
						V8ArrayWrapper processStringArgs = nodeJSWrapper.createNewArray("spotless-format-string.ts", input, formatterOptions);
						V8ObjectWrapper promise = tsFmt.executeObjectFunction("processString", processStringArgs);
						V8ArrayWrapper callbacks = nodeJSWrapper.createNewArray(formatResultCallback)) {

					promise.executeVoidFunction("then", callbacks);

					while (tsFmtResult[0] == null && toThrow[0] == null) {
						nodeJSWrapper.handleMessage();
					}

					if (toThrow[0] != null) {
						throw ThrowingEx.asRuntime(toThrow[0]);
					}

					if (tsFmtResult[0] == null) {
						throw new IllegalStateException("should never happen");
					}
					if (tsFmtResult[0].isError()) {
						throw new RuntimeException(tsFmtResult[0].getMessage());
					}
					return tsFmtResult[0].getFormatted();
				}
			});
		}

		private V8FunctionWrapper createFormatResultCallback(NodeJSWrapper nodeJSWrapper, TsFmtResult[] outputTsFmtResult, Exception[] toThrow) {
			return nodeJSWrapper.createNewFunction((receiver, parameters) -> {
				try (final V8ObjectWrapper result = parameters.getObject(0)) {
					outputTsFmtResult[0] = new TsFmtResult(result.getString("message"), result.getBoolean("error"), result.getString("dest"));
				} catch (Exception e) {
					toThrow[0] = e;
				}
				return receiver;
			});
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
	}
}
