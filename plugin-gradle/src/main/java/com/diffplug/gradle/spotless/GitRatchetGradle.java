/*
 * Copyright 2020-2025 DiffPlug
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
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

import com.diffplug.spotless.extra.GitRatchet;

/** Gradle implementation of GitRatchet. */
public class GitRatchetGradle extends GitRatchet<File> {
	private static final String[] GIT_EXEC_CANDIDATES = {"git", "git.exe", "git.cmd"};

	static {
		GitRatchetGradle.redirectJGitExecutions();
	}

	static void redirectJGitExecutions() {
		SystemReader existing = SystemReader.getInstance();
		SystemReader.setInstance(new DelegatingSystemReader(existing) {
			private AtomicReference<FileBasedConfig> systemConfig = new AtomicReference<>();

			@Override
			public StoredConfig getSystemConfig() throws ConfigInvalidException, IOException {
				FileBasedConfig c = systemConfig.get();
				if (c == null) {
					systemConfig.compareAndSet(null,
							this.openSystemConfig(this.getJGitConfig(), FS.DETECTED));
					c = systemConfig.get();
				}
				updateAll(c);
				return c;
			}

			// lifted from SystemReader since it's private
			private void updateAll(Config config) throws ConfigInvalidException, IOException {
				if (config == null) {
					return;
				}

				updateAll(config.getBaseConfig());
				if (config instanceof FileBasedConfig) {
					FileBasedConfig cfg = (FileBasedConfig) config;
					if (cfg.isOutdated()) {
						cfg.load();
					}
				}
			}

			@Override
			public FileBasedConfig openSystemConfig(final Config parent, final FS fs) {
				// cgit logic: https://git.kernel.org/pub/scm/git/git.git/tree/config.c#n1973 - in git_system_config()
				// They check the GIT_CONFIG_SYSTEM env var first, then follow up with logic based on compile-time parameters
				// We can't replicate this exactly so we'll do the closest approximation that Gradle will allow.
				final String systemPath = this.getenv("GIT_CONFIG_SYSTEM");
				if (systemPath != null) {
					fs.setGitSystemConfig(new File(systemPath).getAbsoluteFile());
					return super.openSystemConfig(parent, fs);
				}

				// match FS.searchPath
				File gitExec = null;
				final String path = this.getenv("PATH");
				if (path != null) {
					outer: for (final String p : path.split(File.pathSeparator)) {
						for (final String name : GIT_EXEC_CANDIDATES) {
							final File candidate = new File(p, name);
							if (candidate.isFile() && candidate.canExecute()) {
								gitExec = candidate.getAbsoluteFile();
								break outer;
							}
						}
					}
				}

				// Guess at common locations
				if (gitExec != null) {
					// If git exec is at <prefix>/bin/git, this returns <prefix>
					File prefix = gitExec.getParentFile().getParentFile();

					// Then we try to resolve a config
					final File systemConfig = new File(prefix, "etc/gitconfig");
					if (systemConfig.exists()) {
						fs.setGitSystemConfig(systemConfig);
						return super.openSystemConfig(parent, fs);
					}
				}

				// Fallback to the non-prefixed path (this is not the logic that cgit uses, but oh well)
				fs.setGitSystemConfig(new File("/etc/gitconfig"));
				return super.openSystemConfig(parent, fs);
			}
		});
	}

	@Override
	protected File getDir(File project) {
		return project;
	}

	@Override
	protected @Nullable File getParent(File project) {
		return project.getParentFile();
	}

	static class DelegatingSystemReader extends SystemReader {
		final SystemReader reader;

		DelegatingSystemReader(SystemReader reader) {
			this.reader = reader;
		}

		@Override
		public String getHostname() {
			return reader.getHostname();
		}

		@Override
		public String getenv(String variable) {
			return reader.getenv(variable);
		}

		@Override
		public String getProperty(String key) {
			return reader.getProperty(key);
		}

		@Override
		public FileBasedConfig openUserConfig(Config parent, FS fs) {
			return reader.openUserConfig(parent, fs);
		}

		@Override
		public FileBasedConfig openSystemConfig(Config parent, FS fs) {
			return reader.openSystemConfig(parent, fs);
		}

		@Override
		public FileBasedConfig openJGitConfig(Config parent, FS fs) {
			return reader.openJGitConfig(parent, fs);
		}

		@Override
		public long getCurrentTime() {
			return reader.getCurrentTime();
		}

		@Override
		public int getTimezone(long when) {
			return reader.getTimezone(when);
		}
	}
}
