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
package com.diffplug.spotless.extra;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.eclipse.jgit.attributes.Attribute;
import org.eclipse.jgit.attributes.AttributesNode;
import org.eclipse.jgit.attributes.AttributesRule;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig.EOL;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.Node;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharSequenceNodeFactory;

import com.diffplug.common.base.Errors;
import com.diffplug.common.tree.TreeStream;
import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.LazyForwardingEquality;
import com.diffplug.spotless.LineEnding;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Uses [.gitattributes](https://git-scm.com/docs/gitattributes) to determine
 * the appropriate line ending. Falls back to the `core.eol` property in the
 * git config if there are no applicable git attributes, then finally falls
 * back to the platform native.
 */
public final class GitAttributesLineEndings {
	// prevent direct instantiation
	private GitAttributesLineEndings() {}

	public static Policy create(File projectDir, Supplier<Iterable<File>> toFormat) {
		return new Policy(projectDir, toFormat);
	}

	static class Policy extends LazyForwardingEquality<FileState> implements LineEnding.Policy {
		private static final long serialVersionUID = 1L;

		final transient File projectDir;
		final transient Supplier<Iterable<File>> toFormat;

		Policy(File projectDir, Supplier<Iterable<File>> toFormat) {
			this.projectDir = Objects.requireNonNull(projectDir);
			this.toFormat = Objects.requireNonNull(toFormat);
		}

		@Override
		protected FileState calculateState() throws Exception {
			return new FileState(projectDir, toFormat.get());
		}

		/**
		 * Initializing the state() for up-to-date checking is faster than the full initialization
		 * needed to actually do the formatting. We load the Runtime lazily from the state().
		 */
		transient Runtime runtime;

		@Override
		public String getEndingFor(File file) {
			if (runtime == null) {
				runtime = state().atRuntime();
			}
			return runtime.getEndingFor(file);
		}
	}

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	static class FileState implements Serializable {
		private static final long serialVersionUID = 1L;

		/** /etc/gitconfig (system-global), ~/.gitconfig, project/.git/config (each might-not exist). */
		transient final FileBasedConfig systemConfig, userConfig, repoConfig;

		/** Global .gitattributes file pointed at by systemConfig or userConfig, and the file in the repo. */
		transient final @Nullable File globalAttributesFile, repoAttributesFile;

		/** git worktree root, might not exist if we're not in a git repo. */
		transient final @Nullable File workTree;

		/** All the .gitattributes files in the work tree that we're formatting. */
		transient final List<File> gitattributes;

		/** The signature of *all* of the files below. */
		final FileSignature signature;

		@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
		FileState(File projectDir, Iterable<File> toFormat) throws IOException {
			Objects.requireNonNull(projectDir);
			Objects.requireNonNull(toFormat);
			/////////////////////////////////
			// USER AND SYSTEM-WIDE VALUES //
			/////////////////////////////////
			systemConfig = SystemReader.getInstance().openSystemConfig(null, FS.DETECTED);
			Errors.log().run(systemConfig::load);
			userConfig = SystemReader.getInstance().openUserConfig(systemConfig, FS.DETECTED);
			Errors.log().run(userConfig::load);

			// copy-pasted from org.eclipse.jgit.lib.CoreConfig
			String globalAttributesPath = userConfig.getString(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_ATTRIBUTESFILE);
			// copy-pasted from org.eclipse.jgit.internal.storage.file.GlobalAttributesNode
			if (globalAttributesPath != null) {
				FS fs = FS.detect();
				if (globalAttributesPath.startsWith("~/")) { //$NON-NLS-1$
					globalAttributesFile = fs.resolve(fs.userHome(), globalAttributesPath.substring(2));
				} else {
					globalAttributesFile = fs.resolve(null, globalAttributesPath);
				}
			} else {
				globalAttributesFile = null;
			}

			//////////////////////////
			// REPO-SPECIFIC VALUES //
			//////////////////////////
			FileRepositoryBuilder builder = new FileRepositoryBuilder();
			builder.findGitDir(projectDir);
			if (builder.getGitDir() != null) {
				workTree = builder.getWorkTree();
				repoConfig = new FileBasedConfig(userConfig, new File(builder.getGitDir(), Constants.CONFIG), FS.DETECTED);
				repoAttributesFile = new File(builder.getGitDir(), Constants.INFO_ATTRIBUTES);
			} else {
				workTree = null;
				// null would make repoConfig.getFile() bomb below
				repoConfig = new FileBasedConfig(userConfig, null, FS.DETECTED) {
					@Override
					public void load() {
						// empty, do not load
					}

					@Override
					public boolean isOutdated() {
						// regular class would bomb here
						return false;
					}
				};
				repoAttributesFile = null;
			}
			Errors.log().run(repoConfig::load);

			// The .gitattributes files which apply to the files we are formatting
			gitattributes = gitAttributes(toFormat);

			// find every actual File which exists above
			Stream<File> misc = Stream.of(systemConfig.getFile(), userConfig.getFile(), repoConfig.getFile(), globalAttributesFile, repoAttributesFile);
			List<File> toSign = Stream.concat(gitattributes.stream(), misc)
					.filter(file -> file != null && file.exists() && file.isFile())
					.collect(Collectors.toList());
			// sign it for up-to-date checking
			signature = FileSignature.signAsSet(toSign);
		}

		/** Returns all of the .gitattributes files which affect the given files. */
		static List<File> gitAttributes(Iterable<File> files) {
			// build a radix tree out of all the parent folders in these files
			ConcurrentRadixTree<String> tree = new ConcurrentRadixTree<>(new DefaultCharSequenceNodeFactory());
			for (File file : files) {
				String parentPath = file.getParent() + File.separator;
				tree.putIfAbsent(parentPath, parentPath);
			}
			// traverse the edge nodes to find the outermost folders
			List<File> edgeFolders = TreeStream.depthFirst(Node::getOutgoingEdges, tree.getNode())
					.filter(node -> node.getOutgoingEdges().isEmpty() && node.getValue() != null)
					.map(node -> new File((String) node.getValue()))
					.collect(Collectors.toList());

			List<File> gitAttrFiles = new ArrayList<>();
			Set<File> visitedFolders = new HashSet<>();
			for (File edgeFolder : edgeFolders) {
				gitAttrAddWithParents(edgeFolder, visitedFolders, gitAttrFiles);
			}
			return gitAttrFiles;
		}

		/** Searches folder and all its parents for gitattributes files. */
		private static void gitAttrAddWithParents(File folder, Set<File> visitedFolders, Collection<File> gitAttrFiles) {
			if (!visitedFolders.add(folder)) {
				// bail if we already visited this folder
				return;
			}

			File gitAttr = new File(folder, Constants.DOT_GIT_ATTRIBUTES);
			if (gitAttr.exists() && gitAttr.isFile()) {
				gitAttrFiles.add(gitAttr);
			}
			File parentFile = folder.getParentFile();
			if (parentFile != null) {
				gitAttrAddWithParents(folder.getParentFile(), visitedFolders, gitAttrFiles);
			}
		}

		private Runtime atRuntime() {
			return new Runtime(parseRules(repoAttributesFile), workTree, repoConfig, parseRules(globalAttributesFile));
		}
	}

	/** https://github.com/git/git/blob/1fe8f2cf461179c41f64efbd1dc0a9fb3b7a0fb1/Documentation/gitattributes.txt */
	static class Runtime {
		/** .git/info/attributes (and the worktree with that file) */
		final List<AttributesRule> infoRules;

		final @Nullable File workTree;

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

		private Runtime(List<AttributesRule> infoRules, @Nullable File workTree, Config config, List<AttributesRule> globalRules) {
			this.infoRules = Objects.requireNonNull(infoRules);
			this.workTree = workTree;
			this.defaultEnding = fromEol(config.getEnum(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_EOL, EOL.NATIVE)).str();
			this.globalRules = Objects.requireNonNull(globalRules);
		}

		private static final String KEY_EOL = "eol";
		private static final boolean IS_FOLDER = false;

		public String getEndingFor(File file) {
			// handle the info rules first, since they trump everything
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
			default: throw new IllegalArgumentException("Unknown eol " + eol);
			}
			// @formatter:on
		}
	}

	/** Parses and caches .gitattributes files. */
	static class AttributesCache {
		final Map<File, List<AttributesRule>> rulesAtPath = new HashMap<>();

		/** Returns a value if there is one, or unspecified if there isn't. */
		public @Nullable String valueFor(File file, String key) {
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
			return rulesAtPath.computeIfAbsent(folder, f -> parseRules(new File(f, Constants.DOT_GIT_ATTRIBUTES)));
		}
	}

	/** Parses a list of rules from the given file, returning an empty list if the file doesn't exist. */
	private static List<AttributesRule> parseRules(@Nullable File file) {
		if (file != null && file.exists() && file.isFile()) {
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
	private static @Nullable String findAttributeInRules(String subpath, boolean isFolder, String key, List<AttributesRule> rules) {
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
