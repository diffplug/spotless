/*
 * Copyright 2023 DiffPlug
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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.diffplug.spotless.ProcessRunner;

public class StandardNpmProcessFactory implements NpmProcessFactory {
	@Override
	public NpmProcess createNpmInstallProcess(NodeServerLayout nodeServerLayout, NpmFormatterStepLocations formatterStepLocations) {
		return new NpmInstall(nodeServerLayout.nodeModulesDir(), formatterStepLocations);
	}

	@Override
	public NpmProcess createNpmServeProcess(NodeServerLayout nodeServerLayout, NpmFormatterStepLocations formatterStepLocations) {
		return new NpmServe(nodeServerLayout.nodeModulesDir(), formatterStepLocations);
	}

	private static abstract class AbstractStandardNpmProcess implements NpmProcess {
		protected final ProcessRunner processRunner = ProcessRunner.usingRingBuffersOfCapacity(100 * 1024); // 100kB

		protected final File workingDir;
		protected final NpmFormatterStepLocations formatterStepLocations;

		public AbstractStandardNpmProcess(File workingDir, NpmFormatterStepLocations formatterStepLocations) {
			this.formatterStepLocations = formatterStepLocations;
			this.workingDir = workingDir;
		}

		protected String npmExecutable() {
			return formatterStepLocations.npmExecutable().getAbsolutePath();
		}

		protected abstract List<String> commandLine();

		protected Map<String, String> environmentVariables() {
			return Map.of(
					"PATH", formatterStepLocations.nodeExecutable().getParentFile().getAbsolutePath() + File.pathSeparator + System.getenv("PATH"));
		}

		@Override
		public ProcessRunner.LongRunningProcess start() {
			try {
				return processRunner.start(workingDir, environmentVariables(), null, true, commandLine());
			} catch (IOException e) {
				throw new NpmProcessException("Failed to launch npm command '" + describe() + "'.", e);
			}
		}

		@Override
		public String describe() {
			return String.format("%s in %s [%s]", getClass().getSimpleName(), workingDir, String.join(" ", commandLine()));
		}
	}

	private static class NpmInstall extends AbstractStandardNpmProcess {

		public NpmInstall(File workingDir, NpmFormatterStepLocations formatterStepLocations) {
			super(workingDir, formatterStepLocations);
		}

		@Override
		protected List<String> commandLine() {
			return List.of(
					npmExecutable(),
					"install",
					"--no-audit",
					"--no-fund",
					"--prefer-offline");
		}
	}

	private static class NpmServe extends AbstractStandardNpmProcess {

		public NpmServe(File workingDir, NpmFormatterStepLocations formatterStepLocations) {
			super(workingDir, formatterStepLocations);
		}

		@Override
		protected List<String> commandLine() {
			return List.of(
					npmExecutable(),
					"start",
					"--scripts-prepend-node-path=true");
		}
	}
}
