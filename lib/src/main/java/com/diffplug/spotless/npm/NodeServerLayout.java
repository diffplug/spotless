/*
 * Copyright 2020-2023 DiffPlug
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.diffplug.spotless.ThrowingEx;

class NodeServerLayout {

	private final File nodeModulesDir;
	private final File packageJsonFile;
	private final File serveJsFile;
	private final File npmrcFile;

	NodeServerLayout(File buildDir, String stepName, String stepSuffix) {
		this.nodeModulesDir = new File(buildDir, String.format("spotless-node-modules-%s-%s", stepName, stepSuffix));
		this.packageJsonFile = new File(nodeModulesDir, "package.json");
		this.serveJsFile = new File(nodeModulesDir, "serve.js");
		this.npmrcFile = new File(nodeModulesDir, ".npmrc");
	}

	File nodeModulesDir() {
		return nodeModulesDir;
	}

	File packageJsonFile() {
		return packageJsonFile;
	}

	File serveJsFile() {
		return serveJsFile;
	}

	public File npmrcFile() {
		return npmrcFile;
	}

	static File getBuildDirFromNodeModulesDir(File nodeModulesDir) {
		return nodeModulesDir.getParentFile();
	}

	public boolean isLayoutPrepared() {
		if (!nodeModulesDir().isDirectory()) {
			return false;
		}
		if (!packageJsonFile().isFile()) {
			return false;
		}
		if (!serveJsFile().isFile()) {
			return false;
		}
		// npmrc is optional, so must not be checked here
		return true;
	}

	public boolean isNodeModulesPrepared() {
		Path nodeModulesInstallDirPath = new File(nodeModulesDir(), "node_modules").toPath();
		if (!Files.isDirectory(nodeModulesInstallDirPath)) {
			return false;
		}
		// check if it is NOT empty
		return ThrowingEx.get(() -> {
			try (Stream<Path> entries = Files.list(nodeModulesInstallDirPath)) {
				return entries.findFirst().isPresent();
			}
		});
	}
}
