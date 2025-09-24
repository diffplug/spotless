/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless.cpp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.generic.LicenseHeaderStep;

class CppDefaultsTest extends ResourceHarness {

	@Test
	void testDelimiterExpr() throws Exception {
		final String header = "/*My tests header*/";
		FormatterStep step = LicenseHeaderStep.headerDelimiter(header, CppDefaults.DELIMITER_EXPR).build();
		final File dummyFile = setFile("src/main/cpp/file1.dummy").toContent("");
		for (String testSource : Arrays.asList(
				"//Accpet multiple spaces between composed term.@using  namespace std;",
				"//Accpet line delimiters between composed term.@using\n namespace std;",
				"//Detecting 'namespace' without 'using'.@namespace foo {}",
				"//Assure key is beginning of word. along foo; @long foo;",
				"//Detecting pre-processor statements.@#include <stdio.h>\nint i;",
				"//Primitive C headers@void  foo();",
				"//Pointer in C headers@int* foo();",
				"//Microsoft C headers@__int8_t foo();")) {
			String output = null;
			try {
				output = step.format(testSource, dummyFile);
			} catch (IllegalArgumentException e) {
				throw new AssertionError("No delimiter found in '%s'".formatted(testSource), e);
			}
			String expected = testSource.replaceAll("(.*?)\\@", header + '\n');
			assertThat(output).isEqualTo(expected).as("Unexpected header insertion for '$s'.", testSource);
		}
	}

}
