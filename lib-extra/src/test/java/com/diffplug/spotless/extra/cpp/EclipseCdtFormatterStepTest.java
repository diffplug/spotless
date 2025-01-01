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
package com.diffplug.spotless.extra.cpp;

import static org.junit.jupiter.api.condition.JRE.JAVA_17;

import java.util.stream.Stream;

import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.eclipse.EquoResourceHarness;

class EclipseCdtFormatterStepTest extends EquoResourceHarness {
	public EclipseCdtFormatterStepTest() {
		super(EclipseCdtFormatterStep.createBuilder(TestProvisioner.mavenCentral()));
	}

	@ParameterizedTest
	@MethodSource
	@EnabledForJreRange(min = JAVA_17)
	void formatWithVersion(String version) throws Exception {
		harnessFor(version).test("main.c",
				"#include <a.h>;\nint main(int argc,   \nchar *argv[]) {}",
				"#include <a.h>;\nint main(int argc, char *argv[]) {\n}\n");
	}

	private static Stream<String> formatWithVersion() {
		return Stream.of("11.0", "11.6", EclipseCdtFormatterStep.defaultVersion());
	}
}
