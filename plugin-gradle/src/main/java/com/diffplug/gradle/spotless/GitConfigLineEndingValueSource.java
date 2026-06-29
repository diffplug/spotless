/*
 * Copyright 2025-2026 DiffPlug
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
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig.AutoCRLF;
import org.eclipse.jgit.lib.CoreConfig.EOL;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;

import com.diffplug.common.base.Errors;
import com.diffplug.spotless.LineEnding;

/**
 * A Gradle {@link ValueSource} that resolves the default line ending from git config.
 *
 * <p>File reads inside {@code obtain()} are not tracked as configuration cache inputs;
 * only the returned value is fingerprinted. This prevents {@code ~/.gitconfig} changes
 * (e.g. CI-injected auth tokens) from invalidating the configuration cache, while still
 * correctly invalidating when the resolved line ending actually changes.
 */
public abstract class GitConfigLineEndingValueSource implements ValueSource<LineEnding.Policy, GitConfigLineEndingValueSource.Params> {

	public interface Params extends ValueSourceParameters {
		DirectoryProperty getProjectDir();
	}

	@Override
	public @Nullable LineEnding.Policy obtain() {
		File projectDir = getParameters().getProjectDir().get().getAsFile();

		FS.DETECTED.setGitSystemConfig(new File("no-global-git-config-for-spotless"));

		FileBasedConfig systemConfig = SystemReader.getInstance().openSystemConfig(null, FS.DETECTED);
		Errors.log().run(systemConfig::load);
		FileBasedConfig userConfig = SystemReader.getInstance().openUserConfig(systemConfig, FS.DETECTED);
		Errors.log().run(userConfig::load);

		// Read repo-specific config if we're in a git repo
		Config config = userConfig;
		File gitDir = findGitDir(projectDir);
		if (gitDir != null) {
			FileBasedConfig repoConfig = new FileBasedConfig(userConfig, new File(gitDir, Constants.CONFIG), FS.DETECTED);
			Errors.log().run(repoConfig::load);
			config = repoConfig;
		}

		return defaultLineEnding(config).createPolicy();
	}

	/** Walks up from projectDir looking for a .git directory. */
	private static @Nullable File findGitDir(File dir) {
		while (dir != null) {
			File dotGit = new File(dir, Constants.DOT_GIT);
			if (dotGit.isDirectory()) {
				return dotGit;
			}
			dir = dir.getParentFile();
		}
		return null;
	}

	private static LineEnding defaultLineEnding(Config config) {
		AutoCRLF autoCRLF = config.getEnum(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_AUTOCRLF, AutoCRLF.FALSE);
		if (autoCRLF == AutoCRLF.TRUE) {
			return LineEnding.WINDOWS;
		} else if (autoCRLF == AutoCRLF.INPUT) {
			return LineEnding.UNIX;
		} else if (autoCRLF == AutoCRLF.FALSE) {
			EOL eol = config.getEnum(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_EOL, EOL.NATIVE);
			switch (eol) {
			case CRLF:
				return LineEnding.WINDOWS;
			case LF:
				return LineEnding.UNIX;
			case NATIVE:
				return LineEnding.PLATFORM_NATIVE;
			default:
				throw new IllegalArgumentException("Unknown eol " + eol);
			}
		} else {
			throw new IllegalStateException("Unexpected value for autoCRLF " + autoCRLF);
		}
	}
}
