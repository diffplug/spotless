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
package com.diffplug.gradle.spotless;

import org.junit.Test;

import com.diffplug.common.testing.EqualsTester;
import com.diffplug.common.testing.SerializableTester;

public class ForwardingEqualityTest {
	static Str s(String key) {
		return new Str(key);
	}

	@SuppressWarnings("serial")
	static class Str extends ForwardingEquality<String> {
		Str(String key) {
			super(key);
		}
	}

	static Int i(int key) {
		return new Int(key);
	}

	@SuppressWarnings("serial")
	static class Int extends ForwardingEquality<Integer> {
		Int(int key) {
			super(key);
		}
	}

	@Test
	public void testEquality() {
		new EqualsTester()
				.addEqualityGroup(s("hello"), s("hello"), s("h" + "ello"))
				.addEqualityGroup(s("world"), s("world"), s("wor" + "ld"))
				.addEqualityGroup(i(3), i(3), i(1 + 2))
				.addEqualityGroup(i(-6), i(-6), i(1 - 7))
				.testEquals();
	}

	@Test
	public void testSerialization() {
		SerializableTester.reserializeAndAssert(s("hello"));
		SerializableTester.reserializeAndAssert(s("world"));
		SerializableTester.reserializeAndAssert(i(4));
		SerializableTester.reserializeAndAssert(i(-6));
	}
}
