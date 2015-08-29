package com.diffplug.gradle.spotless;

public abstract class FormattingOperation {
	public abstract String apply(final String raw) throws Throwable;

	public void init() throws Exception {}
}
