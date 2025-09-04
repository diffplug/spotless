package com.diffplug.spotless.glue.ktfmt;

import com.facebook.ktfmt.format.TrailingCommaManagementStrategy;

public enum KtfmtTrailingCommaManagementStrategy {
	NONE,
	ONLY_ADD,
	COMPLETE;

	public TrailingCommaManagementStrategy toFormatterTrailingCommaManagementStrategy() {
		return switch (this) {
			case NONE -> TrailingCommaManagementStrategy.NONE;
			case ONLY_ADD -> TrailingCommaManagementStrategy.ONLY_ADD;
			case COMPLETE -> TrailingCommaManagementStrategy.COMPLETE;
		};
	}
}
