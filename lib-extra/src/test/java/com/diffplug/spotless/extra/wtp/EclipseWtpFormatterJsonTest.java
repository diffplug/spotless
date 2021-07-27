/*
 * Copyright 2021 DiffPlug
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
package com.diffplug.spotless.extra.wtp;

import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;

class EclipseWtpFormatterJsonTest extends EclipseWtpFormatterCommonTests {

	@Override
	EclipseBasedStepBuilder createBuilder() {
		return EclipseWtpFormatterStep.JSON.createBuilder(TestProvisioner.mavenCentral());
	}

	@Override
	String getTestInput() {
		return "{\"a\": \"b\",	\"c\":   { \"d\": \"e\",\"f\": \"g\"}}";
	}

	@Override
	String getTestExpectation() {
		return "{\n\t\"a\": \"b\",\n\t\"c\": {\n\t\t\"d\": \"e\",\n\t\t\"f\": \"g\"\n\t}\n}";
	}
}
