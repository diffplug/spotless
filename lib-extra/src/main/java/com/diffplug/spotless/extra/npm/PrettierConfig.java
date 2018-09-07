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
package com.diffplug.spotless.extra.npm;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.ThrowingEx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class PrettierConfig implements Serializable {

	private static final long serialVersionUID = -8709340269833126583L;

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	private final transient File prettierConfigPath;

	@SuppressWarnings("unused")
	private final FileSignature prettierConfigPathSignature;

	private final TreeMap<String, Object> options;

	public PrettierConfig(File prettierConfigPath, Map<String, Object> options) {
		try {
			this.prettierConfigPath = prettierConfigPath;
			this.prettierConfigPathSignature = FileSignature.signAsList(this.prettierConfigPath);
			this.options = options == null ? new TreeMap<>() : new TreeMap<>(options);
		} catch (IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	public File getPrettierConfigPath() {
		return prettierConfigPath;
	}

	public Map<String, Object> getOptions() {
		return new TreeMap<>(this.options);
	}
}
