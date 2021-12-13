/*
 * Copyright 2021 DiffPlug
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
import java.io.IOException;
import java.nio.file.Files;

import com.diffplug.common.base.Errors;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCell;

/**
 * Runs the equivalent of {@code spotlessApply} on a single file.
 *
 * This is used to implement both the {@link IdeHook} and also the {@link SpotlessExtension#applyFile(File)} API.
 */
class FileApplier {
	/** Attempt to run the given Spotless task on the given file. */
	SpotlessApplyResult applyFile(SpotlessTaskImpl spotlessTask, File file) {
		if (!file.isAbsolute())
			throw new IllegalArgumentException("File must be an absolute path: " + file);
		if (!spotlessTask.getTarget().contains(file))
			return SpotlessApplyResult.OUT_OF_BOUNDS;
		try (Formatter formatter = spotlessTask.buildFormatter()) {
			if (spotlessTask.getRatchet() != null) {
				if (spotlessTask.getRatchet().isClean(spotlessTask.getProjectDir().get().getAsFile(), spotlessTask.getRootTreeSha(), file)) {
					onResult(SpotlessApplyResult.CLEAN);
					return SpotlessApplyResult.CLEAN;
				}
			}
			byte[] bytes = read(spotlessTask, file);
			PaddedCell.DirtyState dirty = PaddedCell.calculateDirtyState(formatter, file, bytes);
			if (dirty.isClean()) {
				onResult(SpotlessApplyResult.CLEAN);
				return SpotlessApplyResult.CLEAN;
			} else if (dirty.didNotConverge()) {
				onResult(SpotlessApplyResult.DID_NOT_CONVERGE);
				return SpotlessApplyResult.DID_NOT_CONVERGE;
			} else {
				onResult(SpotlessApplyResult.DIRTY);
				writeCanonical(spotlessTask, dirty, file);
				return SpotlessApplyResult.DIRTY;
			}
		} catch (IOException e) {
			onException(e);
			throw Errors.asRuntime(e);
		} finally {
			onFinished();
		}
	}

	/** Read contents of file. Provided so {@code IdeHook} can override this to read from standard input instead. */
	protected byte[] read(SpotlessTaskImpl spotlessTask, File file) throws IOException {
		return Files.readAllBytes(file.toPath());
	}

	/** Write contents of file. Provided so {@code IdeHook} can override this to write to standard output instead. */
	protected void writeCanonical(SpotlessTaskImpl spotlessTask, PaddedCell.DirtyState dirty, File file) throws IOException {
		dirty.writeCanonicalTo(file);
	}

	/** {@code IdeHook} overrides this to display status messages the IDE is expecting. */
	protected void onResult(SpotlessApplyResult result) {}

	/** {@code IdeHook} overrides this to ensure IDE is sent exception back trace. */
	protected void onException(Exception e) {}

	/** {@code IdeHook} overrides this to ensure standard input/output are closed at end of processing. */
	protected void onFinished() {}
}
