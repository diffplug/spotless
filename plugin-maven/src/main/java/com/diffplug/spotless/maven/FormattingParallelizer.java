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
package com.diffplug.spotless.maven;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCell;
import com.diffplug.spotless.PaddedCell.DirtyState;

public class FormattingParallelizer {

	public static final FormattingParallelizer INSTANCE = new FormattingParallelizer();

	private FormattingParallelizer() {}

	private final Executor readerExecutor = Executors.newFixedThreadPool(2, daemonThreadFactory());

	private final Executor formatterExecutor = Executors.newFixedThreadPool(
			Runtime.getRuntime().availableProcessors() * 2, daemonThreadFactory());

	private final Executor writerExecutor = Executors.newFixedThreadPool(2, daemonThreadFactory());

	CompletableFuture<Void> format(File file, Formatter formatter) {
		return readFileContent(file)
				.thenApplyAsync(raw -> calculateDirtyState(file, formatter, raw), formatterExecutor)
				.thenAcceptAsync(dirtyState -> writeFormatted(dirtyState, file), writerExecutor);
	}

	private CompletableFuture<byte[]> readFileContent(File file) {
		return supplyAsync(
				() -> {
					try {
						return Files.readAllBytes(file.toPath());
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				},
				readerExecutor);
	}

	private DirtyState calculateDirtyState(File file, Formatter formatter, byte[] rawBytes) {
		try {
			return PaddedCell.calculateDirtyState(formatter, file, rawBytes);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void writeFormatted(DirtyState dirtyState, File file) {
		if (!dirtyState.isClean() && !dirtyState.didNotConverge()) {
			try {
				dirtyState.writeCanonicalTo(file);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private static ThreadFactory daemonThreadFactory() {
		return runnable -> {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);
			return thread;
		};
	}
}
