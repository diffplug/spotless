/*
 * Copyright 2016-2025 DiffPlug
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
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;

public class PrettierConfig implements Serializable {

	private static final long serialVersionUID = -8709340269833126583L;

	private final FileSignature.Promised prettierConfigPathSignature;

	private final TreeMap<String, Object> options;

	private final Boolean editorconfig;

	public PrettierConfig(@Nullable File prettierConfigPath, @Nullable Map<String, Object> options, @Nullable Boolean editorconfig) {
		this.prettierConfigPathSignature = prettierConfigPath == null ? null : FileSignature.promise(prettierConfigPath);
		this.options = options == null ? new TreeMap<>() : new TreeMap<>(options);
		this.editorconfig = editorconfig;
	}

	@Nullable public File getPrettierConfigPath() {
		return prettierConfigPathSignature == null ? null : prettierConfigPathSignature.get().getOnlyFile();
	}

	public Map<String, Object> getOptions() {
		return new TreeMap<>(this.options);
	}

	@Nullable public Boolean getEditorconfig() {
		return editorconfig;
	}
}
