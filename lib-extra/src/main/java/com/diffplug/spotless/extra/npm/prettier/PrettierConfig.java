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
package com.diffplug.spotless.extra.npm.prettier;

import java.io.File;

import com.diffplug.spotless.extra.npm.prettier.options.PrettierOptions;

public class PrettierConfig {

	private final File prettierConfigPath;

	private final PrettierOptions options;

	public PrettierConfig(File prettierConfigPath, PrettierOptions options) {
		this.prettierConfigPath = prettierConfigPath;
		this.options = options == null ? PrettierOptions.allDefaults() : options;
	}

	public File getPrettierConfigPath() {
		return prettierConfigPath;
	}

	public PrettierOptions getOptions() {
		return options;
	}
}
