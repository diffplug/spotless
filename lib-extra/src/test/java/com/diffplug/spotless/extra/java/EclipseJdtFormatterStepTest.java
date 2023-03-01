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
package com.diffplug.spotless.extra.java;

import java.io.File;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.EquoBasedStepBuilder;
import com.diffplug.spotless.extra.eclipse.EquoResourceHarness;

class EclipseJdtFormatterStepTest extends EquoResourceHarness {
	private final static Jvm.Support<String> OLDEST_FOR_JVM = Jvm.<String> support("Oldest Version").add(8, "4.8").add(11, "4.17");
	private final static String INPUT = "package p; class C{}";
	private final static String EXPECTED = "package p;\nclass C {\n}";

	private static EquoBasedStepBuilder createBuilder() {
		return EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral());
	}

	public EclipseJdtFormatterStepTest() {
		super(createBuilder(), INPUT, EXPECTED);
	}

	@ParameterizedTest
	@MethodSource
	void formatWithVersion(String version) throws Exception {
		assertFormatted(version);
	}

	private static Stream<String> formatWithVersion() {
		return Stream.of(OLDEST_FOR_JVM.getRecommendedFormatterVersion(), EclipseJdtFormatterStep.defaultVersion());
	}

	/** New format interface requires source file information to distinguish module-info from compilation unit */
	@Nested
	class NewFormatInterface extends EquoResourceHarness {
		public NewFormatInterface() {
			super(createBuilder(), "module-info.java", getTestResource("java/eclipse/ModuleInfoUnformatted.test"), getTestResource("java/eclipse/ModuleInfoFormatted.test"));
		}

		@Test
		void formatModuleInfo() throws Exception {
			File settingsFile = createTestFile("java/eclipse/ModuleInfo.prefs");
			assertFormatted(OLDEST_FOR_JVM.getRecommendedFormatterVersion(), settingsFile);
		}
	}
}
