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
package com.diffplug.spotless.maven.incremental;

import java.io.File;
import java.nio.file.Path;

import org.apache.maven.plugins.annotations.Parameter;

import jakarta.annotation.Nullable;

public class UpToDateChecking {

	@Parameter
	private boolean enabled;

	@Parameter
	private String indexFile;

	public boolean isEnabled() {
		return enabled;
	}

	@Nullable public Path getIndexFile() {
		return indexFile == null ? null : new File(indexFile).toPath();
	}

	public static UpToDateChecking enabled() {
		UpToDateChecking upToDateChecking = new UpToDateChecking();
		upToDateChecking.enabled = true;
		return upToDateChecking;
	}
}
