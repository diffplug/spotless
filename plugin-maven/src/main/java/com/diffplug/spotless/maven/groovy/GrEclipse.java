/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.spotless.maven.groovy;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.groovy.GrEclipseFormatterStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class GrEclipse implements FormatterStepFactory {

	@Parameter
	private String file;

	@Parameter
	private String version;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig stepConfig) {
		EclipseBasedStepBuilder grEclipseConfig = GrEclipseFormatterStep.createBuilder(stepConfig.getProvisioner());
		grEclipseConfig.setVersion(version == null ? GrEclipseFormatterStep.defaultVersion() : version);
		if (null != file) {
			File settingsFile = stepConfig.getFileLocator().locateFile(file);
			grEclipseConfig.setPreferences(Arrays.asList(settingsFile));
		}
		return grEclipseConfig.build();
	}
}
