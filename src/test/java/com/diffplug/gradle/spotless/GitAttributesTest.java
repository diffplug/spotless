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
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GitAttributesTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private void write(String path, String... content) throws IOException {
		File file = file(path);
		file.getParentFile().mkdirs();
		Files.write(file.toPath(), Arrays.asList(content));
	}

	private File file(String path) {
		return new File(folder.getRoot(), path);
	}

	@Test
	public void cacheTest() throws IOException {
		write(".gitattributes", "* eol=lf", "*.MF eol=crlf");
		{
			GitAttributesLineEndingPolicy.AttributesCache cache = new GitAttributesLineEndingPolicy.AttributesCache();
			Assert.assertEquals("lf", cache.valueFor(file("someFile"), "eol"));
			Assert.assertEquals("lf", cache.valueFor(file("subfolder/someFile"), "eol"));
			Assert.assertEquals("crlf", cache.valueFor(file("MANIFEST.MF"), "eol"));
			Assert.assertEquals("crlf", cache.valueFor(file("subfolder/MANIFEST.MF"), "eol"));

			// write out a .gitattributes for the subfolder
			write("subfolder/.gitattributes", "* eol=lf");

			// it shouldn't change anything, because it's cached
			Assert.assertEquals("lf", cache.valueFor(file("someFile"), "eol"));
			Assert.assertEquals("lf", cache.valueFor(file("subfolder/someFile"), "eol"));
			Assert.assertEquals("crlf", cache.valueFor(file("MANIFEST.MF"), "eol"));
			Assert.assertEquals("crlf", cache.valueFor(file("subfolder/MANIFEST.MF"), "eol"));
		}

		{
			// but if we make a new cache, it should change
			GitAttributesLineEndingPolicy.AttributesCache cache = new GitAttributesLineEndingPolicy.AttributesCache();
			Assert.assertEquals("lf", cache.valueFor(file("someFile"), "eol"));
			Assert.assertEquals("lf", cache.valueFor(file("subfolder/someFile"), "eol"));
			Assert.assertEquals("crlf", cache.valueFor(file("MANIFEST.MF"), "eol"));
			Assert.assertEquals("lf", cache.valueFor(file("subfolder/MANIFEST.MF"), "eol"));
		}
	}
}
