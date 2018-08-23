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
package com.diffplug.spotless.maven.cpp;

import static org.junit.Assert.assertEquals;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import com.diffplug.spotless.ResourceHarness;

public class CppTest extends ResourceHarness {

	@Test
	public void testDefaultIncludes() throws Exception {
		Cpp cpp = new Cpp();
		cpp.defaultIncludes();

		String includesString = String.join(",", cpp.defaultIncludes());
		setFile("src/main/cpp/file1.c++").toContent("");
		setFile("src/main/cpp/file1.dummy").toContent("");
		setFile("src/test/cpp/file1.h").toContent("");
		setFile("src/test/cpp/file1.dummy").toContent("");

		assertEquals(2, FileUtils.getFiles(rootFolder(), includesString, "").size());

	}
}
