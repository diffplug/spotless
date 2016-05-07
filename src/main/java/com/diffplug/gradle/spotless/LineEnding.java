/*
 * Copyright 2016 DiffPlug
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

/** The line endings written by the tool. */
public enum LineEnding {
	PLATFORM_NATIVE, WINDOWS, UNIX, DERIVED, UNCERTAIN;

	// Must not be set to UNCERTAIN
	public static final LineEnding DEFAULT = PLATFORM_NATIVE;

	// Must not be set to DERIVED or UNCERTAIN
	public static final LineEnding DERIVED_FALLBACK = PLATFORM_NATIVE;
}
