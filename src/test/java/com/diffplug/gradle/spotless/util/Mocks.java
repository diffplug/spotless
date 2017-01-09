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
package com.diffplug.gradle.spotless.util;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

public final class Mocks {
	private Mocks() {}

	public static IncrementalTaskInputs mockIncrementalTaskInputs(Iterable<File> target) {
		return new IncrementalTaskInputs() {
			@Override
			public boolean isIncremental() {
				return false;
			}

			@Override
			public void outOfDate(Action<? super InputFileDetails> action) {
				for (File file : target) {
					action.execute(mockInputFileDetails(file));
				}
			}

			@Override
			public void removed(Action<? super InputFileDetails> action) {
				// do nothing
			}
		};
	}

	private static InputFileDetails mockInputFileDetails(File file) {
		return new InputFileDetails() {
			@Override
			public boolean isAdded() {
				return false;
			}

			@Override
			public boolean isModified() {
				return false;
			}

			@Override
			public boolean isRemoved() {
				return false;
			}

			@Override
			public File getFile() {
				return file;
			}
		};
	}
}
