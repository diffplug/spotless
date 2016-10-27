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

import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;

public class EncodingTest extends GradleIntegrationTest {
	@Test
	public void defaultIsUtf8() throws Exception {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        custom 'replaceMicro', { it.replace('µ', 'A') }",
				"    }",
				"}");
		write("test.java", "µ");
		gradleRunner().withArguments("spotlessApply").forwardOutput().build();
		Assert.assertEquals("A\n", read("test.java"));
	}

	@Test
	public void globalIsRespected() throws Exception {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        custom 'replaceMicro', { it.replace('µ', 'A') }",
				"    }",
				"    encoding 'US-ASCII'",
				"}");
		write("test.java", "µ");
		gradleRunner().withArguments("spotlessApply").build();
		Assert.assertEquals("??\n", read("test.java"));
	}

	@Test
	public void globalIsRespectedButCanBeOverridden() throws Exception {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        custom 'replaceMicro', { it.replace('µ', 'A') }",
				"    }",
				"    format 'utf32', {",
				"        target file('utf32.encoded')",
				"        custom 'replaceMicro', { it.replace('µ', 'A') }",
				"        encoding 'UTF-32'",
				"    }",
				"    encoding 'US-ASCII'",
				"}");
		write("test.java", "µ");
		write("utf32.encoded", LineEnding.UNIX, Charset.forName("UTF-32"), "µ");
		Assert.assertEquals("µ\n", read("utf32.encoded", LineEnding.UNIX, Charset.forName("UTF-32")));

		gradleRunner().withArguments("spotlessApply").build();
		Assert.assertEquals("??\n", read("test.java"));
		Assert.assertEquals("A\n", read("utf32.encoded", LineEnding.UNIX, Charset.forName("UTF-32")));
	}
}
