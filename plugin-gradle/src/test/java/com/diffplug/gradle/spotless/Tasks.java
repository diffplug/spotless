package com.diffplug.gradle.spotless;

final class Tasks {
	private Tasks() {}

	static void execute(SpotlessTask task) throws Exception {
		task.performAction(Mocks.mockIncrementalTaskInputs(task.getTarget()));
	}
}
