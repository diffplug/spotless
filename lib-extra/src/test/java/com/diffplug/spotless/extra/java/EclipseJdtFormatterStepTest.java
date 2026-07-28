/*
 * Copyright 2016-2026 DiffPlug
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
package com.diffplug.spotless.extra.java;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestP2Provisioner;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.EquoBasedStepBuilder;
import com.diffplug.spotless.extra.P2Provisioner;
import com.diffplug.spotless.extra.eclipse.EquoResourceHarness;

class EclipseJdtFormatterStepTest extends EquoResourceHarness {

	/**
	 * Embedded lockfile coverage includes both dependency styles:
	 * <ul>
	 * <li>Range-based Maven POM dependencies: 4.9, 4.11, and 4.25</li>
	 * <li>Exact Maven POM dependencies: 4.26, 4.39, and the default version</li>
	 * </ul>
	 * The cutoff aligns with
	 * <a href="https://github.com/eclipse-platform/eclipse.platform.releng/issues/135">eclipse-platform/eclipse.platform.releng#135</a>,
	 * which switched Maven dependency mapping from OSGi ranges to resolved concrete versions.
	 */
	private static final List<String> EMBEDDED_LOCKFILE_VERSIONS = List.of("4.9", "4.11", "4.25", "4.26", "4.39", EclipseJdtFormatterStep.defaultVersion());

	private static EquoBasedStepBuilder createBuilder() {
		return EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral(), TestP2Provisioner.defaultProvisioner());
	}

	public EclipseJdtFormatterStepTest() {
		super(createBuilder());
	}

	@ParameterizedTest
	@FieldSource("EMBEDDED_LOCKFILE_VERSIONS")
	void formatWithVersion(String version) throws Exception {
		harnessFor(version).test("test.java",
				"package p; class C{}",
				"package p;\nclass C {\n}");
	}

	@ParameterizedTest
	@FieldSource("EMBEDDED_LOCKFILE_VERSIONS")
	void embeddedLockfileVersionsDoNotUseP2(String version) {
		P2Provisioner p2Provisioner = mock();
		EclipseJdtFormatterStep.Builder builder = EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral(), p2Provisioner);
		builder.setVersion(version);
		StepHarnessWithFile.forStep(this, builder.build()).test(
				"test.java",
				"package p; class C{}",
				"package p;\nclass C {\n}");
		verifyNoInteractions(p2Provisioner);
	}

	/** New format interface requires source file information to distinguish module-info from compilation unit */
	@Nested
	class NewFormatInterface extends EquoResourceHarness {
		public NewFormatInterface() {
			super(createBuilder());
		}

		@Test
		void formatModuleInfo() throws Exception {
			harnessFor("4.11", createTestFile("java/eclipse/ModuleInfo.prefs"))
					.testResource("module-info.java", "java/eclipse/ModuleInfoUnformatted.test", "java/eclipse/ModuleInfoFormatted.test");
		}
	}
}
