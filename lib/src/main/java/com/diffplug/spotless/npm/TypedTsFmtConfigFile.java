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
package com.diffplug.spotless.npm;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.ThrowingEx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class TypedTsFmtConfigFile implements Serializable {

	private static final long serialVersionUID = -4442310349275775501L;

	private final TsConfigFileType configFileType;

	private final File configFile;

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	@SuppressWarnings("unused")
	private final FileSignature configFileSignature;

	public TypedTsFmtConfigFile(TsConfigFileType configFileType, File configFile) {
		this.configFileType = requireNonNull(configFileType);
		this.configFile = requireNonNull(configFile);
		try {
			this.configFileSignature = FileSignature.signAsList(configFile);
		} catch (IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	TsConfigFileType configFileType() {
		return configFileType;
	}

	File configFile() {
		return configFile;
	}

	String configFileEnabledOptionName() {
		return this.configFileType.name().toLowerCase(Locale.ROOT);
	}

	String configFileOptionName() {
		return this.configFileEnabledOptionName() + "File";
	}

	String absolutePath() {
		return this.configFile.getAbsolutePath();
	}

	static TypedTsFmtConfigFile named(String name, File file) {
		return new TypedTsFmtConfigFile(TsConfigFileType.forNameIgnoreCase(name), file);
	}

}
