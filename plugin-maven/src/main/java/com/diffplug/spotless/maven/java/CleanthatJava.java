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

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.java.CleanthatStepFactory;
import com.diffplug.spotless.java.GoogleJavaFormatStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

public class CleanthatJava implements FormatterStepFactory {
	@Parameter
	private String groupArtifact;

	@Parameter
	private String version;

	@Parameter
	private List<String> mutators;

	@Parameter
	private List<String> excludedMutators;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		String groupArtifact = this.groupArtifact != null ? this.groupArtifact : CleanthatStepFactory.defaultGroupArtifact();
		String version = this.version != null ? this.version : CleanthatStepFactory.defaultVersion();

		JavaRe
		boolean reflowLongStrings = this.reflowLongStrings != null ? this.reflowLongStrings : GoogleJavaFormatStep.defaultReflowLongStrings();
		return CleanthatStepFactory.create(groupArtifact, version, style, config.getProvisioner(), reflowLongStrings);
	}

	private static String defaultVersion() {
	}
}
