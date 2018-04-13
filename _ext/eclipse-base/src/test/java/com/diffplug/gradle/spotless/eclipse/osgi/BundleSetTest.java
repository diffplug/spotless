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
package com.diffplug.gradle.spotless.eclipse.osgi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class BundleSetTest {

	BundleSet instance;

	@Before
	public void initialize() {
		instance = new BundleSet();
	}

	@Test
	public void testAddGet() throws BundleException {
		Bundle testBundle1 = new TestBundle(1, "a");
		Bundle testBundle2 = new TestBundle(2, "b");
		instance.add(testBundle1);
		instance.add(testBundle2);
		assertEquals("Get by ID 1", testBundle1, instance.get(1));
		assertEquals("Get by ID 2", testBundle2, instance.get(2));
		assertEquals("Get by symbolic name 'a'", testBundle1, instance.get("a"));
		assertEquals("Get by symbolic name 'b'", testBundle2, instance.get("b"));
		assertTrue("Contains all", instance.getAll().containsAll(Arrays.asList(testBundle1, testBundle2)));
	}

	@Test
	public void testSameSymbolicName() throws BundleException {
		final String symbolicName = "sym.a";
		final long id1 = 12345;
		final long id2 = 23456;
		Bundle testBundle1 = new TestBundle(id1, symbolicName);
		Bundle testBundle2 = new TestBundle(id2, symbolicName);
		instance.add(testBundle1);
		boolean exceptionCaught = false;
		try {
			instance.add(testBundle2);
		} catch (BundleException e) {
			exceptionCaught = true;
			assertThat(e.getMessage()).as("BundleException does not contain symbolic name.").contains(symbolicName);
			assertThat(e.getMessage()).as("BundleException does not contain ID of exisiting bundle.").contains(Long.toString(id1));
			assertThat(e.getMessage()).as("BundleException does not contain ID of new bundle.").contains(Long.toString(id2));
		}
		assertThat(exceptionCaught).as("No BundleException thrown for duplicate symblic name.").isTrue();
	}

	@Test
	public void testSameID() throws BundleException {
		final String symbolicName1 = "sym.a";
		final String symbolicName2 = "sym.b";
		final long id = 12345;
		Bundle testBundle1 = new TestBundle(id, symbolicName1);
		Bundle testBundle2 = new TestBundle(id, symbolicName2);
		instance.add(testBundle1);
		boolean exceptionCaught = false;
		try {
			instance.add(testBundle2);
		} catch (BundleException e) {
			exceptionCaught = true;
			assertThat(e.getMessage()).as("BundleException does not contain ID.").contains(Long.toString(id));
			assertThat(e.getMessage()).as("BundleException does not contain symbolic name of exisiting bundle.").contains(symbolicName1);
			assertThat(e.getMessage()).as("BundleException does not contain symbolic name of new bundle.").contains(symbolicName2);
		}
		assertThat(exceptionCaught).as("No BundleException thrown for duplicate symblic name.").isTrue();
	}

}
