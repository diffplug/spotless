/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.spotless.npm;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.TestProvisioner;

public class NodeJsNativeDoubleLoadTest {
	@Test
	public void inMultipleClassLoaders() throws Exception {
		JarState state = JarState.from(NpmFormatterStepStateBase.j2v8MavenCoordinate(), TestProvisioner.mavenCentral());
		ClassLoader loader1 = state.getClassLoader(1);
		ClassLoader loader2 = state.getClassLoader(2);
		createAndTestWrapper(loader1);
		createAndTestWrapper(loader2);
	}

	@Test
	public void multipleTimesInOneClassLoader() throws Exception {
		JarState state = JarState.from(NpmFormatterStepStateBase.j2v8MavenCoordinate(), TestProvisioner.mavenCentral());
		ClassLoader loader3 = state.getClassLoader(3);
		createAndTestWrapper(loader3);
		createAndTestWrapper(loader3);
	}

	private void createAndTestWrapper(ClassLoader loader) throws Exception {
		try (NodeJSWrapper node = new NodeJSWrapper(loader)) {
			V8ObjectWrapper object = node.createNewObject(ImmutableMap.of("a", 1));
			Optional<Integer> value = object.getOptionalInteger("a");
			Assertions.assertThat(value).hasValue(1);
			object.release();
		}
	}
}
