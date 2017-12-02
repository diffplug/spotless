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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class SqlExtensionTest extends GradleIntegrationTest {

	@Test
	public void should_format_sql_with_default_configuration() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    sql {",
				"       dbeaver()",
				"    }",
				"}");

		File sqlFile = write("src/main/resources/aFolder/create.sql", getTestResource("sql/dbeaver/create.dirty"));

		// Run
		gradleRunner().withArguments("spotlessApply").build();

		// Common checks
		assertFileContent(getTestResource("sql/dbeaver/create.clean"), sqlFile);
	}

	@Test
	public void should_format_sql_with_alternative_configuration() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    sql {",
				"       dbeaver().configFile 'myConfig.properties'",
				"    }",
				"}");

		File sqlFile = write("src/main/resources/aFolder/create.sql", getTestResource("sql/dbeaver/create.dirty"));
		write("myConfig.properties", getTestResource("sql/dbeaver/myConfig.properties"));

		// Run
		gradleRunner().withArguments("spotlessApply").build();

		// Common checks
		assertFileContent(getTestResource("sql/dbeaver/create.clean.alternative"), sqlFile);
	}

}
