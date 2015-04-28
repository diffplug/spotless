package com.diffplug.gradle.spotless;

/** The line endings written by the tool. */
public enum LineEnding {
	PLATFORM_NATIVE(System.getProperty("line.separator")), WINDOWS("\r\n"), UNIX("\n");

	public final String string;

	private LineEnding(String ending) {
		this.string = ending;
	}

	public boolean isWin() {
		return string.equals("\r\n");
	}
}
