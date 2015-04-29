package com.diffplug.gradle.spotless;

import org.junit.Test;

public class TrimTrailingWhitespaceTest extends ResourceTest {
	@Test
	public void trimTrailingWhitespace() throws Exception {
		trimTrailingWhitespaceTestCase("");
		trimTrailingWhitespaceTestCase("\n");
		trimTrailingWhitespaceTestCase("\n\n\n");
		trimTrailingWhitespaceTestCase("   preceding");

		trimTrailingWhitespaceTestCase("trailing  ", "trailing");
		trimTrailingWhitespaceTestCase("trailing  \n", "trailing\n");
		trimTrailingWhitespaceTestCase("trailing\t", "trailing");
		trimTrailingWhitespaceTestCase("trailing\t\n", "trailing\n");

		trimTrailingWhitespaceTestCase("\t  trailing  ", "\t  trailing");
		trimTrailingWhitespaceTestCase("\t  trailing  \n", "\t  trailing\n");
		trimTrailingWhitespaceTestCase("\t  trailing\t", "\t  trailing");
		trimTrailingWhitespaceTestCase("\t  trailing\t\n", "\t  trailing\n");

		trimTrailingWhitespaceTestCase("Line\nLine");
		trimTrailingWhitespaceTestCase("Line  \nLine", "Line\nLine");
		trimTrailingWhitespaceTestCase("Line\nLine  ", "Line\nLine");
		trimTrailingWhitespaceTestCase("Line  \nLine  ", "Line\nLine");
		trimTrailingWhitespaceTestCase("  Line  \nLine  ", "  Line\nLine");
	}

	private void trimTrailingWhitespaceTestCase(String before) throws Exception {
		trimTrailingWhitespaceTestCase(before, before);
	}

	private void trimTrailingWhitespaceTestCase(String before, String after) throws Exception {
		super.assertTask(test -> {
			test.trimTrailingWhitespace();
		}, before, after);
	}
}
