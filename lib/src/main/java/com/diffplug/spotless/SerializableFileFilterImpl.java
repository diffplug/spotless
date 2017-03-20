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
package com.diffplug.spotless;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class SerializableFileFilterImpl {
	static class SkipFilesNamed extends NoLambda.EqualityBasedOnSerialization implements SerializableFileFilter {
		private static final long serialVersionUID = 1L;

		private final Set<String> namesToSkip;

		SkipFilesNamed(String... namesToSkip) {
			Objects.requireNonNull(namesToSkip);
			this.namesToSkip = new HashSet<String>(namesToSkip.length);
			Collections.addAll(this.namesToSkip, namesToSkip);
		}

		@Override
		public boolean accept(File pathname) {
			return !namesToSkip.contains(pathname.getName());
		}
	}
}
