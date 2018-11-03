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

import java.io.IOException;

import org.junit.Test;

public class HasBuiltinDelimiterForLicenseTest extends GradleIntegrationTest {

	@Test
	public void testWithCommonInterfaceForConfiguringLicences() throws IOException {
		// TODO: JLL Convert this to a Kotlin example when supported: https://github.com/gradle/kotlin-dsl/issues/492
		setFile("build.gradle").toLines(
				"import com.diffplug.gradle.spotless.HasBuiltinDelimiterForLicense",
				"plugins {",
				"    id(\"org.jetbrains.kotlin.jvm\") version \"1.2.31\"",
				"    id(\"com.diffplug.gradle.spotless\")",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        assert (it instanceof HasBuiltinDelimiterForLicense) : \"Was `$it`\"",
				"    }",
				"    java {",
				"        assert (it instanceof HasBuiltinDelimiterForLicense) : \"Was `$it`\"",
				"    }",
				"    cpp {",
				"        assert (it instanceof HasBuiltinDelimiterForLicense) : \"Was `$it`\"",
				"    }",
				"    css {",
				"        assert (it instanceof HasBuiltinDelimiterForLicense) : \"Was `$it`\"",
				"    }",
				"    html {",
				"        assert (it instanceof HasBuiltinDelimiterForLicense) : \"Was `$it`\"",
				"    }",
				"    js {",
				"        assert (it instanceof HasBuiltinDelimiterForLicense) : \"Was `$it`\"",
				"    }",
				"    json {",
				"        assert (it instanceof HasBuiltinDelimiterForLicense) : \"Was `$it`\"",
				"    }",
				"    xml {",
				"        assert (it instanceof HasBuiltinDelimiterForLicense) : \"Was `$it`\"",
				"    }",
				"}");
		gradleRunner()
				.withGradleVersion("4.6")
				.withArguments("spotlessApply")
				.forwardOutput()
				.build();
	}
}
