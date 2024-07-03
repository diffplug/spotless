/*
 * Copyright 2022-2024 DiffPlug
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
import java.util.Objects;

import javax.annotation.Nullable;

public class EslintTypescriptConfig extends EslintConfig {

	private static final long serialVersionUID = 7047648793633604218L;

	@SuppressWarnings("unused")
	private final RoundtrippableFile typescriptConfigPath;

	public EslintTypescriptConfig(@Nullable File eslintConfigPath, @Nullable String eslintConfigJs, @Nullable File typescriptConfigPath) {
		super(eslintConfigPath, eslintConfigJs);
		this.typescriptConfigPath = typescriptConfigPath != null ? new RoundtrippableFile(typescriptConfigPath) : null;
	}

	@Override
	public EslintConfig withEslintConfigPath(@Nullable File eslintConfigPath) {
		return new EslintTypescriptConfig(eslintConfigPath, this.getEslintConfigJs(), getTypescriptConfigPath());
	}

	@Nullable
	public File getTypescriptConfigPath() {
		return typescriptConfigPath == null ? null : this.typescriptConfigPath.file();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof EslintTypescriptConfig))
			return false;
		if (!super.equals(o))
			return false;
		EslintTypescriptConfig that = (EslintTypescriptConfig) o;
		return Objects.equals(typescriptConfigPath, that.typescriptConfigPath);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), typescriptConfigPath);
	}
}
