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

import java.util.ArrayList;
import java.util.List;

import com.diffplug.common.base.Box;
import com.diffplug.common.debug.LapTimer;
import com.diffplug.common.debug.StepProfiler;
import com.diffplug.common.testing.EqualsTester;

public abstract class StepEqualityTester {
	protected abstract FormatterStep create();

	protected abstract void setupTest(API api);

	public interface API {
		void assertThis();

		void assertThisEqualToThis();

		void areDifferentThan();
	}

	public static final StepProfiler PROFILER = new StepProfiler(LapTimer.createNanoWrap2Sec());

	public void testEquals() {
		List<List<Object>> allGroups = new ArrayList<>();
		Box<List<Object>> currentGroup = Box.of(new ArrayList<>());
		API api = new API() {
			@Override
			public void assertThis() {
				PROFILER.startStep("create");
				currentGroup.get().add(create());
				PROFILER.finish();
			}

			@Override
			public void assertThisEqualToThis() {
				assertThis();
				assertThis();
			}

			@Override
			public void areDifferentThan() {
				allGroups.add(currentGroup.get());
				currentGroup.set(new ArrayList<>());
			}
		};
		setupTest(api);
		List<Object> lastGroup = currentGroup.get();
		if (!lastGroup.isEmpty()) {
			throw new IllegalArgumentException("Looks like you forgot to make a final call to 'areDifferentThan()'.");
		}
		EqualsTester tester = new EqualsTester();
		for (List<Object> step : allGroups) {
			tester.addEqualityGroup(step.toArray());
		}
		PROFILER.startStep("equals");
		tester.testEquals();
		PROFILER.finish();
		PROFILER.printResults();
	}
}
