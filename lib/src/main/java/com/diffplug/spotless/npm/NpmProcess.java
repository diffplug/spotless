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

import java.util.concurrent.ExecutionException;

import com.diffplug.spotless.ProcessRunner.LongRunningProcess;
import com.diffplug.spotless.ProcessRunner.Result;

interface NpmProcess {

	String describe();

	LongRunningProcess start();

	default Result waitFor() {
		try (LongRunningProcess npmProcess = start()) {
			if (npmProcess.waitFor() != 0) {
				throw new NpmProcessException("Running npm command '" + describe() + "' failed with exit code: " + npmProcess.exitValue() + "\n\n" + npmProcess.result());
			}
			return npmProcess.result();
		} catch (InterruptedException e) {
			throw new NpmProcessException("Running npm command '" + describe() + "' was interrupted.", e);
		} catch (ExecutionException e) {
			throw new NpmProcessException("Running npm command '" + describe() + "' failed.", e);
		}
	}

	class NpmProcessException extends RuntimeException {
		private static final long serialVersionUID = 6424331316676759525L;

		public NpmProcessException(String message) {
			super(message);
		}

		public NpmProcessException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
