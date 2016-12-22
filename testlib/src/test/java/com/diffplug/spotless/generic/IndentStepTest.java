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
package com.diffplug.spotless.generic;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.generic.IndentStep;

public class IndentStepTest extends ResourceHarness {
	@Test
	public void tabToTab() throws Throwable {
		FormatterStep indent = IndentStep.Type.TAB.create(4);
		assertOnResources(indent, "indent/IndentedWithTab.test", "indent/IndentedWithTab.test");
	}

	@Test
	public void spaceToSpace() throws Throwable {
		FormatterStep indent = IndentStep.Type.SPACE.create(4);
		assertOnResources(indent, "indent/IndentedWithSpace.test", "indent/IndentedWithSpace.test");
	}

	@Test
	public void spaceToTab() throws Throwable {
		FormatterStep indent = IndentStep.Type.TAB.create(4);
		assertOnResources(indent, "indent/IndentedWithSpace.test", "indent/IndentedWithTab.test");
	}

	@Test
	public void tabToSpace() throws Throwable {
		FormatterStep indent = IndentStep.Type.SPACE.create(4);
		assertOnResources(indent, "indent/IndentedWithTab.test", "indent/IndentedWithSpace.test");
	}

	@Test
	public void doesntClipNewlines() throws Throwable {
		FormatterStep indent = IndentStep.Type.SPACE.create(4);
		String blankNewlines = "\n\n\n\n";
		Assert.assertEquals(blankNewlines, indent.format(blankNewlines, new File("")));
	}
}
