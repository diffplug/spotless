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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the number of processed files, typically by a single Formatter for a whole repository
 */
public class ImpactedFilesTracker {
	protected final AtomicInteger nbSkipped = new AtomicInteger();
	protected final AtomicInteger nbChecked = new AtomicInteger();
	protected final AtomicInteger nbCleaned = new AtomicInteger();

	/**
	 * Some cache mechanism may indicate some content is clean, without having to execute the cleaning process
	 */
	public void skippedAsCleanCache() {
		nbSkipped.incrementAndGet();
	}

	public int getSkipped() {
		return nbSkipped.get();
	}

	public void checked() {
		nbChecked.incrementAndGet();
	}

	public int getChecked() {
		return nbChecked.get();
	}

	public void cleaned() {
		nbCleaned.incrementAndGet();
	}

	public int getCleaned() {
		return nbCleaned.get();
	}

}
