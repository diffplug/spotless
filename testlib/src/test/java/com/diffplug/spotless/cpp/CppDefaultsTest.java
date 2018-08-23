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
package com.diffplug.spotless.cpp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;

import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.generic.LicenseHeaderStep;

public class CppDefaultsTest extends ResourceHarness {

	@Test
	public void testDelimiterExpr() throws Exception {
		final String header = "/*My tests header*/";
		FormatterStep step = LicenseHeaderStep.createFromHeader(header, CppDefaults.DELIMITER_EXPR);
		final File dummyFile = setFile("src/main/cpp/file1.dummy").toContent("");
		for (String testSource : Arrays.asList(
				"//Accpet multiple spaces between composed term.@int  main() {};",
				"//Accpet line delimiters between composed term.@void\n main() {};",
				"//Perfere 'using namespace' over 'namespace'.@using namespace std;",
				"//Detecting 'namespace' without 'using'.@namespace std {}",
				"//Detecting pre-processor statements.@#include <stdio.h>\nint i;")) {
			String output = step.format(testSource, dummyFile);
			String expected = testSource.replaceAll("(.*?)\\@", header + '\n');
			assertThat(output).isEqualTo(expected).as("Unexpected header insertion for '$s'.", testSource);
		}
	}

}
