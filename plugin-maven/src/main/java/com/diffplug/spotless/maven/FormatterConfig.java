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
package com.diffplug.spotless.maven;

import java.io.File;

import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Provisioner;

public class FormatterConfig {

	private final File baseDir;
	private final String encoding;
	private final LineEnding lineEndings;
	private final Provisioner provisioner;

	public FormatterConfig(File baseDir, String encoding, LineEnding lineEndings, Provisioner provisioner) {
		this.baseDir = baseDir;
		this.encoding = encoding;
		this.lineEndings = lineEndings;
		this.provisioner = provisioner;
	}

	public File getBaseDir() {
		return baseDir;
	}

	public String getEncoding() {
		return encoding;
	}

	public LineEnding getLineEndings() {
		return lineEndings;
	}

	public Provisioner getProvisioner() {
		return provisioner;
	}
}
