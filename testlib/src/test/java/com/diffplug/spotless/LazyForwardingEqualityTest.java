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
package com.diffplug.spotless;

import static com.diffplug.common.testing.SerializableTester.reserializeAndAssert;

import org.junit.jupiter.api.Test;

import com.diffplug.common.testing.EqualsTester;

@SuppressWarnings("serial")
class LazyForwardingEqualityTest {
	static Str s(String state) {
		return new Str(state);
	}

	static Other o(String state) {
		return new Other(state);
	}

	static class Str extends LazyForwardingEquality<String> {
		private final String state;

		Str(String state) {
			this.state = state;
		}

		@Override
		protected String calculateState() {
			return state;
		}
	}

	static class Other extends Str {
		Other(String state) {
			super(state);
		}
	}

	@Test
	void testEquality() {
		new EqualsTester()
				.addEqualityGroup(s("hello"), reserializeAndAssert(s("hello")))
				.addEqualityGroup(s("world"), reserializeAndAssert(s("world")))
				.addEqualityGroup(o("hello"), reserializeAndAssert(o("hello")))
				.addEqualityGroup(o("world"), reserializeAndAssert(o("world")))
				.testEquals();
	}
}
