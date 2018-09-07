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
package com.diffplug.spotless.extra.npm.tsfmt;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.npm.NpmConfig;
import com.diffplug.spotless.extra.npm.NpmFormatterStepStateBase;
import com.diffplug.spotless.extra.npm.wrapper.*;

public class TsFmtFormatterStep {

	public static final String NAME = "tsfmt-format";

	public static FormatterStep create(Provisioner provisioner, File buildDir, File npm, TsFmtOptions tsFmtOptions) {
		requireNonNull(provisioner);
		requireNonNull(buildDir);
		requireNonNull(npm);
		requireNonNull(tsFmtOptions);
		return FormatterStep.createLazy(NAME,
				() -> new State(NAME, provisioner, buildDir, npm, tsFmtOptions),
				State::createFormatterFunc);
	}

	public static class State extends NpmFormatterStepStateBase implements Serializable {

		private static final long serialVersionUID = -3811104513825329168L;

		private final TsFmtOptions tsFmtOptions;

		public State(String stepName, Provisioner provisioner, File buildDir, File npm, TsFmtOptions tsFmtOptions) throws IOException {
			super(stepName,
					provisioner,
					new NpmConfig(
							readFileFromClasspath(TsFmtFormatterStep.class, "package.json"),
							"typescript-formatter"),
					buildDir,
					npm);
			this.tsFmtOptions = tsFmtOptions;
		}

		@Override
		@Nonnull
		public FormatterFunc createFormatterFunc() {

			final NodeJSWrapper nodeJSWrapper = nodeJSWrapper();
			final V8ObjectWrapper tsFmt = nodeJSWrapper.require(nodeModulePath());

			final V8ObjectWrapper tsfmtOptions = tsFmtOptions.toV8Object(nodeJSWrapper);

			final TsFmtResult[] tsFmtResult = new TsFmtResult[1];
			V8FunctionWrapper formatResultCallback = nodeJSWrapper.createNewFunction((receiver, parameters) -> {
				final V8ObjectWrapper result = parameters.getObject(0);
				tsFmtResult[0] = new TsFmtResult(result.getString("message"), result.getBoolean("error"), result.getString("dest"));
				//result.release(); // TODO (simschla, 09.08.18): verify if release needed?
				return receiver;
			});

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
				// TODO (simschla, 09.08.18): maybe release node stuff?
				System.out.println("RELEASING FORMATTER FUNCTION");
				asList(formatResultCallback, tsfmtOptions, tsFmt, nodeJSWrapper).forEach(ReflectiveObjectWrapper::release);
			}, input -> {
				tsFmtResult[0] = null;

				// function processString(fileName: string, content: string, opts: Options): Promise<Result> {

				try (
						V8ArrayWrapper processStringArgs = nodeJSWrapper.createNewArray("spotless-format-string.ts", input, tsfmtOptions);
						V8ObjectWrapper promise = tsFmt.executeObjectFunction("processString", processStringArgs);
						V8ArrayWrapper callbacks = nodeJSWrapper.createNewArray(formatResultCallback)) {

					promise.executeVoidFunction("then", callbacks);
					// TODO (simschla, 14.08.18): handle promise resolving without success

					while (tsFmtResult[0] == null) {
						nodeJSWrapper.handleMessage();
					}

					if (tsFmtResult[0] == null) {
						throw new IllegalStateException("should never happen");
					}
					if (tsFmtResult[0].isError()) {
						throw new RuntimeException(tsFmtResult[0].getMessage());
					}
					return tsFmtResult[0].getFormatted();
				}

				// TODO (simschla, 09.08.18): release
				//					callbacks.release();
				//					args.release();
				//					promise.release();

			});
		}
	}
}
