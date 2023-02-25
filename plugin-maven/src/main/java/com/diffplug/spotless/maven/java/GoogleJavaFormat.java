/*
 * Copyright 2016-2021 DiffPlug
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

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.java.GoogleJavaFormatStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class GoogleJavaFormat implements FormatterStepFactory {
	@Parameter
	private String groupArtifact;

	@Parameter
	private String version;

	@Parameter
	private String style;

	@Parameter
	private Boolean reflowLongStrings;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		String groupArtifact = this.groupArtifact != null ? this.groupArtifact : GoogleJavaFormatStep.defaultGroupArtifact();
		String version = this.version != null ? this.version : GoogleJavaFormatStep.defaultVersion();
		String style = this.style != null ? this.style : GoogleJavaFormatStep.defaultStyle();
		boolean reflowLongStrings = this.reflowLongStrings != null ? this.reflowLongStrings : GoogleJavaFormatStep.defaultReflowLongStrings();
		return GoogleJavaFormatStep.create(groupArtifact, version, style, config.getProvisioner(), reflowLongStrings);
	}
}
