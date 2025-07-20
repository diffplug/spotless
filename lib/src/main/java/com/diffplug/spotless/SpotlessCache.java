/*
 * Copyright 2016-2025 DiffPlug
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

import java.io.IOException;
import java.io.Serializable;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spotless' global cache. {@link SpotlessCache#clear()} should be called
 * when Spotless is no longer in use to release any resources it has grabbed.
 */
public final class SpotlessCache {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpotlessCache.class);

	/** Allows comparing keys based on their serialization. */
	static final class SerializedKey {
		final byte[] serialized;
		final int hashCode;

		SerializedKey(Serializable key) {
			Objects.requireNonNull(key);
			serialized = LazyForwardingEquality.toBytes(key);
			hashCode = Arrays.hashCode(serialized);
		}

		@Override
		public final boolean equals(Object other) {
			return other instanceof SerializedKey
					&& Arrays.equals(serialized, ((SerializedKey) other).serialized);
		}

		@Override
		public final int hashCode() {
			return hashCode;
		}
	}

	final Map<SerializedKey, URLClassLoader> cache = new HashMap<>();

	synchronized ClassLoader classloader(JarState state) {
		return classloader(state, state);
	}

	synchronized ClassLoader classloader(Serializable key, JarState state) {
		SerializedKey serializedKey = new SerializedKey(key);
		return cache
				.computeIfAbsent(serializedKey, k -> {
					LOGGER.debug("Allocating an additional FeatureClassLoader for key={} Cache.size was {}", key, cache.size());
					return new FeatureClassLoader(state.jarUrls(), this.getClass().getClassLoader());
				});
	}

	static SpotlessCache instance() {
		return instance;
	}

	/**
	 * Closes all cached classloaders.
	 */
	private static void clear() {
		List<URLClassLoader> toDelete;
		synchronized (instance) {
			toDelete = new ArrayList<>(instance.cache.values());
			instance.cache.clear();
		}
		for (URLClassLoader classLoader : toDelete) {
			try {
				classLoader.close();
			} catch (IOException e) {
				throw ThrowingEx.asRuntime(e);
			}
		}
	}

	private static volatile Object lastClear;

	/**
	 * Closes all cached classloaders iff {@code key} is not {@code .equals()} to the last call to {@code clearOnce()}.
	 * If {@code key} is null, the clear will always happen (as though null != null).
	 */
	public static boolean clearOnce(@Nullable Object key) {
		synchronized (instance) {
			if (key == null) {
				lastClear = null;
			} else if (key.equals(lastClear)) {
				// only clear once
				return false;
			}
			lastClear = key;
		}
		clear();
		return true;
	}

	private static final SpotlessCache instance = new SpotlessCache();
}
