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
package com.diffplug.spotless;

import java.util.function.Supplier;

/**
 * This is a utility class to allow for lazy evaluation of arguments to be passed to a logger
 * and thus avoid unnecessary computation of the arguments if the log level is not enabled.
 */
public final class LazyArgLogger {

	private final Supplier<Object> argSupplier;

	private LazyArgLogger(Supplier<Object> argSupplier) {
		this.argSupplier = argSupplier;
	}

	public static LazyArgLogger lazy(Supplier<Object> argSupplier) {
		return new LazyArgLogger(argSupplier);
	}

	@Override
	public String toString() {
		return String.valueOf(argSupplier.get());
	}
}
