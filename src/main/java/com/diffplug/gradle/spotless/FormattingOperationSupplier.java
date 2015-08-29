package com.diffplug.gradle.spotless;

public final class FormattingOperationSupplier {
	private final FormattingOperation formatter;
	private boolean initialized = false;

	public FormattingOperationSupplier(final FormattingOperation formatter) {
		this.formatter = formatter;
	}

	FormattingOperation get() throws Exception {
		if (!initialized) {
			initialize();
		}
		return formatter;
	}

	private void initialize() throws Exception {
		initialized = true;
		formatter.init();
	}
}
