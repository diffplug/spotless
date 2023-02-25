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
package com.diffplug.spotless;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.generic.ReplaceStep;

class FilterByFileFormatterStepTest extends ResourceHarness {
	@Test
	void behavior() throws Exception {
		FormatterStep underTest = ReplaceStep.create("makeSpaceA", " ", "a")
				.filterByFile(SerializableFileFilter.skipFilesNamed("dontFormat"));
		assertThat(underTest.format(" ", new File("someFileName"))).isEqualTo("a");
		assertThat(underTest.format(" ", new File("dontFormat"))).isEqualTo(" ");
	}

	@Test
	void equality() throws Exception {
		new SerializableEqualityTester() {
			String state;
			String filter;

			@Override
			protected void setupTest(API api) {
				// no filter, standard state
				state = "state";
				filter = null;
				api.areDifferentThan();
				// same state, but now with a filter
				filter = "a";
				api.areDifferentThan();
				// same state, but now with a filter
				filter = "b";
				api.areDifferentThan();
				// same filter, but the state has changed
				state = "otherState";
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				FormatterStep baseStep = FormatterStep.create("name", state, state -> input -> state);
				if (filter == null) {
					return baseStep;
				} else {
					return baseStep.filterByFile(SerializableFileFilter.skipFilesNamed(filter));
				}
			}
		}.testEquals();
	}
}
