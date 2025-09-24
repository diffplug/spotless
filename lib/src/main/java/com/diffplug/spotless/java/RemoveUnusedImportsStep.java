/*
 * Copyright 2016-2025 DiffPlug
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

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;

/** Uses google-java-format or cleanthat.UnnecessaryImport, but only to remove unused imports. */
public class RemoveUnusedImportsStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	static final String NAME = "removeUnusedImports";

	static final String GJF = "google-java-format";
	static final String CLEANTHAT = "cleanthat-javaparser-unnecessaryimport";

	// https://github.com/solven-eu/cleanthat/blob/master/java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessaryImport.java
	private static final String CLEANTHAT_MUTATOR = "UnnecessaryImport";

	// prevent direct instantiation
	private RemoveUnusedImportsStep() {}

	public static String defaultFormatter() {
		return GJF;
	}

	public static FormatterStep create(Provisioner provisioner) {
		// The default importRemover is GJF
		return create(GJF, provisioner);
	}

	public static FormatterStep create(String unusedImportRemover, Provisioner provisioner) {
		Objects.requireNonNull(provisioner, "provisioner");
		switch (unusedImportRemover) {
		case GJF:
			return GoogleJavaFormatStep.createRemoveUnusedImportsOnly(provisioner);
		case CLEANTHAT:
			return CleanthatJavaStep.createWithStepName(NAME, CleanthatJavaStep.defaultGroupArtifact(), CleanthatJavaStep.defaultVersion(), "99.9", List.of(CLEANTHAT_MUTATOR), List.of(), false, provisioner);
		default:
			throw new IllegalArgumentException("Invalid unusedImportRemover: " + unusedImportRemover);
		}
	}
}
