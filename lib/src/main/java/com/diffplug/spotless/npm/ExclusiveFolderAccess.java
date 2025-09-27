/*
 * Copyright 2025 DiffPlug
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
package com.diffplug.spotless.npm;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

import com.diffplug.spotless.ThrowingEx;

interface ExclusiveFolderAccess {

	static ExclusiveFolderAccess forFolder(@Nonnull File folder) {
		return forFolder(folder.getAbsolutePath());
	}

	static ExclusiveFolderAccess forFolder(@Nonnull String path) {
		return new ExclusiveFolderAccessSharedMutex(Objects.requireNonNull(path));
	}

	void runExclusively(ThrowingEx.Runnable runnable);

	final class ExclusiveFolderAccessSharedMutex implements ExclusiveFolderAccess {

		private static final ConcurrentHashMap<String, Lock> mutexes = new ConcurrentHashMap<>();

		private final String path;

		private ExclusiveFolderAccessSharedMutex(@Nonnull String path) {
			this.path = Objects.requireNonNull(path);
		}

		private Lock getMutex() {
			return mutexes.computeIfAbsent(path, k -> new ReentrantLock());
		}

		@Override
		public void runExclusively(ThrowingEx.Runnable runnable) {
			final Lock lock = getMutex();
			try {
				lock.lock();
				runnable.run();
			} catch (Exception e) {
				throw ThrowingEx.asRuntime(e);
			} finally {
				lock.unlock();
			}
		}
	}
}
