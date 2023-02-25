/*
 * Copyright 2016-2020 DiffPlug
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

import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileType;
import org.gradle.api.provider.Provider;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.InputChanges;

import com.diffplug.common.collect.Iterables;
import com.diffplug.spotless.FileSignature;

final class Tasks {
	private Tasks() {}

	static void execute(SpotlessTaskImpl task) throws Exception {
		task.performAction(mockIncrementalTaskInputs(task.getProject().getProjectDir(), task.getTarget()));
	}

	private static InputChanges mockIncrementalTaskInputs(File rootDir, Iterable<File> target) {
		return new InputChanges() {
			@Override
			public boolean isIncremental() {
				return false;
			}

			private Iterable<FileChange> getFileChanges() {
				return Iterables.transform(target, file -> mockInputFileDetails(rootDir, file));
			}

			@Override
			public Iterable<FileChange> getFileChanges(FileCollection arg0) {
				return getFileChanges();
			}

			@Override
			public Iterable<FileChange> getFileChanges(Provider<? extends FileSystemLocation> arg0) {
				return getFileChanges();
			}
		};
	}

	private static FileChange mockInputFileDetails(File rootDir, File file) {
		return new FileChange() {
			@Override
			public ChangeType getChangeType() {
				return ChangeType.MODIFIED;
			}

			@Override
			public File getFile() {
				return file;
			}

			@Override
			public FileType getFileType() {
				return FileType.FILE;
			}

			@Override
			public String getNormalizedPath() {
				String rootPath = FileSignature.pathNativeToUnix(rootDir.getAbsolutePath());
				String absPath = FileSignature.pathNativeToUnix(file.getAbsolutePath());
				return FileSignature.subpath(rootPath, absPath);
			}
		};
	}
}
