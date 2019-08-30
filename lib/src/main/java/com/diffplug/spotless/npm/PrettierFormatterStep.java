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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

public class PrettierFormatterStep {

	public static final String NAME = "prettier-format";

	public static final Map<String, String> defaultDevDependencies() {
		return defaultDevDependenciesWithPrettier("1.16.4");
	}

	public static final Map<String, String> defaultDevDependenciesWithPrettier(String version) {
		return Collections.singletonMap("prettier", version);
	}

	@Deprecated
	public static FormatterStep create(Provisioner provisioner, File buildDir, @Nullable File npm, PrettierConfig prettierConfig) {
		return create(defaultDevDependencies(), provisioner, buildDir, npm, prettierConfig);
	}

	public static FormatterStep create(Map<String, String> devDependencies, Provisioner provisioner, File buildDir, @Nullable File npm,
			PrettierConfig prettierConfig) {
		requireNonNull(devDependencies);
		requireNonNull(provisioner);
		requireNonNull(buildDir);
		return FormatterStep.createLazy(NAME,
				() -> new State(NAME, devDependencies, provisioner, buildDir, npm, prettierConfig),
				State::createFormatterFunc);
	}

	public static class State extends NpmFormatterStepStateBase implements Serializable {

		private static final class FormatterFuncWithFilename implements FormatterFunc {
			private final NodeJSWrapper nodeJSWrapper;
			private final V8ObjectWrapper prettier;
			private final Map<String, Object> optionsToPass;

			private FormatterFuncWithFilename(NodeJSWrapper nodeJSWrapper, V8ObjectWrapper prettier, Map<String, Object> optionsToPass) {
				this.nodeJSWrapper = nodeJSWrapper;
				this.prettier = prettier;
				this.optionsToPass = optionsToPass;
			}

			@Override
			public String apply(String input, @Nullable File source) throws Exception {

				HashMap<String, Object> actualOptions = new HashMap<>(optionsToPass);

				if (source != null) {
					actualOptions.put("filepath", source.getCanonicalPath());
				}

				try (V8ObjectWrapper prettierConfig = nodeJSWrapper.createNewObject(actualOptions);
						V8ArrayWrapper formatParams = nodeJSWrapper.createNewArray(input, prettierConfig);) {
					String result = prettier.executeStringFunction("format", formatParams);
					return result;
				}
			}

			@Override
			public String apply(String input) throws Exception {
				return apply(input, null);
			}
		}

		private static final long serialVersionUID = -3811104513825329168L;
		private final PrettierConfig prettierConfig;

		State(String stepName, Map<String, String> devDependencies, Provisioner provisioner, File buildDir, @Nullable File npm, PrettierConfig prettierConfig)
				throws IOException {
			super(stepName,
					provisioner,
					new NpmConfig(
							replaceDevDependencies(
									readFileFromClasspath(PrettierFormatterStep.class, "/com/diffplug/spotless/npm/prettier-package.json"),
									new TreeMap<>(devDependencies)),
							"prettier"),
					buildDir,
					npm);
			this.prettierConfig = requireNonNull(prettierConfig);
		}

		@Override
		@Nonnull
		public FormatterFunc createFormatterFunc() {

			try {
				final NodeJSWrapper nodeJSWrapper = nodeJSWrapper();
				final V8ObjectWrapper prettier = nodeJSWrapper.require(nodeModulePath());

				@SuppressWarnings("unchecked")
				final Map<String, Object>[] resolvedPrettierOptions = new Map[1];

				if (this.prettierConfig.getPrettierConfigPath() != null) {
					final Exception[] toThrow = new Exception[1];
					try (
							V8FunctionWrapper resolveConfigCallback = createResolveConfigFunction(nodeJSWrapper, resolvedPrettierOptions, toThrow);
							V8ObjectWrapper resolveConfigOption = createResolveConfigOptionObj(nodeJSWrapper);
							V8ArrayWrapper resolveConfigParams = createResolveConfigParamsArray(nodeJSWrapper, resolveConfigOption);

							V8ObjectWrapper promise = prettier.executeObjectFunction("resolveConfig", resolveConfigParams);
							V8ArrayWrapper callbacks = nodeJSWrapper.createNewArray(resolveConfigCallback);) {

						promise.executeVoidFunction("then", callbacks);
						executeResolution(nodeJSWrapper, resolvedPrettierOptions, toThrow);
					}
				} else {
					resolvedPrettierOptions[0] = this.prettierConfig.getOptions();
				}
				Map<String, Object> optionsToPass = resolvedPrettierOptions[0];

				AutoCloseable closeable = () -> {
					asList(prettier, nodeJSWrapper).forEach(ReflectiveObjectWrapper::release);
				};

				FormatterFunc func = new FormatterFuncWithFilename(nodeJSWrapper, prettier, optionsToPass);
				return FormatterFunc.Closeable.of(closeable, func);
			} catch (

			Exception e) {
				throw ThrowingEx.asRuntime(e);
			}
		}

		private V8FunctionWrapper createResolveConfigFunction(NodeJSWrapper nodeJSWrapper, Map<String, Object>[] outputOptions, Exception[] toThrow) {
			return nodeJSWrapper.createNewFunction((receiver, parameters) -> {
				try {
					try (final V8ObjectWrapper configOptions = parameters.getObject(0)) {
						if (configOptions == null) {
							toThrow[0] = new IllegalArgumentException("Cannot find or read config file " + this.prettierConfig.getPrettierConfigPath());
						} else {
							Map<String, Object> resolvedOptions = new TreeMap<>(V8ObjectUtilsWrapper.toMap(configOptions));
							resolvedOptions.putAll(this.prettierConfig.getOptions());
							outputOptions[0] = resolvedOptions;
						}
					}
				} catch (Exception e) {
					toThrow[0] = e;
				}
				return receiver;
			});
		}

		private V8ObjectWrapper createResolveConfigOptionObj(NodeJSWrapper nodeJSWrapper) {
			return nodeJSWrapper.createNewObject()
					.add("config", this.prettierConfig.getPrettierConfigPath().getAbsolutePath());
		}

		private V8ArrayWrapper createResolveConfigParamsArray(NodeJSWrapper nodeJSWrapper, V8ObjectWrapper resolveConfigOption) {
			return nodeJSWrapper.createNewArray()
					.pushNull()
					.push(resolveConfigOption);
		}

		private void executeResolution(NodeJSWrapper nodeJSWrapper, Map<String, Object>[] resolvedPrettierOptions, Exception[] toThrow) {
			while (resolvedPrettierOptions[0] == null && toThrow[0] == null) {
				nodeJSWrapper.handleMessage();
			}

			if (toThrow[0] != null) {
				throw ThrowingEx.asRuntime(toThrow[0]);
			}
		}

	}
}
