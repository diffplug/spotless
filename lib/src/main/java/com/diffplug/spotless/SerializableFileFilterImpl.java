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
import java.util.Objects;

class SerializableFileFilterImpl {
	static class SkipFilesNamed implements SerializableFileFilter {
		private static final long serialVersionUID = 1L;

		private final String nameToSkip;

		SkipFilesNamed(String nameToSkip) {
			this.nameToSkip = Objects.requireNonNull(nameToSkip);
		}

		@Override
		public boolean accept(File pathname) {
			return !pathname.getName().equals(nameToSkip);
		}

		@Override
		public byte[] toBytes() {
			return LazyForwardingEquality.toBytes(this);
		}
	}
}
