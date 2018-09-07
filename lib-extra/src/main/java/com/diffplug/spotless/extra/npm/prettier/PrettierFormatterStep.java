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
package com.diffplug.spotless.extra.npm.prettier;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.extra.npm.NpmConfig;
import com.diffplug.spotless.extra.npm.NpmFormatterStepStateBase;
import com.diffplug.spotless.extra.npm.prettier.options.PrettierOptions;
import com.diffplug.spotless.extra.npm.wrapper.*;

public class PrettierFormatterStep {

	public static final String NAME = "prettier-format";

	public static FormatterStep create(Provisioner provisioner, File buildDir, File npm, PrettierConfig prettierConfig) {
		requireNonNull(provisioner);
		requireNonNull(buildDir);
		requireNonNull(npm);
		return FormatterStep.createLazy(NAME,
				() -> new State(NAME, provisioner, buildDir, npm, prettierConfig),
				State::createFormatterFunc);
	}

	public static class State extends NpmFormatterStepStateBase implements Serializable {

		private static final long serialVersionUID = -3811104513825329168L;
		private final PrettierConfig prettierConfig;

		public State(String stepName, Provisioner provisioner, File buildDir, File npm, PrettierConfig prettierConfig) throws IOException {
			super(stepName,
					provisioner,
					new NpmConfig(
							readFileFromClasspath(PrettierFormatterStep.class, "package.json"),
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

				// TODO (simschla, 14.08.18): use prettier options
				final PrettierOptions[] resolvedPrettierOptions = new PrettierOptions[1];
				if (this.prettierConfig.getPrettierConfigPath() != null) {
					final Exception[] toThrow = new Exception[1];
					try (
							V8FunctionWrapper resolveConfigCallback = nodeJSWrapper.createNewFunction((receiver, parameters) -> {
								try {
									final V8ObjectWrapper configOptions = parameters.getObject(0); // why is this always null
									if (configOptions == null) {
										toThrow[0] = new IllegalArgumentException("Cannot find or read config file " + this.prettierConfig.getPrettierConfigPath());
									} else {
										resolvedPrettierOptions[0] = PrettierOptions
												.fromV8Object(configOptions)
												.overrideWith(this.prettierConfig.getOptions());
									}
								} catch (Exception e) {
									toThrow[0] = e;
								}
								return receiver;
							});
							V8ObjectWrapper resolveConfigOption = nodeJSWrapper.createNewObject()
									.add("config", this.prettierConfig.getPrettierConfigPath().getAbsolutePath());
							V8ArrayWrapper resolveConfigParams = nodeJSWrapper.createNewArray()
									.pushNull()
									.push(resolveConfigOption);
							V8ObjectWrapper promise = prettier.executeObjectFunction("resolveConfig", resolveConfigParams);
							V8ArrayWrapper callbacks = nodeJSWrapper.createNewArray(resolveConfigCallback);) {

						promise.executeVoidFunction("then", callbacks);

						while (resolvedPrettierOptions[0] == null && toThrow[0] == null) {
							nodeJSWrapper.handleMessage();
						}

						if (toThrow[0] != null) {
							throw ThrowingEx.asRuntime(toThrow[0]);
						}
					}
				} else {
					resolvedPrettierOptions[0] = this.prettierConfig.getOptions();
				}

				//				final V8ObjectWrapper prettierConfig = nodeJSWrapper.createNewObject()
				//					.add("parser", "typescript");

				final V8ObjectWrapper prettierConfig = resolvedPrettierOptions[0].toV8Object(nodeJSWrapper);

				return FormatterFunc.Closeable.of(() -> {
					System.out.println("RELEASING PRETTIER FORMATTER FUNCTION");
					asList(prettierConfig, prettier, nodeJSWrapper).forEach(ReflectiveObjectWrapper::release);
				}, input -> {
					try (V8ArrayWrapper formatParams = nodeJSWrapper.createNewArray(input, prettierConfig)) {
						String result = prettier.executeStringFunction("format", formatParams);
						return result;
					}
				});
			} catch (Exception e) {
				throw ThrowingEx.asRuntime(e);
			}

		}
	}
}
