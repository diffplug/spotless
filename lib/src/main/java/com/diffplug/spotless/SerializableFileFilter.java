/*
 * Copyright 2016-2023 DiffPlug
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

import java.io.FileFilter;
import java.io.Serializable;

/** A file filter with full support for serialization. */
public interface SerializableFileFilter extends FileFilter, Serializable, NoLambda {
	/** Creates a FileFilter which will accept all files except files with the given name(s). */
	static SerializableFileFilter skipFilesNamed(String... names) {
		return new SerializableFileFilterImpl.SkipFilesNamed(names);
	}
}
