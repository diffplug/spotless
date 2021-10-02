/*
 * Copyright 2016-2021 DiffPlug
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

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;

class DBeaverSQLFormatterStepTest extends ResourceHarness {

	@Test
	void behavior() throws Exception {
		FormatterStep step = DBeaverSQLFormatterStep.create(Collections.emptySet());
		StepHarness.forStep(step)
				.testResource("sql/dbeaver/full.dirty", "sql/dbeaver/full.clean")
				.testResource("sql/dbeaver/V1_initial.sql.dirty", "sql/dbeaver/V1_initial.sql.clean")
				.testResource("sql/dbeaver/alter-table.dirty", "sql/dbeaver/alter-table.clean")
				.testResource("sql/dbeaver/create.dirty", "sql/dbeaver/create.clean")
				.testResource("sql/dbeaver/jdbi-params.dirty", "sql/dbeaver/jdbi-params.clean");
	}

	@Test
	void behaviorWithConfigFile() throws Exception {
		FormatterStep step = DBeaverSQLFormatterStep.create(createTestFiles("sql/dbeaver/sqlConfig.properties"));
		StepHarness.forStep(step)
				.testResource("sql/dbeaver/create.dirty", "sql/dbeaver/create.clean");
	}

	@Test
	void behaviorWithAlternativeConfigFile() throws Exception {
		FormatterStep step = DBeaverSQLFormatterStep.create(createTestFiles("sql/dbeaver/sqlConfig2.properties"));
		StepHarness.forStep(step)
				.testResource("sql/dbeaver/create.dirty", "sql/dbeaver/create.clean.alternative");
	}

	@Test
	void equality() throws Exception {
		List<File> sqlConfig1 = createTestFiles("sql/dbeaver/sqlConfig.properties");
		List<File> sqlConfig2 = createTestFiles("sql/dbeaver/sqlConfig2.properties");
		new SerializableEqualityTester() {
			List<File> settingsFiles;

			@Override
			protected void setupTest(API api) {
				settingsFiles = sqlConfig1;
				api.areDifferentThan();

				settingsFiles = sqlConfig2;
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return DBeaverSQLFormatterStep.create(settingsFiles);
			}
		}.testEquals();
	}

}
