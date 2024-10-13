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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Gradle requires three things:
 * - Gradle defines cache equality based on your serialized representation
 * - Combined with remote build cache, you cannot have any absolute paths in
 * your serialized representation
 * - Combined with configuration cache, you must be able to roundtrip yourself
 * through serialization
 *
 * These requirements are at odds with each other, as described in these issues
 * - Gradle issue to define custom equality
 * https://github.com/gradle/gradle/issues/29816
 * - Spotless plea for developer cache instead of configuration cache
 * https://github.com/diffplug/spotless/issues/987
 * - Spotless cache miss bug fixed by this class
 * https://github.com/diffplug/spotless/issues/2168
 *
 * This class is a `List<FormatterStep>` which can optimize the
 * serialized representation for either
 * - roundtrip integrity
 * - OR
 * - equality
 *
 * Because it is not possible to provide both at the same time.
 * It is a horrific hack, but it works, and it's the only way I can figure
 * to make Spotless work with all of Gradle's cache systems at once.
 */
public class ConfigurationCacheHackList implements java.io.Serializable {
	private final boolean optimizeForEquality;
	private final ArrayList<Object> backingList = new ArrayList<>();

	public static ConfigurationCacheHackList forEquality() {
		return new ConfigurationCacheHackList(true);
	}

	public static ConfigurationCacheHackList forRoundtrip() {
		return new ConfigurationCacheHackList(false);
	}

	private ConfigurationCacheHackList(boolean optimizeForEquality) {
		this.optimizeForEquality = optimizeForEquality;
	}

	public void clear() {
		backingList.clear();
	}

	public void addAll(Collection<? extends FormatterStep> c) {
		for (FormatterStep step : c) {
			if (step instanceof FormatterStepSerializationRoundtrip) {
				var clone = ((FormatterStepSerializationRoundtrip) step).hackClone(optimizeForEquality);
				backingList.add(clone);
			} else {
				backingList.add(step);
			}
		}
	}

	public List<FormatterStep> getSteps() {
		var result = new ArrayList<FormatterStep>(backingList.size());
		for (Object obj : backingList) {
			if (obj instanceof FormatterStepSerializationRoundtrip.HackClone) {
				result.add(((FormatterStepSerializationRoundtrip.HackClone) obj).rehydrate());
			} else {
				result.add((FormatterStep) obj);
			}
		}
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ConfigurationCacheHackList stepList = (ConfigurationCacheHackList) o;
		return optimizeForEquality == stepList.optimizeForEquality &&
				backingList.equals(stepList.backingList);
	}

	@Override
	public int hashCode() {
		return Objects.hash(optimizeForEquality, backingList);
	}
}
