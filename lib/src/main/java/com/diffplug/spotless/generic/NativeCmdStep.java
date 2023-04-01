/*
 * Copyright 2021 DiffPlug
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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;

public class NativeCmdStep {
	// prevent direct instantiation
	private NativeCmdStep() {}

	public static FormatterStep create(String name, File pathToExe, List<String> arguments) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(pathToExe, "pathToExe");
		return FormatterStep.createLazy(name, () -> new State(FileSignature.signAsList(pathToExe), arguments), State::toFunc);
	}

	static class State implements Serializable {
		private static final long serialVersionUID = 1L;

		final FileSignature pathToExe;

		final List<String> arguments;

		State(FileSignature pathToExe, List<String> arguments) {
			this.pathToExe = pathToExe;
			this.arguments = arguments;
		}

		String format(ProcessRunner runner, String input) throws IOException, InterruptedException {
			List<String> argumentsWithPathToExe = new ArrayList<>();
			argumentsWithPathToExe.add(pathToExe.getOnlyFile().getAbsolutePath());
			if (arguments != null) {
				argumentsWithPathToExe.addAll(arguments);
			}
			return runner.exec(input.getBytes(StandardCharsets.UTF_8), argumentsWithPathToExe).assertExitZero(StandardCharsets.UTF_8);
		}

		FormatterFunc.Closeable toFunc() {
			ProcessRunner runner = new ProcessRunner();
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}
}
