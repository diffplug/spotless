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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.eclipse.jgit.attributes.Attribute;
import org.eclipse.jgit.attributes.AttributesNode;
import org.eclipse.jgit.attributes.AttributesNodeProvider;
import org.eclipse.jgit.attributes.AttributesRule;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig.EOL;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Unhandled;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Uses [.gitattributes](https://git-scm.com/docs/gitattributes) to determine
 * the appropriate line ending. Falls back to the `core.eol` property in the
 * git config if there are no applicable git attributes, then finally falls
 * back to the platform native.
 */
class GitAttributesLineEndings {

	public static Policy create(File rootFolder) {
		return new Policy(rootFolder);
	}

	static class Policy extends LazyForwardingEquality<FileState> implements LineEnding.Policy {
		private static final long serialVersionUID = 1L;

		final transient File rootFolder;

		Policy(File rootFolder) {
			this.rootFolder = Objects.requireNonNull(rootFolder);
		}

		@Override
		protected FileState calculateKey() throws Throwable {
			return new FileState(rootFolder);
		}

		/**
		 * Initializing the key() for up-to-date checking is faster than the full initialization
		 * needed to actually do the formatting. We load the Runtime lazily from the key().
		 */
		transient Runtime runtime;

		@Override
		public String getEndingFor(File file) {
			if (runtime == null) {
				runtime = Errors.rethrow().get(key()::atRuntime);
			}
			return runtime.getEndingFor(file);
		}
	}

	static class FileState implements Serializable {
		private static final long serialVersionUID = 1L;

		/**
		 * The root folder of the repository, if it exists.
		 *
		 * Transient because not needed to uniquely identify a FileState instance, and also because
		 * Gradle only needs this class to be Serializable so it can compare FileState instances for
		 * incremental builds.
		 */
		@Nullable
		transient final File workTree;

		/** The signature of *all* of the files below. */
		final FileSignature signature;

		/**
		 * .git/config (per-repo), might not exist if we're not in a git repo.
		 *
		 * Transient because not needed to uniquely identify a FileState instance...
		 */
		@Nullable
		transient final File repoConfig;

		/**
		 * /etc/gitconfig (system-global), might not exist.
		 *
		 * Transient because not needed to uniquely identify a FileState instance...
		 */
		@Nullable
		@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
		transient final File systemConfig;

		/**
		 * ~/.gitconfig (per-user), might not exist.
		 *
		 * Transient because not needed to uniquely identify a FileState instance...
		 */
		@Nullable
		@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
		transient final File userConfig;

		/**
		 * All the .gitattributes files in the work tree that we're formatting.
		 *
		 * Transient because not needed to uniquely identify a FileState instance...
		 */
		@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
		transient final List<File> gitattributes;

		FileState(File rootFolder) throws IOException {
			FS fs = FS.detect();
			this.systemConfig = fs.getGitSystemConfig();
			File userConfig = new File(fs.userHome(), ".gitconfig");
			this.userConfig = userConfig.exists() ? userConfig : null;

			FileRepositoryBuilder builder = new FileRepositoryBuilder();
			builder.findGitDir(rootFolder);
			if (builder.getGitDir() != null) {
				workTree = builder.getWorkTree();
				File repoConfig = new File(builder.getGitDir(), Constants.INFO_ATTRIBUTES);
				if (repoConfig.exists()) {
					this.repoConfig = repoConfig;
				} else {
					this.repoConfig = null;
				}
			} else {
				this.repoConfig = null;
				this.workTree = null;
			}
			// TODO: efficiently walk the rootFolder to find all .gitattributes files in the folder
			// Maybe we should take advantage of gradle's FileCollection here?  Find all tips, then
			// work towards the root in the fileTree
			this.gitattributes = Collections.emptyList();

			List<File> toIndex = new ArrayList<>(gitattributes.size() + 3);
			toIndex.addAll(gitattributes);
			if (systemConfig != null) {
				toIndex.add(systemConfig);
			}
			if (userConfig != null) {
				toIndex.add(userConfig);
			}
			if (repoConfig != null) {
				toIndex.add(repoConfig);
			}
			signature = new FileSignature(toIndex);
		}

		private Runtime atRuntime() throws IOException {
			if (workTree != null) {
				FileRepositoryBuilder builder = new FileRepositoryBuilder();
				builder.findGitDir(workTree);
				Objects.requireNonNull(builder.getGitDir(), "If we found a workTree, there should def be a git dir");
				Repository repo = builder.build();
				AttributesNodeProvider nodeProvider = repo.createAttributesNodeProvider();
				Function<AttributesNode, List<AttributesRule>> getRules = node -> node == null ? Collections.emptyList() : node.getRules();
				return new Runtime(repo.getConfig(),
						getRules.apply(nodeProvider.getInfoAttributesNode()), repo.getWorkTree(),
						getRules.apply(nodeProvider.getGlobalAttributesNode()));
			} else {
				// there's no repo, so it takes some work to grab the system-wide values
				Config systemConfig = SystemReader.getInstance().openSystemConfig(null, FS.DETECTED);
				Config userConfig = SystemReader.getInstance().openUserConfig(systemConfig, FS.DETECTED);
				if (userConfig == null) {
					userConfig = new Config();
				}

				List<AttributesRule> globalRules = Collections.emptyList();
				// copy-pasted from org.eclipse.jgit.lib.CoreConfig
				String globalAttributesPath = userConfig.getString(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_ATTRIBUTESFILE);
				// copy-pasted from org.eclipse.jgit.internal.storage.file.GlobalAttributesNode
				if (globalAttributesPath != null) {
					FS fs = FS.detect();
					File attributesFile;
					if (globalAttributesPath.startsWith("~/")) { //$NON-NLS-1$
						attributesFile = fs.resolve(fs.userHome(), globalAttributesPath.substring(2));
					} else {
						attributesFile = fs.resolve(null, globalAttributesPath);
					}
					globalRules = parseRules(attributesFile);
				}
				return new Runtime(userConfig,
						// no git info file
						Collections.emptyList(), null,
						globalRules);
			}
		}
	}

	static class Runtime {
		/** .git/info/attributes (and the worktree with that file) */
		final List<AttributesRule> infoRules;
		final File workTree; // nullable

		/** Cache of local .gitattributes files. */
		final AttributesCache cache = new AttributesCache();

		/** Global .gitattributes file. */
		final List<AttributesRule> globalRules;

		/**
		 * Default line ending, determined in this order (paths are a teensy different platform to platform).
		 *
		 * - .git/config (per-repo)
		 * - ~/.gitconfig (per-user)
		 * - /etc/gitconfig (system-wide)
		 * - <platform native>
		 */
		final String defaultEnding;

		private Runtime(Config config, List<AttributesRule> infoRules, File workTree, List<AttributesRule> globalRules) {
			this.infoRules = Objects.requireNonNull(infoRules);
			this.workTree = workTree;
			this.globalRules = Objects.requireNonNull(globalRules);
			this.defaultEnding = fromEol(config.getEnum(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_EOL, EOL.NATIVE)).str();
		}

		private static final String KEY_EOL = "eol";
		private static final boolean IS_FOLDER = false;

		public String getEndingFor(File file) {
			// handle the .gitinfo worktree file
			if (workTree != null && !infoRules.isEmpty()) {
				String rootPath = workTree.getAbsolutePath();
				String path = file.getAbsolutePath();
				if (path.startsWith(rootPath)) {
					String subpath = path.substring(rootPath.length() + 1);
					String infoResult = findAttributeInRules(subpath, IS_FOLDER, KEY_EOL, infoRules);
					if (infoResult != null) {
						return convertEolToLineEnding(infoResult, file);
					}
				}
			}

			// handle the local .gitattributes (if any)
			String localResult = cache.valueFor(file, KEY_EOL);
			if (localResult != null) {
				return convertEolToLineEnding(localResult, file);
			}

			// handle the global .gitattributes
			String globalResult = findAttributeInRules(file.getAbsolutePath(), IS_FOLDER, KEY_EOL, globalRules);
			if (globalResult != null) {
				return convertEolToLineEnding(globalResult, file);
			}

			// if all else fails, use the default value
			return defaultEnding;
		}

		private static String convertEolToLineEnding(String eol, File file) {
			switch (eol.toLowerCase(Locale.ROOT)) {
			case "lf":
				return LineEnding.UNIX.str();
			case "crlf":
				return LineEnding.WINDOWS.str();
			default:
				System.err.println(".gitattributes file has unspecified eol value: " + eol + " for " + file + ", defaulting to platform native");
				return LineEnding.PLATFORM_NATIVE.str();
			}
		}

		/** Creates a LineEnding from an EOL. */
		private static LineEnding fromEol(EOL eol) {
			// @formatter:off
			switch (eol) {
			case CRLF:    return LineEnding.WINDOWS;
			case LF:      return LineEnding.UNIX;
			case NATIVE:  return LineEnding.PLATFORM_NATIVE;
			default: throw Unhandled.enumException(eol);
			}
			// @formatter:on
		}
	}

	/** Parses and caches .gitattributes files. */
	static class AttributesCache {
		final Map<File, List<AttributesRule>> rulesAtPath = new HashMap<>();

		/** Returns a value if there is one, or unspecified if there isn't. */
		public String valueFor(File file, String key) {
			StringBuilder pathBuilder = new StringBuilder(file.getAbsolutePath().length());
			boolean isDirectory = file.isDirectory();
			File parent = file.getParentFile();

			pathBuilder.append(file.getName());
			while (parent != null) {
				String path = pathBuilder.toString();

				String value = findAttributeInRules(path, isDirectory, key, getRulesForFolder(parent));
				if (value != null) {
					return value;
				}

				pathBuilder.insert(0, parent.getName() + "/");
				parent = parent.getParentFile();
			}
			return null;
		}

		/** Returns the gitattributes rules for the given folder. */
		private List<AttributesRule> getRulesForFolder(File folder) {
			return rulesAtPath.computeIfAbsent(folder, f -> parseRules(new File(f, ".gitattributes")));
		}
	}

	/** Parses a list of rules from the given file, returning an empty list if the file doesn't exist. */
	private static List<AttributesRule> parseRules(File file) {
		if (file.exists() && file.isFile()) {
			try (InputStream stream = new FileInputStream(file)) {
				AttributesNode parsed = new AttributesNode();
				parsed.parse(stream);
				return parsed.getRules();
			} catch (IOException e) {
				// no need to crash the whole plugin
				System.err.println("Problem parsing " + file.getAbsolutePath());
				e.printStackTrace();
			}
		}
		return Collections.emptyList();
	}

	/** Parses an attribute value from a list of rules, returning null if there is no match for the given key. */
	private static String findAttributeInRules(String subpath, boolean isFolder, String key, List<AttributesRule> rules) {
		String value = null;
		// later rules override earlier ones
		for (AttributesRule rule : rules) {
			if (rule.isMatch(subpath, isFolder)) {
				for (Attribute attribute : rule.getAttributes()) {
					if (attribute.getKey().equals(key)) {
						value = attribute.getValue();
					}
				}
			}
		}
		return value;
	}
}
