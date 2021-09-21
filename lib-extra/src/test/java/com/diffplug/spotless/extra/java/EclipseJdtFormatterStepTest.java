/*
 * Copyright 2016-2021 DiffPlug
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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.eclipse.EclipseResourceHarness;

class EclipseJdtFormatterStepTest extends EclipseResourceHarness {
	private final static String NON_SEMANTIC_ECLIPSE_VERSION = "4.7.3a";
	private final static Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support("Oldest Version").add(8, "4.6.1");
	private final static String INPUT = "package p; class C{}";
	private final static String EXPECTED = "package p;\nclass C {\n}";

	public EclipseJdtFormatterStepTest() {
		super(EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral()), INPUT, EXPECTED);
	}

	@ParameterizedTest
	@MethodSource
	void formatWithVersion(String version) throws Exception {
		assertFormatted(version);
	}

	private static Stream<String> formatWithVersion() {
		return Stream.of(NON_SEMANTIC_ECLIPSE_VERSION, JVM_SUPPORT.getRecommendedFormatterVersion(), EclipseJdtFormatterStep.defaultVersion());
	}
}
