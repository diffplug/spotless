package com.diffplug.spotless.java;

import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.StepEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

public class RemoveUnusedImportsStepTest {
	@Test
	public void behavior() throws Exception {
		StepHarness.forStep(RemoveUnusedImportsStep.create(TestProvisioner.mavenCentral()))
				.testResource("java/removeunusedimports/JavaCodeUnformatted.test", "java/removeunusedimports/JavaCodeFormatted.test");
	}

	@Test
	public void equality() throws Exception {
		new StepEqualityTester() {
			@Override
			protected void setupTest(API api) {
				api.assertThisEqualToThis();
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return RemoveUnusedImportsStep.create(TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}
}
