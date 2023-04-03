/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.maven;

import static com.diffplug.spotless.maven.FileLocator.TMP_RESOURCE_FILE_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Paths;

import org.codehaus.plexus.resource.ResourceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.diffplug.spotless.ResourceHarness;

class FileLocatorTest extends ResourceHarness {

	private ResourceManager resourceManager;
	private FileLocator fileLocator;

	@BeforeEach
	void beforeEach() {
		resourceManager = mock(ResourceManager.class);
		fileLocator = new FileLocator(resourceManager, rootFolder(), rootFolder());
	}

	@Test
	void locateEmptyString() {
		assertThat(fileLocator.locateFile("")).isNull();
	}

	@Test
	void locateNull() {
		assertThat(fileLocator.locateFile(null)).isNull();
	}

	@Test
	void locateXmlFile() throws Exception {
		testFileLocator(Paths.get("tmp", "configs", "my-config.xml").toString(), "xml");
	}

	@Test
	void locatePropertiesFile() throws Exception {
		testFileLocator(Paths.get("home", "ubuntu", "my-other-config.properties").toString(), "properties");
	}

	@Test
	void locateConfFileWithIncorrectSeparators() throws Exception {
		var oppositeSeparator = "/".equals(File.separator) ? "\\" : "/";
		var path = "tmp" + oppositeSeparator + "configs" + oppositeSeparator + "hello.conf";

		testFileLocator(path, "conf");
	}

	private void testFileLocator(String path, String extension) throws Exception {
		var tmpOutputFile = new File("tmp-file");
		when(resourceManager.getResourceAsFile(any(), any())).thenReturn(tmpOutputFile);

		File locatedFile = fileLocator.locateFile(path);
		assertThat(locatedFile).isEqualTo(tmpOutputFile);

		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		verify(resourceManager).getResourceAsFile(eq(path), argCaptor.capture());
		assertThat(argCaptor.getValue()).startsWith(TMP_RESOURCE_FILE_PREFIX).endsWith('.' + extension);
	}
}
