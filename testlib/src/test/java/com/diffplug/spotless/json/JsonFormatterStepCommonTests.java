package com.diffplug.spotless.json;

import com.diffplug.spotless.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class JsonFormatterStepCommonTests {

	protected static final int INDENT = 4;

	@Test
	void cannotProvideNullProvisioner() {
		assertThatThrownBy(() -> createFormatterStep(INDENT, null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("provisioner cannot be null");
	}

	@Test
	void handlesNestedObject() throws Exception {
		doWithResource("nestedObject");
	}

	@Test
	void handlesSingletonArray() throws Exception {
		doWithResource("singletonArray");
	}

	@Test
	void handlesEmptyFile() throws Exception {
		doWithResource("empty");
	}

	@Test
	void canSetCustomIndentationLevel() throws Exception {
		FormatterStep step = createFormatterStep(6, TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStep(step);

		String before = "json/singletonArrayBefore.json";
		String after = "json/singletonArrayAfter6Spaces.json";
		stepHarness.testResource(before, after);
	}

	@Test
	void canSetIndentationLevelTo0() throws Exception {
		FormatterStep step = createFormatterStep(0, TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStep(step);

		String before = "json/singletonArrayBefore.json";
		String after = "json/singletonArrayAfter0Spaces.json";
		stepHarness.testResource(before, after);
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			int spaces = 0;

			@Override
			protected void setupTest(API api) {
				// no changes, are the same
				api.areDifferentThan();

				// with different spacing
				spaces = 1;
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return createFormatterStep(spaces, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}

	protected abstract FormatterStep createFormatterStep(int indent, Provisioner provisioner);

	protected StepHarness getStepHarness() {
		return StepHarness.forStep(createFormatterStep(INDENT, TestProvisioner.mavenCentral()));
	}

	protected void doWithResource(String name) throws Exception {
		String before = String.format("json/%sBefore.json", name);
		String after = String.format("json/%sAfter.json", name);
		getStepHarness().testResource(before, after);
	}

}
