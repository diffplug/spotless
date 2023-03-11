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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.diffplug.spotless.ThrowingEx;

class NodeServerLayout {

	private static final Pattern PACKAGE_JSON_NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
	static final String NODE_MODULES = "node_modules";

	private final File nodeModulesDir;
	private final File packageJsonFile;

	private final File packageLockJsonFile;

	private final File serveJsFile;
	private final File npmrcFile;

	NodeServerLayout(File buildDir, String packageJsonContent) {
		this.nodeModulesDir = new File(buildDir, nodeModulesDirName(packageJsonContent));
		this.packageJsonFile = new File(nodeModulesDir, "package.json");
		this.packageLockJsonFile = new File(nodeModulesDir, "package-lock.json");
		this.serveJsFile = new File(nodeModulesDir, "serve.js");
		this.npmrcFile = new File(nodeModulesDir, ".npmrc");
	}

	private static String nodeModulesDirName(String packageJsonContent) {
		String md5Hash = NpmResourceHelper.md5(packageJsonContent);
		Matcher matcher = PACKAGE_JSON_NAME_PATTERN.matcher(packageJsonContent);
		if (!matcher.find()) {
			throw new IllegalArgumentException("package.json must contain a name property");
		}
		String packageName = matcher.group(1);
		return String.format("%s-node-modules-%s", packageName, md5Hash);
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

	public boolean isLayoutPrepared() {
		if (!nodeModulesDir().isDirectory()) {
			return false;
		}
		if (!packageJsonFile().isFile()) {
			return false;
		}
		if (!packageLockJsonFile.isFile()) {
			return false;
		}
		// npmrc is optional, so must not be checked here
		return serveJsFile().isFile();
	}

	public boolean isNodeModulesPrepared() {
		Path nodeModulesInstallDirPath = new File(nodeModulesDir(), NODE_MODULES).toPath();
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

	@Override
	public String toString() {
		return String.format(
				"NodeServerLayout[nodeModulesDir=%s, packageJsonFile=%s, serveJsFile=%s, npmrcFile=%s]",
				this.nodeModulesDir,
				this.packageJsonFile,
				this.serveJsFile,
				this.npmrcFile);
	}
}
