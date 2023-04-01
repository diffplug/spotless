/*
 * Copyright 2022 DiffPlug
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
package com.diffplug.spotless.glue.ktfmt;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class KtfmtFormattingOptions {

	@Nullable
	private Integer maxWidth;

	@Nullable
	private Integer blockIndent;

	@Nullable
	private Integer continuationIndent;

	@Nullable
	private Boolean removeUnusedImport;

	public KtfmtFormattingOptions(
			@Nullable Integer maxWidth,
			@Nullable Integer blockIndent,
			@Nullable Integer continuationIndent,
			@Nullable Boolean removeUnusedImport) {
		this.maxWidth = maxWidth;
		this.blockIndent = blockIndent;
		this.continuationIndent = continuationIndent;
		this.removeUnusedImport = removeUnusedImport;
	}

	@Nonnull
	public Optional<Integer> getMaxWidth() {
		return Optional.ofNullable(maxWidth);
	}

	@Nonnull
	public Optional<Integer> getBlockIndent() {
		return Optional.ofNullable(blockIndent);
	}

	@Nonnull
	public Optional<Integer> getContinuationIndent() {
		return Optional.ofNullable(continuationIndent);
	}

	@Nonnull
	public Optional<Boolean> getRemoveUnusedImport() {
		return Optional.ofNullable(removeUnusedImport);
	}

	public void setMaxWidth(int maxWidth) {
		if (maxWidth <= 0) {
			throw new IllegalArgumentException("Max width cannot be negative value or 0");
		}
		this.maxWidth = maxWidth;
	}

	public void setBlockIndent(int blockIndent) {
		if (blockIndent < 0) {
			throw new IllegalArgumentException("Block indent cannot be negative value");
		}
		this.blockIndent = blockIndent;
	}

	public void setContinuationIndent(int continuationIndent) {
		if (continuationIndent < 0) {
			throw new IllegalArgumentException("Continuation indent cannot be negative value");
		}
		this.continuationIndent = continuationIndent;
	}

	public void setRemoveUnusedImport(boolean removeUnusedImport) {
		this.removeUnusedImport = removeUnusedImport;
	}
}
