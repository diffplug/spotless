/*
 * Copyright 2016-2022 DiffPlug
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
package com.diffplug.spotless.extra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ResourceHarness;

class GitAttributesTest extends ResourceHarness {
	private List<File> testFiles() {
		List<File> result = new ArrayList<>();
		for (String path : TEST_PATHS) {
			setFile(path).toContent("");
			result.add(newFile(path));
		}
		return result;
	}

	private static List<String> TEST_PATHS = Arrays.asList("someFile", "subfolder/someFile", "MANIFEST.MF", "subfolder/MANIFEST.MF");

	@Test
	void cacheTest() throws IOException {
		setFile(".gitattributes").toContent(StringPrinter.buildStringFromLines(
				"* eol=lf",
				"*.MF eol=crlf"));
		{
			GitAttributesLineEndings.AttributesCache cache = new GitAttributesLineEndings.AttributesCache();
			Assertions.assertThat(cache.valueFor(newFile("someFile"), "eol")).isEqualTo("lf");
			Assertions.assertThat(cache.valueFor(newFile("subfolder/someFile"), "eol")).isEqualTo("lf");
			Assertions.assertThat(cache.valueFor(newFile("MANIFEST.MF"), "eol")).isEqualTo("crlf");
			Assertions.assertThat(cache.valueFor(newFile("subfolder/MANIFEST.MF"), "eol")).isEqualTo("crlf");

			// write out a .gitattributes for the subfolder
			setFile("subfolder/.gitattributes").toContent("* eol=lf");

			// it shouldn't change anything, because it's cached
			Assertions.assertThat(cache.valueFor(newFile("someFile"), "eol")).isEqualTo("lf");
			Assertions.assertThat(cache.valueFor(newFile("subfolder/someFile"), "eol")).isEqualTo("lf");
			Assertions.assertThat(cache.valueFor(newFile("MANIFEST.MF"), "eol")).isEqualTo("crlf");
			Assertions.assertThat(cache.valueFor(newFile("subfolder/MANIFEST.MF"), "eol")).isEqualTo("crlf");
		}
		{
			// but if we make a new cache, it should change
			GitAttributesLineEndings.AttributesCache cache = new GitAttributesLineEndings.AttributesCache();
			Assertions.assertThat(cache.valueFor(newFile("someFile"), "eol")).isEqualTo("lf");
			Assertions.assertThat(cache.valueFor(newFile("subfolder/someFile"), "eol")).isEqualTo("lf");
			Assertions.assertThat(cache.valueFor(newFile("MANIFEST.MF"), "eol")).isEqualTo("crlf");
			Assertions.assertThat(cache.valueFor(newFile("subfolder/MANIFEST.MF"), "eol")).isEqualTo("lf");
		}
	}

	@Test
	void policyTest() throws IOException {
		setFile(".gitattributes").toContent(StringPrinter.buildStringFromLines(
				"* eol=lf",
				"*.MF eol=crlf"));
		LineEnding.Policy policy = LineEnding.GIT_ATTRIBUTES.createPolicy(rootFolder(), () -> testFiles());
		Assertions.assertThat(policy.getEndingFor(newFile("someFile"))).isEqualTo("\n");
		Assertions.assertThat(policy.getEndingFor(newFile("subfolder/someFile"))).isEqualTo("\n");
		Assertions.assertThat(policy.getEndingFor(newFile("MANIFEST.MF"))).isEqualTo("\r\n");
		Assertions.assertThat(policy.getEndingFor(newFile("subfolder/MANIFEST.MF"))).isEqualTo("\r\n");
	}
}
