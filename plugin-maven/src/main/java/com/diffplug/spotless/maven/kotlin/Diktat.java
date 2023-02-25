/*
 * Copyright 2021-2022 DiffPlug
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
package com.diffplug.spotless.maven.kotlin;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.kotlin.DiktatStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class Diktat implements FormatterStepFactory {

	@Parameter
	private String version;

	@Parameter
	private String configFile;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig stepConfig) {
		FileSignature config = null;
		if (configFile != null) {
			config = ThrowingEx.get(() -> FileSignature.signAsList(stepConfig.getFileLocator().locateFile(configFile)));
		}
		String diktatVersion = version != null ? version : DiktatStep.defaultVersionDiktat();
		return DiktatStep.create(diktatVersion, stepConfig.getProvisioner(), config);
	}
}
