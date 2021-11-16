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
package com.diffplug.spotless.maven.incremental;

import java.nio.file.Path;

import org.apache.maven.project.MavenProject;

class FileIndexConfig {

	private static final String INDEX_FILE_NAME = "spotless-index";

	private final MavenProject project;
	private final PluginFingerprint pluginFingerprint;

	FileIndexConfig(MavenProject project) {
		this(project, PluginFingerprint.empty());
	}

	FileIndexConfig(MavenProject project, PluginFingerprint pluginFingerprint) {
		this.project = project;
		this.pluginFingerprint = pluginFingerprint;
	}

	Path getProjectDir() {
		return project.getBasedir().toPath();
	}

	Path getIndexFile() {
		Path targetDir = getProjectDir().resolve(project.getBuild().getDirectory());
		return targetDir.resolve(INDEX_FILE_NAME);
	}

	PluginFingerprint getPluginFingerprint() {
		return pluginFingerprint;
	}
}
