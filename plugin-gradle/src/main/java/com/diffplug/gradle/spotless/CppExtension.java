/*
 * Copyright 2016-2020 DiffPlug
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
package com.diffplug.gradle.spotless;

import static com.diffplug.gradle.spotless.PluginGradlePreconditions.requireElementsNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.Project;

import com.diffplug.spotless.cpp.CppDefaults;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.cpp.EclipseCdtFormatterStep;

public class CppExtension extends FormatExtension implements HasBuiltinDelimiterForLicense {
	static final String NAME = "cpp";

	public CppExtension(SpotlessExtensionBase spotless) {
		super(spotless);
	}

	public EclipseConfig eclipseCdt() {
		return new EclipseConfig(EclipseCdtFormatterStep.defaultVersion());
	}

	public EclipseConfig eclipseCdt(String version) {
		return new EclipseConfig(version);
	}

	/** Use {@link #eclipseCdt} instead. */
	@Deprecated
	public EclipseConfig eclipse() {
		getProject().getLogger().warn("Spotless: in the `cpp { }` block, use `eclipseCdt()` instead of `eclipse()`");
		return new EclipseConfig(EclipseCdtFormatterStep.defaultVersion());
	}

	/** Use {@link #eclipseCdt} instead. */
	@Deprecated
	public EclipseConfig eclipse(String version) {
		getProject().getLogger().warn("Spotless: in the `cpp { }` block, use `eclipseCdt('" + version + "')` instead of `eclipse('" + version + "')`");
		return new EclipseConfig(version);
	}

	public class EclipseConfig {
		private final EclipseBasedStepBuilder builder;

		EclipseConfig(String version) {
			builder = EclipseCdtFormatterStep.createBuilder(provisioner());
			builder.setVersion(version);
			addStep(builder.build());
		}

		public void configFile(Object... configFiles) {
			requireElementsNonNull(configFiles);
			Project project = getProject();
			builder.setPreferences(project.files(configFiles).getFiles());
			replaceStep(builder.build());
		}
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			/*
			 * The org.gradle.language.c and org.gradle.language.cpp source sets are seldom used.
			 * Most Gradle C/C++ use external CMake builds (so the source location is unknown to Gradle).
			 * Hence file extension based filtering is used in line with the org.eclipse.core.contenttype.contentTypes<
			 * defined by the CDT plugin.
			 */
			noDefaultTarget();
			target(FILE_FILTER.toArray());
		}
		super.setupTask(task);
	}

	/**
	 * Filter based on Eclipse-CDT <code>org.eclipse.core.contenttype.contentTypes</code>
	 * extension <code>cSource</code>, <code>cHeader</code>, <code>cxxSource</code> and <code>cxxHeader</code>.
	 */
	@Deprecated
	private static final List<String> FILE_FILTER = Collections.unmodifiableList(
			Arrays.asList("c", "h", "C", "cpp", "cxx", "cc", "c++", "h", "hpp", "hh", "hxx", "inc")
					.stream().map(s -> {
						return "**/*." + s;
					}).collect(Collectors.toList()));

	@Override
	public LicenseHeaderConfig licenseHeader(String licenseHeader) {
		return licenseHeader(licenseHeader, CppDefaults.DELIMITER_EXPR);
	}

	@Override
	public LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile) {
		return licenseHeaderFile(licenseHeaderFile, CppDefaults.DELIMITER_EXPR);
	}
}
