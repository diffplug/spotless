/*
 * Copyright 2016-2026 DiffPlug
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

import static java.util.Collections.unmodifiableList;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.LintSuppression;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.P2Provisioner;

public class FormatterConfig {

	private final String encoding;
	private final LineEnding lineEndings;
	private final Optional<String> ratchetFrom;
	private final Provisioner provisioner;
	private final P2Provisioner p2Provisioner;
	private final FileLocator fileLocator;
	private final List<FormatterStepFactory> globalStepFactories;
	private final Optional<String> spotlessSetLicenseHeaderYearsFromGitHistory;
	private final List<LintSuppression> lintSuppressions;

	public FormatterConfig(File baseDir, String encoding, LineEnding lineEndings, Optional<String> ratchetFrom, Provisioner provisioner,
			P2Provisioner p2Provisioner, FileLocator fileLocator, List<FormatterStepFactory> globalStepFactories, Optional<String> spotlessSetLicenseHeaderYearsFromGitHistory, List<LintSuppression> lintSuppressions) {
		this.encoding = encoding;
		this.lineEndings = lineEndings;
		this.ratchetFrom = ratchetFrom;
		this.provisioner = provisioner;
		this.p2Provisioner = p2Provisioner;
		this.fileLocator = fileLocator;
		this.globalStepFactories = globalStepFactories;
		this.spotlessSetLicenseHeaderYearsFromGitHistory = spotlessSetLicenseHeaderYearsFromGitHistory;
		this.lintSuppressions = lintSuppressions;
	}

	public String getEncoding() {
		return encoding;
	}

	public LineEnding getLineEndings() {
		return lineEndings;
	}

	public Optional<String> getRatchetFrom() {
		return ratchetFrom;
	}

	public Provisioner getProvisioner() {
		return provisioner;
	}

	public P2Provisioner getP2Provisioner() {
		return p2Provisioner;
	}

	public List<FormatterStepFactory> getGlobalStepFactories() {
		return unmodifiableList(globalStepFactories);
	}

	public Optional<String> getSpotlessSetLicenseHeaderYearsFromGitHistory() {
		return spotlessSetLicenseHeaderYearsFromGitHistory;
	}

	public FileLocator getFileLocator() {
		return fileLocator;
	}

	public List<LintSuppression> getLintSuppressions() {
		return unmodifiableList(lintSuppressions);
	}
}
