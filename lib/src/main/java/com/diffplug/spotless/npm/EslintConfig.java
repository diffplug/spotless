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
import java.io.Serializable;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.ThrowingEx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class EslintConfig implements Serializable {

	private static final long serialVersionUID = -6196834313082791248L;

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	@Nullable
	private final transient File eslintConfigPath;

	@SuppressWarnings("unused")
	private final FileSignature eslintConfigPathSignature;

	private final String eslintConfigJs;

	public EslintConfig(@Nullable File eslintConfigPath, @Nullable String eslintConfigJs) {
		try {
			this.eslintConfigPath = eslintConfigPath;
			this.eslintConfigPathSignature = eslintConfigPath != null ? FileSignature.signAsList(this.eslintConfigPath) : FileSignature.signAsList();
			this.eslintConfigJs = eslintConfigJs;
		} catch (IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	public EslintConfig withEslintConfigPath(@Nullable File eslintConfigPath) {
		return new EslintConfig(eslintConfigPath, this.eslintConfigJs);
	}

	@Nullable
	public File getEslintConfigPath() {
		return eslintConfigPath;
	}

	@Nullable
	public String getEslintConfigJs() {
		return eslintConfigJs;
	}

	public EslintConfig verify() {
		if (eslintConfigPath == null && eslintConfigJs == null) {
			throw new IllegalArgumentException("ESLint must be configured using either a configFile or a configJs - but both are null.");
		}
		return this;
	}
}
