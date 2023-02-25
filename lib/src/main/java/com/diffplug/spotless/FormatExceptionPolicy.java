/*
 * Copyright 2016-2021 DiffPlug
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

import java.io.Serializable;

/** A policy for handling exceptions in the format. */
public interface FormatExceptionPolicy extends Serializable, NoLambda {
	/** Called for every error in the formatter. */
	public void handleError(Throwable e, FormatterStep step, String relativePath);

	/**
	 * Returns a byte array representation of everything inside this {@code FormatExceptionPolicy}.
	 *
	 * The main purpose of this method is to ensure one can't instantiate this class with lambda
	 * expressions, which are notoriously difficult to serialize and deserialize properly.
	 */
	public byte[] toBytes();

	/**
	 * A policy which rethrows subclasses of {@code Error} and logs other kinds of Exception.
	 */
	public static FormatExceptionPolicy failOnlyOnError() {
		return new FormatExceptionPolicyLegacy();
	}
}
