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
package com.diffplug.spotless.extra.groovy;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.eclipse.EclipseCommonTests;

class GrEclipseFormatterStepTest extends EclipseCommonTests {
	@Override
	protected String[] getSupportedVersions() {
		return new String[]{"4.17.0", "4.18.0", "4.19.0"};
	}

	@Override
	protected String getTestInput(String version) {
		return "class F{ def m(){} }";
	}

	@Override
	protected String getTestExpectation(String version) {
		return "class F{\n\tdef m(){}\n}";
	}

	@Override
	protected FormatterStep createStep(String version) {
		EclipseBasedStepBuilder builder = GrEclipseFormatterStep.createBuilder(TestProvisioner.mavenCentral());
		builder.setVersion(version);
		return builder.build();
	}

}
