package com.diffplug.gradle.spotless;

import org.junit.Test;

public class EndWithNewlineTest extends ResourceTest {
	@Test
	public void trimTrailingNewlines() throws Exception {
		endWithNewlineTest("", "\n");
		endWithNewlineTest("\n");
		endWithNewlineTest("\n\n\n\n", "\n");
		endWithNewlineTest("line", "line\n");
		endWithNewlineTest("line\n");
		endWithNewlineTest("line\nline\n\n\n\n", "line\nline\n");
	}

	private void endWithNewlineTest(String before) throws Exception {
		endWithNewlineTest(before, before);
	}

	private void endWithNewlineTest(String before, String after) throws Exception {
		super.assertTask(test -> {
			test.endWithNewline();
		}, before, after);
	}
}
