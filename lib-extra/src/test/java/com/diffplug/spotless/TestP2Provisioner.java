/*
 * Copyright 2016-2026 DiffPlug
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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.common.base.Suppliers;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.io.Files;
import com.diffplug.spotless.extra.P2Provisioner;

public class TestP2Provisioner {
	/** Creates a P2Provisioner which will cache the result of previous calls. */
	@SuppressWarnings("unchecked")
	private static P2Provisioner caching(String name, Supplier<P2Provisioner> input) {
		File spotlessDir = new File(StandardSystemProperty.USER_DIR.value()).getParentFile();
		File testlib = new File(spotlessDir, "testlib");
		File cacheFile = new File(testlib, "build/tmp/testp2provisioner." + name + ".cache");

		Map<CacheKey, ImmutableList<File>> cached;
		if (cacheFile.exists()) {
			try (ObjectInputStream inputStream = new ObjectInputStream(Files.asByteSource(cacheFile).openBufferedStream())) {
				cached = (Map<CacheKey, ImmutableList<File>>) inputStream.readObject();
			} catch (IOException | ClassNotFoundException e) {
				throw Errors.asRuntime(e);
			}
		} else {
			cached = new HashMap<>();
			try {
				Files.createParentDirs(cacheFile);
			} catch (IOException e) {
				throw Errors.asRuntime(e);
			}
		}
		return (modelWrapper, mavenProvisioner, cacheDirectory) -> {
			CacheKey key = new CacheKey(
					List.copyOf(modelWrapper.getP2Repos()),
					List.copyOf(modelWrapper.getInstallList()),
					Set.copyOf(modelWrapper.getFilterNames()),
					List.copyOf(modelWrapper.getPureMaven()),
					modelWrapper.isUseMavenCentral(),
					cacheDirectory);

			synchronized (TestP2Provisioner.class) {
				ImmutableList<File> result = cached.get(key);
				// double-check that depcache pruning hasn't removed them since our cache cached them
				boolean needsToBeSet = result == null || !result.stream().allMatch(file -> file.exists() && file.isFile() && file.length() > 0);
				if (needsToBeSet) {
					result = ImmutableList.copyOf(input.get().provisionP2Dependencies(modelWrapper, mavenProvisioner, cacheDirectory));
					cached.put(key, result);
					try (ObjectOutputStream outputStream = new ObjectOutputStream(Files.asByteSink(cacheFile).openBufferedStream())) {
						outputStream.writeObject(cached);
					} catch (IOException e) {
						throw Errors.asRuntime(e);
					}
				}
				return result;
			}
		};
	}

	/** Creates a default P2Provisioner with caching for tests. */
	public static P2Provisioner defaultProvisioner() {
		return DEFAULT_PROVISIONER.get();
	}

	private static final Supplier<P2Provisioner> DEFAULT_PROVISIONER = Suppliers.memoize(() -> caching("default", P2Provisioner::createDefault));

	/**
	 * Cache key capturing all P2Model state that affects query results.
	 * Must be Serializable for disk caching.
	 */
	private static class CacheKey implements Serializable {
		private static final long serialVersionUID = 1L;
		private final List<String> p2Repos;
		private final List<String> installList;
		private final Set<String> filterNames;
		private final List<String> pureMaven;
		private final boolean useMavenCentral;
		@Nullable private final File cacheDirectory;

		CacheKey(List<String> p2Repos, List<String> installList, Set<String> filterNames,
				List<String> pureMaven, boolean useMavenCentral, @Nullable File cacheDirectory) {
			this.p2Repos = p2Repos;
			this.installList = installList;
			this.filterNames = filterNames;
			this.pureMaven = pureMaven;
			this.useMavenCentral = useMavenCentral;
			this.cacheDirectory = cacheDirectory;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			CacheKey cacheKey = (CacheKey) o;
			return useMavenCentral == cacheKey.useMavenCentral &&
					Objects.equals(p2Repos, cacheKey.p2Repos) &&
					Objects.equals(installList, cacheKey.installList) &&
					Objects.equals(filterNames, cacheKey.filterNames) &&
					Objects.equals(pureMaven, cacheKey.pureMaven) &&
					Objects.equals(cacheDirectory, cacheKey.cacheDirectory);
		}

		@Override
		public int hashCode() {
			return Objects.hash(p2Repos, installList, filterNames, pureMaven, useMavenCentral, cacheDirectory);
		}
	}
}
