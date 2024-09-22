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
package com.diffplug.spotless.maven.rdf;

import java.util.Map;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;
import com.diffplug.spotless.rdf.RdfFormatterConfig;
import com.diffplug.spotless.rdf.RdfFormatterStep;

public class RdfFormat implements FormatterStepFactory {
   @Parameter
   String turtleFormatterVersion = RdfFormatterStep.LATEST_TURTLE_FORMATTER_VERSION;

   @Parameter
   boolean failOnWarning = true;

   @Parameter
   boolean verify = true;

   @Parameter
   Map<String, String> turtle;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		RdfFormatterConfig formatterConfig = RdfFormatterConfig
			.builder()
				.failOnWarning(failOnWarning)
				.turtleFormatterVersion(turtleFormatterVersion)
				.verify(verify)
				.build();
		try {
			return RdfFormatterStep.create(formatterConfig,
				turtle, config.getProvisioner());
		} catch (Exception e) {
			throw new RuntimeException("Error creating RDF formatter step", e);
		}
	}
}
