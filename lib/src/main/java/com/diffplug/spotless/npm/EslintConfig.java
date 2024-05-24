/*
 * Copyright 2016-2024 DiffPlug
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
import java.io.Serializable;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;

public class EslintConfig implements Serializable {
	@SuppressWarnings("unused")
	private final FileSignature.Promised eslintConfigPathSignature;
	private final String eslintConfigJs;

	public EslintConfig(@Nullable File eslintConfigPath, @Nullable String eslintConfigJs) {
		this.eslintConfigPathSignature = eslintConfigPath == null ? null : FileSignature.promise(eslintConfigPath);
		this.eslintConfigJs = eslintConfigJs;
	}

	public EslintConfig withEslintConfigPath(@Nullable File eslintConfigPath) {
		return new EslintConfig(eslintConfigPath, this.eslintConfigJs);
	}

	@Nullable
	public File getEslintConfigPath() {
		return eslintConfigPathSignature == null ? null : eslintConfigPathSignature.get().getOnlyFile();
	}

	@Nullable
	public String getEslintConfigJs() {
		return eslintConfigJs;
	}

	public EslintConfig verify() {
		if (eslintConfigPathSignature == null && eslintConfigJs == null) {
			throw new IllegalArgumentException("ESLint must be configured using either a configFile or a configJs - but both are null.");
		}
		return this;
	}
}
