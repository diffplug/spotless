/*
 * Copyright 2021-2022 DiffPlug
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
package com.diffplug.spotless.json;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.*;

class JsonSimpleStepTest extends JsonFormatterStepCommonTests {

	@Test
	void handlesSingletonObject() throws Exception {
		doWithResource("singletonObject");
	}

	@Test
	void handlesSingletonObjectWithArray() throws Exception {
		doWithResource("singletonObjectWithArray");
	}

	@Test
	void handlesComplexNestedObject() throws Exception {
		doWithResource("cucumberJsonSample");
	}

	@Test
	void handlesObjectWithNull() throws Exception {
		doWithResource("objectWithNull");
	}

	@Test
	void handlesInvalidJson() {
		assertThatThrownBy(() -> doWithResource("invalidJson"))
				.isInstanceOf(AssertionError.class)
				.hasMessage("Unable to format JSON")
				.hasRootCauseMessage("Expected a ',' or '}' at 9 [character 0 line 3]");
	}

	@Test
	void handlesNotJson() {
		assertThatThrownBy(() -> doWithResource("notJson"))
				.isInstanceOf(AssertionError.class)
				.hasMessage("Unable to determine JSON type, expected a '{' or '[' but found '#'")
				.hasNoCause();
	}

	@Override
	protected FormatterStep createFormatterStep(int indent, Provisioner provisioner) {
		return JsonSimpleStep.create(indent, provisioner);
	}
}
