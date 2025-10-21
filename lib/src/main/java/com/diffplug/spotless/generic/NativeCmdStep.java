/*
 * Copyright 2021-2025 DiffPlug
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
package com.diffplug.spotless.generic;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NativeCmdStep {
	// prevent direct instantiation
	private NativeCmdStep() {}

	public static FormatterStep create(String name, File pathToExe, List<String> arguments) {
		requireNonNull(name, "name");
		requireNonNull(pathToExe, "pathToExe");
		return FormatterStep.createLazy(name, () -> new State(FileSignature.promise(pathToExe), arguments), State::toRuntime, Runtime::toFunc);
	}

	static class State implements Serializable {
		private static final long serialVersionUID = 2L;
		final FileSignature.Promised pathToExe;
		final List<String> arguments;

		State(FileSignature.Promised pathToExe, List<String> arguments) {
			this.pathToExe = pathToExe;
			this.arguments = arguments;
		}

		Runtime toRuntime() {
			return new Runtime(pathToExe.get().getOnlyFile(), arguments);
		}
	}

	static class Runtime implements Serializable {
		private static final long serialVersionUID = 2L;
		final File pathToExe;
		final List<String> arguments;

		Runtime(File pathToExe, List<String> arguments) {
			this.pathToExe = pathToExe;
			this.arguments = arguments;
		}

		String format(ProcessRunner runner, String input) throws IOException, InterruptedException {
			List<String> argumentsWithPathToExe = new ArrayList<>();
			argumentsWithPathToExe.add(pathToExe.getAbsolutePath());
			if (arguments != null) {
				argumentsWithPathToExe.addAll(arguments);
			}
			return runner.exec(input.getBytes(UTF_8), argumentsWithPathToExe).assertExitZero(UTF_8);
		}

		FormatterFunc.Closeable toFunc() {
			ProcessRunner runner = new ProcessRunner();
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}
}
