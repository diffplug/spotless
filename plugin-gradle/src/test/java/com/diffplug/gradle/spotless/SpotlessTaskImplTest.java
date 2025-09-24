/*
 * Copyright 2023-2025 DiffPlug
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
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.diffplug.spotless.Formatter;

public class SpotlessTaskImplTest {
	@Test
	public void testThrowsMessageContainsFilename() throws Exception {
		SpotlessTaskImpl task = Mockito.mock(SpotlessTaskImpl.class, Mockito.CALLS_REAL_METHODS);
		Mockito.when(task.getLogger()).thenReturn(Mockito.mock(Logger.class));

		File projectDir = Path.of("unitTests", "projectDir").toFile();
		DirectoryProperty projectDirProperty = Mockito.mock(DirectoryProperty.class, Mockito.RETURNS_DEEP_STUBS);
		Mockito.when(projectDirProperty.get().getAsFile()).thenReturn(projectDir);

		Mockito.when(task.getProjectDir()).thenReturn(projectDirProperty);

		File input = Path.of("unitTests", "projectDir", "someInput").toFile();
		Formatter formatter = Mockito.mock(Formatter.class);

		Assertions.assertThatThrownBy(() -> task.processInputFile(null, formatter, input, "someInput")).hasMessageContaining(input.toString());
	}
}
