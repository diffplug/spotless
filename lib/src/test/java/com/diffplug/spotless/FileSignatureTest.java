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

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.junit.Test;

public class FileSignatureTest {
	private AbstractListAssert<?, List<? extends Integer>, Integer, ObjectAssert<Integer>> sortedSetResult(Integer... input) {
		List<Integer> result = FileSignature.toSortedSet(Arrays.asList(input));
		return Assertions.assertThat(result);
	}

	@Test
	public void testToSortedSet() {
		sortedSetResult().containsExactly();
		sortedSetResult(1).containsExactly(1);
		sortedSetResult(1, 1).containsExactly(1);
		sortedSetResult(2, 1).containsExactly(1, 2);
		sortedSetResult(1, 2, 1).containsExactly(1, 2);
	}
}
