/*
 * Copyright 2022-2023 DiffPlug
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

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.ThrowingEx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class EslintTypescriptConfig extends EslintConfig {

	private static final long serialVersionUID = -126864670181617006L;

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	@Nullable
	private final transient File typescriptConfigPath;

	@SuppressWarnings("unused")
	private final FileSignature typescriptConfigPathSignature;

	public EslintTypescriptConfig(@Nullable File eslintConfigPath, @Nullable String eslintConfigJs, @Nullable File typescriptConfigPath) {
		super(eslintConfigPath, eslintConfigJs);
		try {
			this.typescriptConfigPath = typescriptConfigPath;
			this.typescriptConfigPathSignature = typescriptConfigPath != null ? FileSignature.signAsList(this.typescriptConfigPath) : FileSignature.signAsList();
		} catch (IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	@Override
	public EslintConfig withEslintConfigPath(@Nullable File eslintConfigPath) {
		return new EslintTypescriptConfig(eslintConfigPath, this.getEslintConfigJs(), this.typescriptConfigPath);
	}

	@Nullable
	public File getTypescriptConfigPath() {
		return typescriptConfigPath;
	}
}
