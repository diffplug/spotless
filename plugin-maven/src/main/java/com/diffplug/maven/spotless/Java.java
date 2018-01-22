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
package com.diffplug.maven.spotless;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;

public class Java implements FormatterFactory {

	private static final String EXTENSION = "java";

	@Parameter
	private String encoding;

	@Parameter
	private LineEnding lineEndings;

	@Parameter
	private List<FormatterStepFactory> steps;

	@Override
	public String fileExtension() {
		return EXTENSION;
	}

	@Override
	public Formatter newFormatter(List<File> filesToFormat, MojoConfig mojoConfig) {
		Charset formatterEncoding = encoding == null ? Charset.forName(mojoConfig.getEncoding()) : Charset.forName(encoding);
		LineEnding formatterLineEndings = lineEndings == null ? mojoConfig.getLineEndings() : lineEndings;
		LineEnding.Policy formatterLineEndingPolicy = formatterLineEndings.createPolicy(mojoConfig.getBaseDir(), () -> filesToFormat);

		List<FormatterStep> formatterSteps = steps.stream()
				.map(factory -> factory.newFormatterStep(mojoConfig))
				.collect(toList());

		return Formatter.builder()
				.encoding(formatterEncoding)
				.lineEndingsPolicy(formatterLineEndingPolicy)
				.exceptionPolicy(new FormatExceptionPolicyStrict())
				.steps(formatterSteps)
				.rootDir(mojoConfig.getBaseDir().toPath())
				.build();
	}
}
