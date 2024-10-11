/*
 * Copyright 2024 DiffPlug
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

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Gradle requires three things:
 * - Gradle defines cache equality based on your serialized representation
 * - Combined with remote build cache, you cannot have any absolute paths in your serialized representation
 * - Combined with configuration cache, you must be able to roundtrip yourself through serialization
 *
 * These requirements are at odds with each other, as described in these issues
 * - Gradle issue to define custom equality https://github.com/gradle/gradle/issues/29816
 * - Spotless plea for developer cache instead of configuration cache https://github.com/diffplug/spotless/issues/987
 * - Spotless cache miss bug fixed by this class https://github.com/diffplug/spotless/issues/2168
 *
 * The point of this class is to create containers which can optimize the serialized representation for either
 * - roundtrip integrity
 * - equality
 *
 * It is a horrific hack, but it works, and it's the only way I can figure
 * to make Spotless work with all of Gradle's cache systems.
 */
public class ConfigurationCacheHack {
	static boolean SERIALIZE_FOR_ROUNDTRIP = false;

	public enum OptimizeFor {
		ROUNDTRIP, EQUALITY,
	}

	public static class StepList extends AbstractList<FormatterStep> {
		private final boolean optimizeForEquality;
		private transient ArrayList<FormatterStep> backingList = new ArrayList<>();

		public StepList(OptimizeFor optimizeFor) {
			this.optimizeForEquality = optimizeFor == OptimizeFor.EQUALITY;
		}

		@Override
		public void clear() {
			backingList.clear();
		}

		@Override
		public boolean addAll(Collection<? extends FormatterStep> c) {
			return backingList.addAll(c);
		}

		private void writeObject(java.io.ObjectOutputStream out) throws IOException {
			out.defaultWriteObject();
			SERIALIZE_FOR_ROUNDTRIP = !optimizeForEquality;
			out.writeObject(backingList);
		}

		private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
			in.defaultReadObject();
			backingList = (ArrayList<FormatterStep>) in.readObject();
		}

		@Override
		public FormatterStep get(int index) {
			return backingList.get(index);
		}

		@Override
		public int size() {
			return backingList.size();
		}
	}
}
