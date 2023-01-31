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

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import com.diffplug.spotless.ResourceHarness;

public abstract class NpmFormatterStepCommonTests extends ResourceHarness {

	protected NpmPathResolver npmPathResolver() {
		return new NpmPathResolver(npmExecutable(), nodeExecutable(), npmrc(), Collections.emptyList());
	}

	private File npmExecutable() {
		return NpmExecutableResolver.tryFind().orElseThrow(() -> new IllegalStateException("cannot detect npm binary"));
	}

	private File nodeExecutable() {
		return NodeExecutableResolver.tryFindNextTo(npmExecutable()).orElseThrow(() -> new IllegalStateException("cannot detect node binary"));
	}

	private File npmrc() {
		return new NpmrcResolver().tryFind().orElse(null);
	}

	private File buildDir = null;

	protected File buildDir() throws IOException {
		if (this.buildDir == null) {
			this.buildDir = newFolder("build-dir");
		}
		return this.buildDir;
	}

	private File projectDir = null;

	protected File projectDir() throws IOException {
		if (this.projectDir == null) {
			this.projectDir = newFolder("project-dir");
		}
		return this.projectDir;
	}
}
