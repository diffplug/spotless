/*
 * Copyright 2023 DiffPlug
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

/**
 * Tracks the number of processed files, typically by a single Formatter for a whole repository
 */
class ImpactedFilesTracker {
	protected int nbskippedAsCleanCache = 0;
	protected int nbCheckedButAlreadyClean = 0;
	protected int nbCleaned = 0;

	/**
	 * Some cache mechanism may indicate some content is clean, without having to execute the cleaning process
	 */
	public void skippedAsCleanCache() {
		nbskippedAsCleanCache++;
	}

	public int getSkippedAsCleanCache() {
		return nbskippedAsCleanCache;
	}

	public void checkedButAlreadyClean() {
		nbCheckedButAlreadyClean++;
	}

	public int getCheckedButAlreadyClean() {
		return nbCheckedButAlreadyClean;
	}

	public void cleaned() {
		nbCleaned++;
	}

	public int getCleaned() {
		return nbCleaned;
	}

	public int getTotal() {
		return nbskippedAsCleanCache + nbCheckedButAlreadyClean + nbCleaned;
	}
}
