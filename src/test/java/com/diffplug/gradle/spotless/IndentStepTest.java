package com.diffplug.gradle.spotless;

import org.junit.Test;

public class IndentStepTest extends ResourceTest {
	@Test
	public void tabToTab() throws Throwable {
		IndentStep indent = new IndentStep(IndentStep.Type.TAB, 4);
		assertStep(indent::format, "IndentedWithTab.test", "IndentedWithTab.test");
	}

	@Test
	public void spaceToSpace() throws Throwable {
		IndentStep indent = new IndentStep(IndentStep.Type.SPACE, 4);
		assertStep(indent::format, "IndentedWithSpace.test", "IndentedWithSpace.test");
	}

	@Test
	public void spaceToTab() throws Throwable {
		IndentStep indent = new IndentStep(IndentStep.Type.TAB, 4);
		assertStep(indent::format, "IndentedWithSpace.test", "IndentedWithTab.test");
	}

	@Test
	public void tabToSpace() throws Throwable {
		IndentStep indent = new IndentStep(IndentStep.Type.SPACE, 4);
		assertStep(indent::format, "IndentedWithTab.test", "IndentedWithSpace.test");
	}
}
