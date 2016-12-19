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
package com.diffplug.spotless;

import static com.diffplug.common.testing.SerializableTester.*;

import org.junit.Test;

import com.diffplug.common.testing.EqualsTester;
import com.diffplug.spotless.LazyForwardingEquality;

@SuppressWarnings("serial")
public class LazyForwardingEqualityTest {
	static Str s(String key) {
		return new Str(key);
	}

	static Other o(String key) {
		return new Other(key);
	}

	static Immediate i(String key) {
		return new Immediate(key);
	}

	static class Str extends LazyForwardingEquality<String> {
		private String key;

		Str(String key) {
			this.key = key;
		}

		@Override
		protected String calculateKey() {
			return key;
		}
	}

	static class Other extends Str {
		Other(String key) {
			super(key);
		}
	}

	static class Immediate extends ForwardingEquality<String> {
		Immediate(String key) {
			super(key);
		}
	}

	/**
	 * Equality should be based entirely on the key,
	 * no matter the encapsulating clazz, lazy or not.
	 */
	@Test
	public void testEquality() {
		new EqualsTester()
				.addEqualityGroup(
						s("hello"), reserializeAndAssert(s("hello")),
						o("hello"), reserializeAndAssert(o("hello")),
						i("hello"), reserializeAndAssert(i("hello")))
				.addEqualityGroup(
						s("world"), reserializeAndAssert(s("world")),
						o("world"), reserializeAndAssert(o("world")),
						i("world"), reserializeAndAssert(i("world")))
				.testEquals();
	}
}
