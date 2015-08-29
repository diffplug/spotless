/*
 * Copyright 2015 DiffPlug
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
package com.diffplug.gradle.spotless;

import org.junit.Assert;
import org.junit.Test;

public class IndentStepTest extends ResourceTest {

	FormattingOperation applyIndent(final IndentStep indent) {
		return new FormattingOperation() {
			@Override
			public String apply(String raw) throws Throwable {
				return indent.format(raw);
			}
		};
	}

	@Test
	public void tabToTab() throws Throwable {
		assertStep(applyIndent(new IndentStep(IndentStep.Type.TAB, 4)), "IndentedWithTab.test", "IndentedWithTab.test");
	}

	@Test
	public void spaceToSpace() throws Throwable {
		assertStep(applyIndent(new IndentStep(IndentStep.Type.SPACE, 4)), "IndentedWithSpace.test", "IndentedWithSpace.test");
	}

	@Test
	public void spaceToTab() throws Throwable {
		assertStep(applyIndent(new IndentStep(IndentStep.Type.TAB, 4)), "IndentedWithSpace.test", "IndentedWithTab.test");
	}

	@Test
	public void tabToSpace() throws Throwable {
		assertStep(applyIndent(new IndentStep(IndentStep.Type.SPACE, 4)), "IndentedWithTab.test", "IndentedWithSpace.test");
	}

	@Test
	public void doesntClipNewlines() throws Throwable {
		IndentStep indent = new IndentStep(IndentStep.Type.SPACE, 4);
		String blankNewlines = "\n\n\n\n";
		Assert.assertEquals(blankNewlines, indent.format(blankNewlines));
	}
}
