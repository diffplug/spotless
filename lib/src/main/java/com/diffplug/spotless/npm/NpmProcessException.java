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

import javax.annotation.CheckForNull;

import com.diffplug.spotless.ProcessRunner;

public class NpmProcessException extends RuntimeException {
	private static final long serialVersionUID = 6424331316676759525L;
	private final transient ProcessRunner.Result result;

	public NpmProcessException(String message, ProcessRunner.Result result) {
		super(message);
		this.result = result;
	}

	public NpmProcessException(String message, Throwable cause) {
		super(message, cause);
		this.result = null;
	}

	@CheckForNull
	public ProcessRunner.Result getResult() {
		return result;
	}
}
