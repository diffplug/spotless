/*
 * Copyright 2024 DiffPlug
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

/**
 * Formatter which is equal to itself, but not to any other Formatter.
 */
public class NeverUpToDateStep implements FormatterStep {
	/**
	 * @param name
	 *             The name of the formatter step
	 * @param function
	 *             The function used by the formatter step
	 * @return A FormatterStep which will never report that it is up-to-date, because
	 *         it is not equal to the serialized representation of itself.
	 */
	public static FormatterStep create(
			String name,
			SerializedFunction<String, String> function) {
		return new NeverUpToDateStep(name, function);
	}

	private static final long serialVersionUID = 1L;

	private final String name;
	private final SerializedFunction<String, String> formatter; // initialized lazily

	NeverUpToDateStep(String name, SerializedFunction<String, String> formatter) {
		this.name = name;
		this.formatter = formatter;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String format(String rawUnix, File file) throws Exception {
		return formatter.apply(rawUnix);
	}

	@Override
	public void close() throws Exception {
		if (formatter instanceof FormatterFunc.Closeable) {
			((FormatterFunc.Closeable) formatter).close();
		}
	}
}
