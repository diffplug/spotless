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
package com.diffplug.spotless.maven.java;

import static com.diffplug.spotless.extra.java.EclipseFormatterStep.defaultVersion;
import static java.util.Collections.singleton;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.extra.java.EclipseFormatterStep;
import com.diffplug.spotless.maven.FormatterFactory;
import com.diffplug.spotless.maven.FormatterStepFactory;
import com.diffplug.spotless.maven.MojoConfig;

public class Eclipse implements FormatterStepFactory {

	@Parameter(required = true)
	private File file;

	@Parameter
	private String version;

	@Override
	public FormatterStep newFormatterStep(FormatterFactory parent, MojoConfig mojoConfig) {
		String formatterVersion = version == null ? defaultVersion() : version;
		Set<File> settingsFiles = singleton(file);
		return EclipseFormatterStep.create(formatterVersion, settingsFiles, mojoConfig.getProvisioner());
	}
}
