/*
 * Copyright 2020-2023 DiffPlug
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

import javax.annotation.Nullable;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

import com.diffplug.spotless.extra.GitRatchet;

/** Gradle implementation of GitRatchet. */
public class GitRatchetGradle extends GitRatchet<File> {
	static {
		preventJGitFromCallingExecutables();
	}

	static void preventJGitFromCallingExecutables() {
		SystemReader reader = SystemReader.getInstance();
		SystemReader.setInstance(new DelegatingSystemReader(reader) {
			@Override
			public String getenv(String variable) {
				if ("PATH".equals(variable)) {
					return "";
				} else {
					return super.getenv(variable);
				}
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
			return reader.getProperty(variable);
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
