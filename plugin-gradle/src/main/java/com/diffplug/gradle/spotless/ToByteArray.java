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

import java.io.Serializable;

interface ToByteArray extends Serializable {
	/**
	 * Returns a byte array representation of everything inside this `SerializableFileFilter`.
	 *
	 * The main purpose of this interface is to allow other interfaces to extend it, which in turn
	 * ensures that one can't instantiate the extending interfaces with lambda expressions, which are
	 * notoriously difficult to serialize and deserialize properly. (See
	 * `SerializableFileFilterImpl.SkipFilesNamed` for an example of how to make a serializable
	 * subclass.)
	 */
	byte[] toBytes();
}
