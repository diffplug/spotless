/*
 * Copyright 2022-2025 DiffPlug
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
import java.io.Serial;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;

public class EslintTypescriptConfig extends EslintConfig {
	@Serial
	private static final long serialVersionUID = 2L;

	@SuppressWarnings("unused")
	private final FileSignature.Promised typescriptConfigPathSignature;

	public EslintTypescriptConfig(@Nullable File eslintConfigPath, @Nullable String eslintConfigJs, @Nullable File typescriptConfigPath) {
		super(eslintConfigPath, eslintConfigJs);
		this.typescriptConfigPathSignature = typescriptConfigPath != null ? FileSignature.promise(typescriptConfigPath) : null;
	}

	@Override
	public EslintConfig withEslintConfigPath(@Nullable File eslintConfigPath) {
		return new EslintTypescriptConfig(eslintConfigPath, this.getEslintConfigJs(), getTypescriptConfigPath());
	}

	@Nullable public File getTypescriptConfigPath() {
		return typescriptConfigPathSignature == null ? null : this.typescriptConfigPathSignature.get().getOnlyFile();
	}
}
