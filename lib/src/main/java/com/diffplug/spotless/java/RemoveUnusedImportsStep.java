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
package com.diffplug.spotless.java;

import java.util.Arrays;
import java.util.Objects;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;

/** Uses google-java-format or cleanthat.UnnecessaryImport, but only to remove unused imports. */
public class RemoveUnusedImportsStep {
	static final String NAME = "removeUnusedImports";

	static final String GJF = "google-java-format";
	static final String CLEANTHAT = "cleanthat-javaparser-unnecessaryimport";

	// https://github.com/solven-eu/cleanthat/blob/master/java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessaryImport.java
	private static final String CLEANTHAT_MUTATOR = "UnnecessaryImport";

	// prevent direct instantiation
	private RemoveUnusedImportsStep() {}

	public static final String defaultFormatter() {
		return GJF;
	}

	public static FormatterStep create(Provisioner provisioner) {
		// The default importRemover is GJF
		return create(GJF, provisioner);
	}

	public static FormatterStep create(String unusedImportRemover, Provisioner provisioner) {
		Objects.requireNonNull(provisioner, "provisioner");

		if (GJF.equals(unusedImportRemover)) {
			return FormatterStep.createLazy(NAME,
					() -> new GoogleJavaFormatStep.State(NAME, GoogleJavaFormatStep.defaultVersion(), provisioner),
					GoogleJavaFormatStep.State::createRemoveUnusedImportsOnly);
		} else if (CLEANTHAT.equals(unusedImportRemover)) {
			return FormatterStep.createLazy(NAME,
					() -> new CleanthatJavaStep.State(NAME, CleanthatJavaStep.defaultGroupArtifact(), CleanthatJavaStep.defaultVersion(), "99.9", Arrays.asList(CLEANTHAT_MUTATOR), Arrays.asList(), false, provisioner),
					CleanthatJavaStep.State::createFormat);
		} else {
			throw new IllegalArgumentException("Invalid unusedImportRemover: " + unusedImportRemover);
		}
	}
}
