/*
 * Copyright 2016 DiffPlug
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

import javax.annotation.Nonnull;
import java.io.Serializable;

class NpmConfig implements Serializable {

	private static final long serialVersionUID = -7660089232952131272L;

	private final String packageJsonContent;

	private final String npmModule;

	private final String serveScriptContent;

	public NpmConfig(String packageJsonContent, String npmModule, String serveScriptContent) {
		this.packageJsonContent = packageJsonContent;
		this.npmModule = npmModule;
		this.serveScriptContent = serveScriptContent;
	}

	public String getPackageJsonContent() {
		return packageJsonContent;
	}

	public String getNpmModule() {
		return npmModule;
	}

	@Nonnull
	public String getServeScriptContent() {
		return serveScriptContent;
	}
}
