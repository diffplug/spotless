/*
 * Copyright 2016 DiffPlug
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

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.eclipse.EclipseCommonTests;

public class EclipseJdtFormatterStepTest extends EclipseCommonTests {

	@Override
	protected String[] getSupportedVersions() {
		return new String[]{"4.6.1", "4.6.2", "4.6.3", "4.7.0", "4.7.1", "4.7.2", "4.7.3a", "4.8.0", "4.9.0", "4.10.0",
				"4.11.0"};
	}

	@Override
	protected String getTestInput(String version) {
		return "package p; class C{}";
	}

	@Override
	protected String getTestExpectation(String version) {
		return "package p;\nclass C {\n}";
	}

	@Override
	protected FormatterStep createStep(String version) {
		EclipseBasedStepBuilder builder = EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral());
		builder.setVersion(version);
		return builder.build();
	}

}
