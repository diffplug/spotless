/*
 * Copyright 2020-2023 DiffPlug
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NpmPathResolver {

	private final File explicitNpmExecutable;

	private final File explicitNodeExecutable;

	private final File explicitNpmrcFile;

	private final List<File> additionalNpmrcLocations;

	public NpmPathResolver(File explicitNpmExecutable, File explicitNodeExecutable, File explicitNpmrcFile, List<File> additionalNpmrcLocations) {
		this.explicitNpmExecutable = explicitNpmExecutable;
		this.explicitNodeExecutable = explicitNodeExecutable;
		this.explicitNpmrcFile = explicitNpmrcFile;
		this.additionalNpmrcLocations = Collections.unmodifiableList(new ArrayList<>(additionalNpmrcLocations));
	}

	public File resolveNpmExecutable() {
		return Optional.ofNullable(this.explicitNpmExecutable)
				.orElseGet(() -> NpmExecutableResolver.tryFind()
						.orElseThrow(() -> new IllegalStateException("Can't automatically determine npm executable and none was specifically supplied!\n\n" + NpmExecutableResolver.explainMessage())));
	}

	public File resolveNodeExecutable() {
		return Optional.ofNullable(this.explicitNodeExecutable)
				.orElseGet(() -> NodeExecutableResolver.tryFindNextTo(resolveNpmExecutable())
						.orElseThrow(() -> new IllegalStateException("Can't automatically determine node executable and none was specifically supplied!\n\n" + NpmExecutableResolver.explainMessage())));
	}

	public String resolveNpmrcContent() {
		File npmrcFile = Optional.ofNullable(this.explicitNpmrcFile)
				.orElseGet(() -> new NpmrcResolver(additionalNpmrcLocations).tryFind()
						.orElse(null));
		if (npmrcFile != null) {
			return NpmResourceHelper.readUtf8StringFromFile(npmrcFile);
		}
		return null;
	}

}
