/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.cli.subcommands.steps.generic;

import java.io.File;

import picocli.CommandLine;

@CommandLine.Command(name = "license-header", description = "Runs license header")
public class LicenseHeader extends SpotlessStepSubCommand {

	@CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
	LicenseHeaderOption licenseHeaderOption;

	static class LicenseHeaderOption {
		@CommandLine.Option(names = {"--header", "-H"}, required = true)
		String header;
		@CommandLine.Option(names = {"--header-file", "-f"}, required = true)
		File headerFile;
	}

	@Override
	public void prepare() {
		super.prepare();
		System.out.println(licenseHeaderOption.header != null ? "Header: " + licenseHeaderOption.header : "HeaderFile:" + licenseHeaderOption.headerFile);
	}
}
