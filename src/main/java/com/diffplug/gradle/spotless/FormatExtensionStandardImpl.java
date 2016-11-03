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
 */
abstract class FormatExtensionStandardImpl<Key extends Serializable> extends Strict<Key> {
	private static final long serialVersionUID = 1L;

	/** Transient because only the state matters. */
	final transient String name;
	/** Transient because only the state matters. */
	final transient Throwing.BiFunction<Key, String, String> formatter;

	public FormatExtensionStandardImpl(String name, Throwing.BiFunction<Key, String, String> formatter) {
		this.name = Objects.requireNonNull(name);
		this.formatter = Objects.requireNonNull(formatter);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected String format(Key key, String rawUnix, File file) throws Throwable {
		return formatter.apply(key, rawUnix);
	}

	static class Eager<Key extends Serializable> extends FormatExtensionStandardImpl<Key> {
		private static final long serialVersionUID = 1L;

		/** Transient because the key is persisted by the superclass. */
		final transient Key key;

		public Eager(String name, Key key, Throwing.BiFunction<Key, String, String> formatter) {
			super(name, formatter);
			this.key = Objects.requireNonNull(key);
		}

		@Override
		protected Key calculateKey() throws Exception {
			return key;
		}
	}

	static class Lazy<Key extends Serializable> extends FormatExtensionStandardImpl<Key> {
		private static final long serialVersionUID = 1L;

		/** Transient because the key is persisted by the superclass. */
		final transient Throwing.Supplier<Key> keySupplier;

		public Lazy(String name, Throwing.Supplier<Key> keySupplier, Throwing.BiFunction<Key, String, String> formatter) {
			super(name, formatter);
			this.keySupplier = Objects.requireNonNull(keySupplier);
		}

		@Override
		protected Key calculateKey() throws Throwable {
			return keySupplier.get();
		}
	}
}
