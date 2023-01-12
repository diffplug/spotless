/*
 * Copyright 2021-2022 DiffPlug
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.gradle.api.GradleException;
import org.gradle.api.Task;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.LazyForwardingEquality;

class JvmLocalCache {
	private static GradleException cacheIsStale() {
		return new GradleException("Spotless JVM-local cache is stale. Regenerate the cache with\n" +
				"  " + (FileSignature.machineIsWin() ? "rmdir /q /s" : "rm -rf") + " .gradle/configuration-cache\n" +
				"To make this workaround obsolete, please upvote https://github.com/diffplug/spotless/issues/987");
	}

	interface LiveCache<T> {
		T get();

		void set(T value);
	}

	static <T> LiveCache<T> createLive(Task task, String propertyName) {
		return new LiveCacheKeyImpl<T>(new InternalCacheKey(task.getProject().getProjectDir(), task.getPath(), propertyName));
	}

	static class LiveCacheKeyImpl<T> implements LiveCache<T>, Serializable {
		InternalCacheKey internalKey;

		LiveCacheKeyImpl(InternalCacheKey internalKey) {
			this.internalKey = internalKey;
		}

		@Override
		public void set(T value) {

			// whenever we cache an instance of LazyForwardingEquality, we want to make sure that we give it
			// a chance to null-out its initialization lambda (see https://github.com/diffplug/spotless/issues/1194#issuecomment-1120744842)
			LazyForwardingEquality.unlazy(value);
			daemonState.put(internalKey, value);
		}

		@Override
		public T get() {
			Object value = daemonState.get(internalKey);
			if (value == null) {
				// TODO: throw TriggerConfigurationException(); (see https://github.com/diffplug/spotless/issues/987)
				throw cacheIsStale();
			} else {
				return (T) value;
			}
		}
	}

	private static Map<InternalCacheKey, Object> daemonState = Collections.synchronizedMap(new HashMap<>());

	private static class InternalCacheKey implements Serializable {
		private File projectDir;
		private String taskPath;
		private String propertyName;

		InternalCacheKey(File projectDir, String taskPath, String keyName) {
			this.projectDir = projectDir;
			this.taskPath = taskPath;
			this.propertyName = keyName;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			InternalCacheKey that = (InternalCacheKey) o;
			return projectDir.equals(that.projectDir) && taskPath.equals(that.taskPath) && propertyName.equals(that.propertyName);
		}

		@Override
		public int hashCode() {
			return Objects.hash(projectDir, taskPath, propertyName);
		}
	}
}
