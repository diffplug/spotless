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

import java.nio.charset.Charset;

import com.diffplug.spotless.Provisioner;

public class FormatterStepConfig {

	private final Charset encoding;
	private final String licenseHeaderDelimiter;
	private final Provisioner provisioner;
	private final FileLocator fileLocator;

	public FormatterStepConfig(Charset encoding, String licenseHeaderDelimiter, Provisioner provisioner, FileLocator fileLocator) {
		this.encoding = encoding;
		this.licenseHeaderDelimiter = licenseHeaderDelimiter;
		this.provisioner = provisioner;
		this.fileLocator = fileLocator;
	}

	public Charset getEncoding() {
		return encoding;
	}

	public String getLicenseHeaderDelimiter() {
		return licenseHeaderDelimiter;
	}

	public Provisioner getProvisioner() {
		return provisioner;
	}

	public FileLocator getFileLocator() {
		return fileLocator;
	}
}
