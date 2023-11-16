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
package com.diffplug.spotless.extra;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.jgit.util.SystemReader;

import com.diffplug.common.base.Errors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility methods for Git workarounds.
 */
public final class GitWorkarounds {
	private GitWorkarounds() {}

	/**
	 * Finds the .git directory for the given project directory.
	 * <p>
	 * Ordinarily one would just use JGit for this, but it doesn't support worktrees properly.
	 * So this applies an additional workaround for that.
	 *
	 * @param projectDir the project directory.
	 * @return the path to the .git directory.
	 */
	static @Nullable File getDotGitDir(File projectDir) {
		return fileRepositoryResolverForProject(projectDir).getGitDir();
	}

	/**
	 * Creates a {@link RepositorySpecificResolver} for the given project directory.
	 * <p>
	 * This applies a workaround for JGit not supporting worktrees properly.
	 *
	 * @param projectDir the project directory.
	 * @return the builder.
	 */
	static RepositorySpecificResolver fileRepositoryResolverForProject(File projectDir) {
		return fileRepositoryResolverForProject(projectDir, null);
	}

	/**
	 * Creates a {@link RepositorySpecificResolver} for the given project directory.
	 * <p>
	 * This applies a workaround for JGit not supporting worktrees properly.
	 *
	 * @param projectDir the project directory.
	 * @param baseConfig the user and system level git config.
	 * @return the builder.
	 */
	static RepositorySpecificResolver fileRepositoryResolverForProject(File projectDir, @Nullable Config baseConfig) {
		RepositorySpecificResolver repositoryResolver = new RepositorySpecificResolver(baseConfig);
		repositoryResolver.findGitDir(projectDir);
		repositoryResolver.readEnvironment();
		if (repositoryResolver.getGitDir() != null || repositoryResolver.getWorkTree() != null) {
			Errors.rethrow().get(repositoryResolver::setup);
		}
		return repositoryResolver;
	}

	/**
	 * Piggyback on the {@link FileRepositoryBuilder} mechanics for finding the git directory.
	 * <p>
	 * Here we take into account that git repositories can share a common directory. This directory
	 * will contain ./config ./objects/, ./info/, and ./refs/.
	 */
	static class RepositorySpecificResolver extends FileRepositoryBuilder {
		/**
		 * The common directory file is used to define $GIT_COMMON_DIR if environment variable is not set.
		 * https://github.com/git/git/blob/b23dac905bde28da47543484320db16312c87551/Documentation/gitrepository-layout.txt#L259
		 */
		private static final String COMMON_DIR = "commondir";
		private static final String GIT_COMMON_DIR_ENV_KEY = "GIT_COMMON_DIR";

		/**
		 * Using an extension it is possible to have per-worktree config.
		 * https://github.com/git/git/blob/b23dac905bde28da47543484320db16312c87551/Documentation/git-worktree.txt#L366
		 */
		private static final String EXTENSIONS_WORKTREE_CONFIG = "worktreeConfig";
		private static final String EXTENSIONS_WORKTREE_CONFIG_FILENAME = "config.worktree";

		private File commonDirectory;

		private Config baseConfig;

		public RepositorySpecificResolver() {
			this(null);
		}

		public RepositorySpecificResolver(@Nullable Config baseConfig) {
			this.baseConfig = baseConfig;
		}

		/** @return the repository specific configuration. */
		Config getRepositoryConfig() {
			return Errors.rethrow().get(this::getConfig);
		}

		/**
		 * @return the repository's configuration.
		 * @throws IOException on errors accessing the configuration file.
		 * @throws IllegalArgumentException on malformed configuration.
		 */
		@Override
		protected Config loadConfig() throws IOException {
			if (getGitDir() != null) {
				File path = resolveWithCommonDir(Constants.CONFIG);
				FileBasedConfig cfg = null;
				if (this.baseConfig == null) {
					cfg = new FileBasedConfig(path, safeFS());
				} else {
					cfg = new FileBasedConfig(baseConfig, path, safeFS());
				}
				try {
					cfg.load();

					// Check for per-worktree config, it should be parsed after the common config
					if (cfg.getBoolean(ConfigConstants.CONFIG_EXTENSIONS_SECTION, EXTENSIONS_WORKTREE_CONFIG, false)) {
						File worktreeSpecificConfig = safeFS().resolve(getGitDir(), EXTENSIONS_WORKTREE_CONFIG_FILENAME);
						if (safeFS().exists(worktreeSpecificConfig) && safeFS().isFile(worktreeSpecificConfig)) {
							// It is important to base this on the common config, as both the common config and the per-worktree config should be used
							cfg = new FileBasedConfig(cfg, worktreeSpecificConfig, safeFS());
							try {
								cfg.load();
							} catch (ConfigInvalidException err) {
								throw new IllegalArgumentException("Failed to parse config " + worktreeSpecificConfig.getAbsolutePath(), err);
							}
						}
					}
				} catch (ConfigInvalidException err) {
					throw new IllegalArgumentException("Failed to parse config " + path.getAbsolutePath(), err);
				}
				return cfg;
			}
			return super.loadConfig();
		}

		@Override
		protected void setupGitDir() throws IOException {
			super.setupGitDir();

			// Setup common directory
			if (commonDirectory == null) {
				File commonDirFile = safeFS().resolve(getGitDir(), COMMON_DIR);
				if (safeFS().exists(commonDirFile) && safeFS().isFile(commonDirFile)) {
					byte[] content = IO.readFully(commonDirFile);
					if (content.length < 1) {
						throw emptyFile(commonDirFile);
					}

					int lineEnd = RawParseUtils.nextLF(content, 0);
					while (content[lineEnd - 1] == '\n' || (content[lineEnd - 1] == '\r' && SystemReader.getInstance().isWindows())) {
						lineEnd--;
					}
					if (lineEnd <= 1) {
						throw emptyFile(commonDirFile);
					}

					String commonPath = RawParseUtils.decode(content, 0, lineEnd);
					File common = new File(commonPath);
					if (common.isAbsolute()) {
						commonDirectory = common;
					} else {
						commonDirectory = safeFS().resolve(getGitDir(), commonPath).getCanonicalFile();
					}
				}
			}

			// Setup object directory
			if (getObjectDirectory() == null) {
				setObjectDirectory(resolveWithCommonDir(Constants.OBJECTS));
			}
		}

		private static IOException emptyFile(File commonDir) {
			return new IOException("Empty 'commondir' file: " + commonDir.getAbsolutePath());
		}

		@SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE")
		@Override
		public FileRepositoryBuilder readEnvironment(SystemReader sr) {
			super.readEnvironment(sr);

			// Always overwrite, will trump over the common dir file
			String val = sr.getenv(GIT_COMMON_DIR_ENV_KEY);
			if (val != null) {
				commonDirectory = new File(val);
			}

			return self();
		}

		/**
		 * For repository with multiple linked worktrees some data might be shared in a "common" directory.
		 *
		 * @param target the file we want to resolve.
		 * @return a file resolved from the {@link #getGitDir()}, or possibly in the path specified by $GIT_COMMON_DIR or {@code commondir} file.
		 */
		File resolveWithCommonDir(String target) {
			if (commonDirectory != null) {
				return safeFS().resolve(commonDirectory, target);
			}
			return safeFS().resolve(getGitDir(), target);
		}
	}
}
