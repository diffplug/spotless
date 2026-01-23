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

import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.EquoBasedStepBuilder;
import com.diffplug.spotless.extra.P2Provisioner;
import com.diffplug.spotless.extra.eclipse.EquoResourceHarness;

class EclipseJdtFormatterStepTest extends EquoResourceHarness {
	private static EquoBasedStepBuilder createBuilder() {
		return EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral(), P2Provisioner.createDefault());
	}

	public EclipseJdtFormatterStepTest() {
		super(createBuilder());
	}

	@ParameterizedTest
	@MethodSource
	void formatWithVersion(String version) throws Exception {
		harnessFor(version).test("test.java",
				"package p; class C{}",
				"package p;\nclass C {\n}");
	}

	private static Stream<String> formatWithVersion() {
		return Stream.of("4.9", EclipseJdtFormatterStep.defaultVersion());
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
