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

/**
 * Result codes for {@link SpotlessExtension#applyFile(File)} API.
 */
public enum SpotlessApplyResult {
	/** Spotless was not configured to process this file. */
	OUT_OF_BOUNDS,
	/** File was clean (had correct formatting from the start). */
	CLEAN,
	/** Formatting of file failed to converge. */
	DID_NOT_CONVERGE,
	/** File was dirty (has been overwritten with formatted version) */
	DIRTY
}
