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

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nonnull;

class NpmConfig implements Serializable {

	private static final long serialVersionUID = 684264546497914877L;

	@Nonnull
	private final String packageJsonContent;

	private final String serveScriptContent;

	private final String npmrcContent;

	public NpmConfig(@Nonnull String packageJsonContent, String serveScriptContent, String npmrcContent) {
		this.packageJsonContent = Objects.requireNonNull(packageJsonContent);
		this.serveScriptContent = serveScriptContent;
		this.npmrcContent = npmrcContent;
	}

	@Nonnull
	public String getPackageJsonContent() {
		return packageJsonContent;
	}

	public String getServeScriptContent() {
		return serveScriptContent;
	}

	public String getNpmrcContent() {
		return npmrcContent;
	}
}
