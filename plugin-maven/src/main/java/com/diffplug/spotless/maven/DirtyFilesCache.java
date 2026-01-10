package com.diffplug.spotless.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

class DirtyFilesCache {
	private final Map<Key, List<String>> cache = new ConcurrentHashMap<>();
	private static volatile DirtyFilesCache instance = new DirtyFilesCache();

	static DirtyFilesCache instance() {
		if (instance == null) {
			synchronized (DirtyFilesCache.class) {
				if (instance == null) {
					instance = new DirtyFilesCache();
				}
			}
		}
		return instance;
	}


	Iterable<String> getDirtyFiles(File baseDir, String ratchetFrom) {
		Key key = new Key(baseDir, ratchetFrom);
		return cache.computeIfAbsent(
			key,
			k -> {
				try {
					return GitRatchetMaven.instance().getDirtyFiles(baseDir, ratchetFrom);
				} catch (IOException e) {
					throw new PluginException("Unable to scan file tree rooted at " + baseDir, e);
				}
			}
		);
	}

	private static final class Key {
		private final File baseDir;
		private final String ratchetFrom;

		private Key(File baseDir, String ratchetFrom) {
			this.baseDir = baseDir;
			this.ratchetFrom = ratchetFrom;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			Key key = (Key) o;
			return Objects.equals(baseDir, key.baseDir) && Objects.equals(ratchetFrom, key.ratchetFrom);
		}

		@Override
		public int hashCode() {
			return Objects.hash(baseDir, ratchetFrom);
		}
	}
}
