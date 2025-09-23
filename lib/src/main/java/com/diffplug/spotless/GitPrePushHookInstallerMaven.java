/*
 * Copyright 2025 DiffPlug
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
package com.diffplug.spotless;

import static com.diffplug.spotless.GitPrePushHookInstaller.Executor.MAVEN;

import java.io.File;

/**
 * Implementation of {@link GitPrePushHookInstaller} specifically for Maven-based projects.
 * This class installs a Git pre-push hook that uses Maven to check and apply Spotless formatting.
 */
public class GitPrePushHookInstallerMaven extends GitPrePushHookInstaller {

	public GitPrePushHookInstallerMaven(GitPreHookLogger logger, File root) {
		super(logger, root);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String preHookContent() {
		return preHookTemplate(MAVEN, "spotless:check", "spotless:apply");
	}
}
