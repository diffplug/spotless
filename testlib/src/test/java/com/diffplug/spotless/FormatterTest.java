/*
 * Copyright 2016-2024 DiffPlug
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
package com.diffplug.spotless;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.spotless.generic.EndWithNewlineStep;

class FormatterTest {
	@Test
	void toUnix() {
		Assertions.assertEquals("1\n2\n3", LineEnding.toUnix("1\n2\n3"));
		Assertions.assertEquals("1\n2\n3", LineEnding.toUnix("1\r2\r3"));
		Assertions.assertEquals("1\n2\n3", LineEnding.toUnix("1\r\n2\r\n3"));
	}

	// Formatter normally needs to be closed, but no resources will be leaked in this special case
	@Test
	void equality() {
		new SerializableEqualityTester() {
			private LineEnding.Policy lineEndingsPolicy = LineEnding.UNIX.createPolicy();
			private Charset encoding = StandardCharsets.UTF_8;
			private List<FormatterStep> steps = new ArrayList<>();

			@Override
			protected void setupTest(API api) throws Exception {
				api.areDifferentThan();

				lineEndingsPolicy = LineEnding.WINDOWS.createPolicy();
				api.areDifferentThan();

				encoding = StandardCharsets.UTF_16;
				api.areDifferentThan();

				steps.add(EndWithNewlineStep.create());
				api.areDifferentThan();
			}

			@Override
			protected Formatter create() {
				return Formatter.builder()
						.lineEndingsPolicy(lineEndingsPolicy)
						.encoding(encoding)
						.steps(steps)
						.build();
			}
		}.testEquals();
	}

	// If there is no File actually holding the content, one may rely on Formatter.NO_FILE_ON_DISK
	@Test
	public void testExceptionWithSentinelNoFileOnDisk() throws Exception {
		LineEnding.Policy lineEndingsPolicy = LineEnding.UNIX.createPolicy();
		Charset encoding = StandardCharsets.UTF_8;
		FormatExceptionPolicy exceptionPolicy = FormatExceptionPolicy.failOnlyOnError();

		Path rootDir = Paths.get(StandardSystemProperty.USER_DIR.value());

		FormatterStep step = Mockito.mock(FormatterStep.class);
		Mockito.when(step.getName()).thenReturn("someFailingStep");
		Mockito.when(step.format(Mockito.anyString(), Mockito.any(File.class))).thenThrow(new IllegalArgumentException("someReason"));
		List<FormatterStep> steps = Collections.singletonList(step);

		Formatter formatter = Formatter.builder()
				.lineEndingsPolicy(lineEndingsPolicy)
				.encoding(encoding)
				.steps(steps)
				.exceptionPolicy(exceptionPolicy)
				.build();

		formatter.compute("someFileContent", Formatter.NO_FILE_SENTINEL);
	}

	// rootDir may be a path not from the default FileSystem
	@Test
	public void testExceptionWithRootDirIsNotFileSystem() throws Exception {
		LineEnding.Policy lineEndingsPolicy = LineEnding.UNIX.createPolicy();
		Charset encoding = StandardCharsets.UTF_8;
		FormatExceptionPolicy exceptionPolicy = FormatExceptionPolicy.failOnlyOnError();

		Path rootDir = Mockito.mock(Path.class);
		FileSystem customFileSystem = Mockito.mock(FileSystem.class);
		Mockito.when(rootDir.getFileSystem()).thenReturn(customFileSystem);

		Path pathFromFile = Mockito.mock(Path.class);
		Mockito.when(customFileSystem.getPath(Mockito.anyString())).thenReturn(pathFromFile);

		Path relativized = Mockito.mock(Path.class);
		Mockito.when(rootDir.relativize(Mockito.any(Path.class))).then(invok -> {
			Path filePath = invok.getArgument(0);
			if (filePath.getFileSystem() == FileSystems.getDefault()) {
				throw new IllegalArgumentException("Can not relativize through different FileSystems");
			}

			return relativized;
		});

		FormatterStep step = Mockito.mock(FormatterStep.class);
		Mockito.when(step.getName()).thenReturn("someFailingStep");
		Mockito.when(step.format(Mockito.anyString(), Mockito.any(File.class))).thenThrow(new IllegalArgumentException("someReason"));
		List<FormatterStep> steps = Collections.singletonList(step);

		Formatter formatter = Formatter.builder()
				.lineEndingsPolicy(lineEndingsPolicy)
				.encoding(encoding)
				.steps(steps)
				.exceptionPolicy(exceptionPolicy)
				.build();

		formatter.compute("someFileContent", new File("/some/folder/some.file"));
	}

}
