/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless.sql;

import java.io.IOException;

import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.SerializableEqualityTester.API;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.sql.HibernateStep.Kind;

public class HibernateStepTest extends ResourceHarness {
	@Test
	public void behaviorBasic() throws Exception {
		FormatterStep step = HibernateStep.create(TestProvisioner.mavenCentral(), HibernateStep.Kind.BASIC);
		StepHarness.forStep(step)
				.testResource("sql/hibernate/basic.dirty", "sql/hibernate/basic.clean");
	}

	@Test
	public void behaviorDdl() throws Exception {
		FormatterStep step = HibernateStep.create(TestProvisioner.mavenCentral(), HibernateStep.Kind.DDL);
		StepHarness.forStep(step)
				.testResource("sql/hibernate/ddl.dirty", "sql/hibernate/ddl.clean");
	}

	@Test
	public void equality() throws Exception {
		new SerializableEqualityTester() {
			String version = HibernateStep.defaultVersion();
			Kind kind = Kind.BASIC;

			@Override
			protected void setupTest(API api) throws IOException {
				// same version == same
				api.areDifferentThan();
				// change the version, and it's different
				version = "5.2.11.Final";
				api.areDifferentThan();
				// change the kind, and it's different
				kind = Kind.DDL;
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return HibernateStep.create(version, TestProvisioner.mavenCentral(), kind);
			}
		}.testEquals();
	}
}
