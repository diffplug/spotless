/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.cli.core;

import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nonnull;

public class TargetFileTypeInferer {

	private final TargetResolver targetResolver;

	public TargetFileTypeInferer(TargetResolver targetResolver) {
		this.targetResolver = Objects.requireNonNull(targetResolver);
	}

	public TargetFileType inferTargetFileType() {
		return targetResolver.resolveTargets()
				.limit(5) // only check the first n files
				.map(this::inferFileType)
				.reduce(this::reduceFileType)
				.orElseGet(TargetFileType::unknown);
	}

	private TargetFileType reduceFileType(TargetFileType fileType1, TargetFileType fileType2) {
		if (Objects.equals(fileType1, fileType2)) {
			return fileType1;
		}
		return TargetFileType.unknown();
	}

	private TargetFileType inferFileType(@Nonnull java.nio.file.Path path) {
		String fileName = path.getFileName().toString();
		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex == -1) {
			return TargetFileType.unknown();
		}
		String fileExtension = fileName.substring(lastDotIndex + 1);
		return new TargetFileType(fileExtension);
	}

	public final static class TargetFileType {
		private final String fileExtension;

		private TargetFileType(String fileExtension) {
			this.fileExtension = fileExtension;
		}

		public String fileExtension() {
			return fileExtension;
		}

		public FileType fileType() {
			return FileType.fromFileExtension(fileExtension);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			TargetFileType that = (TargetFileType) o;
			return Objects.equals(fileExtension, that.fileExtension);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(fileExtension);
		}

		static TargetFileType unknown() {
			return new TargetFileType(null);
		}

		static TargetFileType fromExtension(String fileExtension) {
			return new TargetFileType(fileExtension);
		}
	}

	public enum FileType {
		JAVA, CPP, ANTLR4("g4"), GROOVY, PROTOBUF("proto"), KOTLIN("kt"), UNDETERMINED("");

		private final String fileExtensionOverride;

		FileType() {
			this.fileExtensionOverride = null;
		}

		FileType(String fileExtensionOverride) {
			this.fileExtensionOverride = fileExtensionOverride;
		}

		public String fileExtension() {
			return fileExtensionOverride == null ? name().toLowerCase() : fileExtensionOverride;
		}

		public static FileType fromFileExtension(String fileExtension) {
			if (fileExtension == null || fileExtension.isEmpty()) {
				return UNDETERMINED;
			}
			return Arrays.stream(values())
					.filter(fileType -> fileType.fileExtension().equalsIgnoreCase(fileExtension))
					.findFirst()
					.orElse(UNDETERMINED);
		}
	}
}
