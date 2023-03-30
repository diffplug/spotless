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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.diffplug.common.base.Box;
import com.diffplug.common.testing.EqualsTester;

public abstract class SerializableEqualityTester {
	protected abstract Serializable create();

	protected abstract void setupTest(API api) throws Exception;

	public interface API {
		void areDifferentThan();
	}

	public void testEquals() {
		List<List<Object>> allGroups = new ArrayList<>();
		Box<List<Object>> currentGroup = Box.of(new ArrayList<>());
		var api = new API() {
			@Override
			public void areDifferentThan() {
				currentGroup.modify(current -> {
					// create two instances, and add them to the group
					current.add(create());
					current.add(create());
					// create two instances using a serialization roundtrip, and add them to the group
					current.add(reserialize(create()));
					current.add(reserialize(create()));
					// add this group to the list of all groups
					allGroups.add(current);
					// and return a new blank group for the next call
					return new ArrayList<>();
				});
			}
		};
		try {
			setupTest(api);
		} catch (Exception e) {
			throw new AssertionError("Error during setupTest", e);
		}
		List<Object> lastGroup = currentGroup.get();
		if (!lastGroup.isEmpty()) {
			throw new IllegalArgumentException("Looks like you forgot to make a final call to 'areDifferentThan()'.");
		}
		EqualsTester tester = new EqualsTester();
		for (List<Object> step : allGroups) {
			tester.addEqualityGroup(step.toArray());
		}
		tester.testEquals();
	}

	@SuppressWarnings("unchecked")
	private static <T extends Serializable> T reserialize(T input) {
		byte[] asBytes = LazyForwardingEquality.toBytes(input);
		var byteInput = new ByteArrayInputStream(asBytes);
		try (var objectInput = new ObjectInputStream(byteInput)) {
			return (T) objectInput.readObject();
		} catch (IOException | ClassNotFoundException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}
}
