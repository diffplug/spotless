/*
 * Copyright 2016-2024 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.extra.java;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.EquoBasedStepBuilder;

class EclipseJdtFormatterStepSpecialCaseTest {
	/** https://github.com/diffplug/spotless/issues/1638 */
	@Test
	void issue_1638() {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("eclipse_formatter_issue_1638.xml").getFile());
		EquoBasedStepBuilder builder = EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral());
		builder.setPreferences(List.of(file));
		StepHarness.forStep(builder.build())
				.testResource("java/eclipse/AbstractType.test", "java/eclipse/AbstractType.clean");
	}

	@Test
	void sort_members_global_by_visibility() {
		EclipseJdtFormatterStep.Builder builder = EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral());
		builder.setMembersOrdering("SF,SI,SM,F,I,C,M,T", false);
		builder.setVisibilityOrdering("B,R,D,V");
		StepHarness.forStep(builder.build())
				.testResource("java/eclipse/SortExample.test", "java/eclipse/SortExample.sortMembersByVisibility.clean");
	}

	@Test
	void sort_members_global_enabled() {
		EclipseJdtFormatterStep.Builder builder = EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral());
		builder.setMembersOrdering("SF,SI,SM,F,I,C,M,T", false);
		StepHarness.forStep(builder.build())
				.testResource("java/eclipse/SortExample.test", "java/eclipse/SortExample.sortMembers.clean");
	}

	@Test
	void sort_members_global_no_fields() {
		EclipseJdtFormatterStep.Builder builder = EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral());
		builder.setMembersOrdering("SF,SI,SM,F,I,C,M,T", true);
		StepHarness.forStep(builder.build())
				.testResource("java/eclipse/SortExample.test", "java/eclipse/SortExample.sortMembersNoFields.clean");
	}

	@Test
	void sort_members_local_by_visibility() {
		EclipseJdtFormatterStep.Builder builder = EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral());
		builder.setMembersOrdering("SF,SI,SM,F,I,C,M,T", false);
		builder.setVisibilityOrdering("B,R,D,V");
		StepHarness.forStep(builder.build())
				.testResource("java/eclipse/SortExample.localSortByVisibility.test", "java/eclipse/SortExample.localSortByVisibility.clean");
	}

	@Test
	void sort_members_local_enabled_false() {
		EclipseJdtFormatterStep.Builder builder = EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral());
		builder.setMembersOrdering("SF,SI,SM,F,I,C,M,T", false);
		StepHarness.forStep(builder.build())
				.testResource("java/eclipse/SortExample.localEnabledFalse.test", "java/eclipse/SortExample.localEnabledFalse.clean");
	}

	@Test
	void sort_members_local_no_fields() {
		EclipseJdtFormatterStep.Builder builder = EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral());
		builder.setMembersOrdering("SF,SI,SM,F,I,C,M,T", false);
		StepHarness.forStep(builder.build())
				.testResource("java/eclipse/SortExample.localDoNotSortFields.test", "java/eclipse/SortExample.localDoNotSortFields.clean");
	}


	@Test
	void sort_members_local_enabled_true() {
		EclipseJdtFormatterStep.Builder builder = EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral());
		StepHarness.forStep(builder.build())
			.testResource("java/eclipse/SortExample.localEnabledTrue.test", "java/eclipse/SortExample.localEnabledTrue.clean");
	}
}
