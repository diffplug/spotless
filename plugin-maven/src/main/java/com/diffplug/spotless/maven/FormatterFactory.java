/*
 * Copyright 2016-2024 DiffPlug
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

import static com.diffplug.spotless.maven.AbstractSpotlessMojo.RATCHETFROM_NONE;
import static java.util.Collections.emptySet;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.diffplug.common.collect.Sets;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.maven.generic.EclipseWtp;
import com.diffplug.spotless.maven.generic.EndWithNewline;
import com.diffplug.spotless.maven.generic.Indent;
import com.diffplug.spotless.maven.generic.Jsr223;
import com.diffplug.spotless.maven.generic.LicenseHeader;
import com.diffplug.spotless.maven.generic.NativeCmd;
import com.diffplug.spotless.maven.generic.Prettier;
import com.diffplug.spotless.maven.generic.Replace;
import com.diffplug.spotless.maven.generic.ReplaceRegex;
import com.diffplug.spotless.maven.generic.ToggleOffOn;
import com.diffplug.spotless.maven.generic.TrimTrailingWhitespace;

public abstract class FormatterFactory {
	@Parameter
	private String encoding;

	@Parameter
	private LineEnding lineEndings;

	/** Sentinel to distinguish between "don't ratchet this format" and "use spotless parent format". */
	private static final String RATCHETFROM_NOT_SET_AT_FORMAT_LEVEL = " not set at format level ";

	@Parameter
	private String ratchetFrom = RATCHETFROM_NOT_SET_AT_FORMAT_LEVEL;

	@Parameter
	private String[] includes;

	@Parameter
	private String[] excludes;

	private final List<FormatterStepFactory> stepFactories = new ArrayList<>();

	private ToggleOffOn toggle;

	public abstract Set<String> defaultIncludes(MavenProject project);

	public abstract String licenseHeaderDelimiter();

	public final Set<String> includes() {
		return includes == null ? emptySet() : Sets.newHashSet(includes);
	}

	public final Set<String> excludes() {
		return excludes == null ? emptySet() : Sets.newHashSet(excludes);
	}

	public final Formatter newFormatter(Supplier<Iterable<File>> filesToFormat, FormatterConfig config) {
		Charset formatterEncoding = encoding(config);
		LineEnding formatterLineEndings = lineEndings(config);
		LineEnding.Policy formatterLineEndingPolicy = formatterLineEndings.createPolicy(config.getFileLocator().getBaseDir(), filesToFormat);

		FormatterStepConfig stepConfig = stepConfig(formatterEncoding, config);
		List<FormatterStepFactory> factories = gatherStepFactories(config.getGlobalStepFactories(), stepFactories);

		List<FormatterStep> formatterSteps = factories.stream()
				.filter(Objects::nonNull) // all unrecognized steps from XML config appear as nulls in the list
				.map(factory -> factory.newFormatterStep(stepConfig))
				.collect(Collectors.toCollection(() -> new ArrayList<FormatterStep>()));
		if (toggle != null) {
			List<FormatterStep> formatterStepsBeforeToggle = formatterSteps;
			formatterSteps = List.of(toggle.createFence().preserveWithin(formatterStepsBeforeToggle));
		}

		String formatterName = this.getClass().getSimpleName();
		return Formatter.builder()
				.name(formatterName)
				.encoding(formatterEncoding)
				.lineEndingsPolicy(formatterLineEndingPolicy)
				.exceptionPolicy(new FormatExceptionPolicyStrict())
				.steps(formatterSteps)
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

	public final void addJsr223(Jsr223 jsr223) {
		addStepFactory(jsr223);
	}

	public final void addTrimTrailingWhitespace(TrimTrailingWhitespace trimTrailingWhitespace) {
		addStepFactory(trimTrailingWhitespace);
	}

	public final void addReplace(Replace replace) {
		addStepFactory(replace);
	}

	public final void addNativeCmd(NativeCmd nativeCmd) {
		addStepFactory(nativeCmd);
	}

	public final void addReplaceRegex(ReplaceRegex replaceRegex) {
		addStepFactory(replaceRegex);
	}

	public final void addEclipseWtp(EclipseWtp eclipseWtp) {
		addStepFactory(eclipseWtp);
	}

	public final void addPrettier(Prettier prettier) {
		addStepFactory(prettier);
	}

	public final void addToggleOffOn(ToggleOffOn toggle) {
		this.toggle = toggle;
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

	Optional<String> ratchetFrom(FormatterConfig config) {
		if (RATCHETFROM_NOT_SET_AT_FORMAT_LEVEL.equals(ratchetFrom)) {
			return config.getRatchetFrom();
		} else if (RATCHETFROM_NONE.equals(ratchetFrom)) {
			return Optional.empty();
		} else {
			return Optional.ofNullable(ratchetFrom);
		}
	}

	private FormatterStepConfig stepConfig(Charset encoding, FormatterConfig config) {
		return new FormatterStepConfig(encoding, licenseHeaderDelimiter(), ratchetFrom(config), config.getProvisioner(), config.getFileLocator(), config.getSpotlessSetLicenseHeaderYearsFromGitHistory());
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
