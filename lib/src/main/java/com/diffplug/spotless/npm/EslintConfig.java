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
import java.util.Objects;

import javax.annotation.Nullable;

public class EslintConfig implements Serializable {

	private static final long serialVersionUID = -5436020379478813853L;

	@SuppressWarnings("unused")
	private final RoundtrippableFile eslintConfigPath;
	private final String eslintConfigJs;

	public EslintConfig(@Nullable File eslintConfigPath, @Nullable String eslintConfigJs) {
		this.eslintConfigPath = eslintConfigPath == null ? null : new RoundtrippableFile(eslintConfigPath);
		this.eslintConfigJs = eslintConfigJs;
	}

	public EslintConfig withEslintConfigPath(@Nullable File eslintConfigPath) {
		return new EslintConfig(eslintConfigPath, this.eslintConfigJs);
	}

	@Nullable
	public File getEslintConfigPath() {
		return eslintConfigPath == null ? null : eslintConfigPath.file();
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

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof EslintConfig))
			return false;
		EslintConfig that = (EslintConfig) o;
		return Objects.equals(eslintConfigPath, that.eslintConfigPath) && Objects.equals(eslintConfigJs, that.eslintConfigJs);
	}

	@Override
	public int hashCode() {
		return Objects.hash(eslintConfigPath, eslintConfigJs);
	}
}
