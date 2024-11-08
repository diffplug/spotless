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

import java.util.Objects;

import javax.annotation.Nonnull;

public class SpotlessActionContext {

	private final TargetFileTypeInferer.TargetFileType targetFileType;

	public SpotlessActionContext(@Nonnull TargetFileTypeInferer.TargetFileType targetFileType) {
		this.targetFileType = Objects.requireNonNull(targetFileType);
	}

	@Nonnull
	public TargetFileTypeInferer.TargetFileType targetFileType() {
		return targetFileType;
	}
}
