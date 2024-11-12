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
package com.diffplug.spotless.cli.steps;

import java.util.List;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.cli.core.SpotlessActionContext;
import com.diffplug.spotless.cli.help.OptionConstants;
import com.diffplug.spotless.java.GoogleJavaFormatStep;

import picocli.CommandLine;

@CommandLine.Command(name = "google-java-format", description = "Runs google java format")
public class GoogleJavaFormat extends SpotlessFormatterStep {

	@CommandLine.Option(names = {"--style", "-s"}, required = false, defaultValue = "GOOGLE", description = "The style to use for the google java format." + OptionConstants.VALID_VALUES_SUFFIX + OptionConstants.DEFAULT_VALUE_SUFFIX)
	Style style;

	@CommandLine.Option(names = {"--reflow-long-strings", "-r"}, required = false, defaultValue = "false", description = "Reflow long strings." + OptionConstants.DEFAULT_VALUE_SUFFIX)
	boolean reflowLongStrings;

	@CommandLine.Option(names = {"--reorder-imports", "-i"}, required = false, defaultValue = "false", description = "Reorder imports." + OptionConstants.DEFAULT_VALUE_SUFFIX)
	boolean reorderImports;

	@CommandLine.Option(names = {"--format-javadoc", "-j"}, required = false, defaultValue = "true", description = "Format javadoc." + OptionConstants.DEFAULT_VALUE_SUFFIX)
	boolean formatJavadoc;

	@Override
	public List<FormatterStep> prepareFormatterSteps(SpotlessActionContext context) {
		return List.of(GoogleJavaFormatStep.create(
				GoogleJavaFormatStep.defaultGroupArtifact(),
				GoogleJavaFormatStep.defaultVersion(),
				style.name(),
				context.provisioner(),
				reflowLongStrings,
				reorderImports,
				formatJavadoc));
	}

	public enum Style {
		AOSP, GOOGLE
	}
}
