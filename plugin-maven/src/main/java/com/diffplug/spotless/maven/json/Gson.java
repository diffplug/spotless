/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.maven.json;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.json.gson.GsonConfig;
import com.diffplug.spotless.json.gson.GsonStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class Gson implements FormatterStepFactory {
	private static final String DEFAULT_GSON_VERSION = "2.10.1";

	@Parameter
	int indentSpaces = Json.DEFAULT_INDENTATION;

	@Parameter
	boolean sortByKeys = false;

	@Parameter
	boolean escapeHtml = false;

	@Parameter
	String version = DEFAULT_GSON_VERSION;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig stepConfig) {
		var indentSpaces = this.indentSpaces;
		return GsonStep.create(new GsonConfig(sortByKeys, escapeHtml, indentSpaces, version), stepConfig.getProvisioner());
	}
}
