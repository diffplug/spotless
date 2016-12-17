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

import java.util.List;

import com.diffplug.common.collect.testing.ListTestSuiteBuilder;
import com.diffplug.common.collect.testing.TestStringListGenerator;
import com.diffplug.common.collect.testing.features.CollectionFeature;
import com.diffplug.common.collect.testing.features.CollectionSize;

import junit.framework.TestSuite;

public class NonSerializableListTest {
	public static TestSuite suite() {
		return ListTestSuiteBuilder
				.using(new TestStringListGenerator() {
					@Override
					protected List<String> create(String[] elements) {
						return NonSerializableList.of(elements);
					}
				})
				.named("NonSerializableList")
				.withFeatures(
						CollectionSize.ANY,
						CollectionFeature.ALLOWS_NULL_QUERIES)
				.createTestSuite();
	}
}
