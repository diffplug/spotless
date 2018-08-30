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
package com.diffplug.spotless.maven;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.common.collect.Sets;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.maven.generic.*;

public abstract class FormatterFactory {
	@Parameter
	private String encoding;

	@Parameter
	private LineEnding lineEndings;

	@Parameter
	private String[] includes;

	@Parameter
	private String[] excludes;

	private final List<FormatterStepFactory> stepFactories = new ArrayList<>();

	public abstract Set<String> defaultIncludes();

	public abstract String licenseHeaderDelimiter();

	public final Set<String> includes() {
		return includes == null ? emptySet() : Sets.newHashSet(includes);
	}

	public final Set<String> excludes() {
		return excludes == null ? emptySet() : Sets.newHashSet(excludes);
	}

	public final Formatter newFormatter(List<File> filesToFormat, FormatterConfig config) {
		Charset formatterEncoding = encoding(config);
		LineEnding formatterLineEndings = lineEndings(config);
		LineEnding.Policy formatterLineEndingPolicy = formatterLineEndings.createPolicy(config.getBaseDir(), () -> filesToFormat);

		FormatterStepConfig stepConfig = stepConfig(formatterEncoding, config);
		List<FormatterStepFactory> factories = gatherStepFactories(config.getGlobalStepFactories(), stepFactories);

		List<FormatterStep> formatterSteps = factories.stream()
				.filter(Objects::nonNull) // all unrecognized steps from XML config appear as nulls in the list
				.map(factory -> factory.newFormatterStep(stepConfig))
				.collect(toList());

		return Formatter.builder()
				.encoding(formatterEncoding)
				.lineEndingsPolicy(formatterLineEndingPolicy)
				.exceptionPolicy(new FormatExceptionPolicyStrict())
				.steps(formatterSteps)
				.rootDir(config.getBaseDir().toPath())
				.build();
	}

	public final void addLicenseHeader(LicenseHeader licenseHeader) {
		addStepFactory(licenseHeader);
	}

	public final void addEndWithNewline(EndWithNewline endWithNewline) {
		addStepFactory(endWithNewline);
	}

	public final void addIndent(Indent indent) {
		addStepFactory(indent);
	}

	public final void addTrimTrailingWhitespace(TrimTrailingWhitespace trimTrailingWhitespace) {
		addStepFactory(trimTrailingWhitespace);
	}

	public final void addReplace(Replace replace) {
		addStepFactory(replace);
	}

	public final void addReplaceRegex(ReplaceRegex replaceRegex) {
		addStepFactory(replaceRegex);
	}

	protected final void addStepFactory(FormatterStepFactory stepFactory) {
		Objects.requireNonNull(stepFactory);
		stepFactories.add(stepFactory);
	}

	private Charset encoding(FormatterConfig config) {
		return Charset.forName(encoding == null ? config.getEncoding() : encoding);
	}

	private LineEnding lineEndings(FormatterConfig config) {
		return lineEndings == null ? config.getLineEndings() : lineEndings;
	}

	private FormatterStepConfig stepConfig(Charset encoding, FormatterConfig config) {
		return new FormatterStepConfig(encoding, licenseHeaderDelimiter(), config.getProvisioner(), config.getFileLocator());
	}

	private static List<FormatterStepFactory> gatherStepFactories(List<FormatterStepFactory> allGlobal, List<FormatterStepFactory> allConfigured) {
		List<FormatterStepFactory> result = new ArrayList<>();
		for (FormatterStepFactory global : allGlobal) {
			if (!formatterStepOverriden(global, allConfigured)) {
				result.add(global);
			}
		}
		result.addAll(allConfigured);
		return result;
	}

	private static boolean formatterStepOverriden(FormatterStepFactory global, List<FormatterStepFactory> allConfigured) {
		return allConfigured.stream()
				.anyMatch(configured -> configured.getClass() == global.getClass());
	}
}
