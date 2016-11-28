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
import java.io.Serializable;
import java.util.Objects;

import com.diffplug.common.base.Throwing;
import com.diffplug.gradle.spotless.FormatterStep.Strict;

/**
 * Standard implementation of FormatExtension which cleanly enforces
 * separation of serializable configuration and a pure format function.
 *
 * Not an inner-class of FormatterStep so that it can stay entirely private
 * from the API.
 */
final class FormatterStepStandardImpl<Key extends Serializable> extends Strict<Key> {

	private static final long serialVersionUID = 1L;

	/** Transient because only the key matters. */
	final transient String name;
	/** Transient because only the key matters. */
	final transient Throwing.Supplier<Key> keySupplier;
	final transient Throwing.Function<Key, Throwing.Function<String, String>> keyToFormatter;

	FormatterStepStandardImpl(String name,
			Throwing.Supplier<Key> keySupplier,
			Throwing.Function<Key, Throwing.Function<String, String>> keyToFormatter) {
		this.name = Objects.requireNonNull(name);
		this.keySupplier = Objects.requireNonNull(keySupplier);
		this.keyToFormatter = Objects.requireNonNull(keyToFormatter);
	}

	@Override
	public String getName() {
		return name;
	}

	/** Initialized lazily as a pure function of the key. */
	transient Throwing.Function<String, String> initializedFormatter;

	@Override
	protected Key calculateKey() throws Throwable {
		Key key = keySupplier.get();
		initializedFormatter = keyToFormatter.apply(key);
		return key;
	}

	@Override
	protected String format(Key key, String rawUnix, File file) throws Throwable {
		return initializedFormatter.apply(rawUnix);
	}
}
