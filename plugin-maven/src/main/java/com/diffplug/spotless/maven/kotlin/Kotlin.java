/*
 * Copyright 2016-2023 DiffPlug
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

import static com.diffplug.spotless.kotlin.KotlinConstants.LICENSE_HEADER_DELIMITER;

import java.util.Set;

import org.apache.maven.project.MavenProject;

import com.diffplug.common.collect.ImmutableSet;
import com.diffplug.spotless.maven.FormatterFactory;

public class Kotlin extends FormatterFactory {

	private static final Set<String> DEFAULT_INCLUDES = ImmutableSet.of("src/main/kotlin/**/*.kt", "src/test/kotlin/**/*.kt");

	@Override
	public Set<String> defaultIncludes(MavenProject project) {
		return DEFAULT_INCLUDES;
	}

	@Override
	public String licenseHeaderDelimiter() {
		return LICENSE_HEADER_DELIMITER;
	}

	public void addKtlint(Ktlint ktlint) {
		addStepFactory(ktlint);
	}

	public void addKtfmt(Ktfmt ktfmt) {
		addStepFactory(ktfmt);
	}

	public void addDiktat(Diktat diktat) {
		addStepFactory(diktat);
	}
}
