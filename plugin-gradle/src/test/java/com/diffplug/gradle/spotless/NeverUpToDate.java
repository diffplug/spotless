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

import java.io.File;
import java.util.Random;

import com.diffplug.common.base.Throwing;

/**
 * Creates trivial tasks which are always out-of-date,
 * but are still useful for testing.
 */
class NeverUpToDate extends LazyForwardingEquality<Integer> implements FormatterStep {
	private static final long serialVersionUID = 1L;

	private final transient String name;
	private final transient Throwing.Function<String, String> formatter;

	private NeverUpToDate(String name, Throwing.Function<String, String> formatter) {
		this.name = name;
		this.formatter = formatter;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String format(String raw, File file) throws Throwable {
		return formatter.apply(raw);
	}

	public static FormatterStep create(String name, Throwing.Function<String, String> formatter) {
		return new NeverUpToDate(name, formatter);
	}

	private static final Random RANDOM = new Random();

	/** Ensures that the serialized representation will never be equal to that of any other step. */
	@Override
	protected Integer calculateKey() throws Throwable {
		return RANDOM.nextInt();
	}
}
