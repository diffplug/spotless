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

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.diffplug.spotless.Provisioner;

public class SpotlessActionContext {

	private final TargetFileTypeInferer.TargetFileType targetFileType;
	private final FileResolver fileResolver;
	private final ExecutionLayout executionLayout;

	private SpotlessActionContext(@Nonnull TargetFileTypeInferer.TargetFileType targetFileType, @Nonnull FileResolver fileResolver, @Nonnull SpotlessCommandLineStream commandLineStream) {
		this.targetFileType = Objects.requireNonNull(targetFileType);
		this.fileResolver = Objects.requireNonNull(fileResolver);
		this.executionLayout = ExecutionLayout.create(fileResolver, Objects.requireNonNull(commandLineStream));
	}

	@Nonnull
	public TargetFileTypeInferer.TargetFileType targetFileType() {
		return targetFileType;
	}

	public File resolveFile(File file) {
		return fileResolver.resolveFile(file);
	}

	public Path resolvePath(Path path) {
		return fileResolver.resolvePath(path);
	}

	public Provisioner provisioner() {
		return CliJarProvisioner.INSTANCE;
	}

	public ExecutionLayout executionLayout() {
		return executionLayout;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private TargetFileTypeInferer.TargetFileType targetFileType;
		private FileResolver fileResolver;
		private SpotlessCommandLineStream commandLineStream;

		public Builder targetFileType(TargetFileTypeInferer.TargetFileType targetFileType) {
			this.targetFileType = targetFileType;
			return this;
		}

		public Builder fileResolver(FileResolver fileResolver) {
			this.fileResolver = fileResolver;
			return this;
		}

		public Builder commandLineStream(SpotlessCommandLineStream commandLineStream) {
			this.commandLineStream = commandLineStream;
			return this;
		}

		public SpotlessActionContext build() {
			return new SpotlessActionContext(targetFileType, fileResolver, commandLineStream);
		}
	}
}
