/*
 * Copyright 2023-2026 DiffPlug
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

import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.java.CleanthatJavaStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class CleanthatJava implements FormatterStepFactory {
	@Parameter
	private String groupArtifact;

	@Parameter
	private String version;

	/**
	 * Optional: override the version of JavaParser pulled in as a transitive dependency of Cleanthat. Advanced use-case:
	 * declaring a version here appends {@code com.github.javaparser:javaparser-symbol-solver-core:<javaparserVersion>}
	 * to the resolved classpath, so standard Maven resolution rules (newest wins) apply. At your own risk.
	 */
	@Parameter
	private String javaparserVersion;

	// https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#source
	@Parameter(property = "maven.compiler.source")
	private String sourceJdk = CleanthatJavaStep.defaultSourceJdk();

	@Parameter
	private List<String> mutators = CleanthatJavaStep.defaultMutators();

	@Parameter
	private List<String> excludedMutators = CleanthatJavaStep.defaultExcludedMutators();

	@Parameter
	private boolean includeDraft = CleanthatJavaStep.defaultIncludeDraft();

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		String groupArtifact = this.groupArtifact != null ? this.groupArtifact : CleanthatJavaStep.defaultGroupArtifact();
		String version = this.version != null ? this.version : CleanthatJavaStep.defaultVersion();

		return CleanthatJavaStep.create(groupArtifact, version, javaparserVersion, sourceJdk, mutators, excludedMutators, includeDraft, config.getProvisioner());
	}
}
