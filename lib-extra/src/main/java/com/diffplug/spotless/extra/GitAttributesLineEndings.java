/*
 * Copyright 2016-2023 DiffPlug
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.eclipse.jgit.attributes.Attribute;
import org.eclipse.jgit.attributes.AttributesNode;
import org.eclipse.jgit.attributes.AttributesRule;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig;
import org.eclipse.jgit.lib.CoreConfig.AutoCRLF;
import org.eclipse.jgit.lib.CoreConfig.EOL;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharSequenceNodeFactory;

import com.diffplug.common.base.Errors;
import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.LazyForwardingEquality;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.extra.GitWorkarounds.RepositorySpecificResolver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Uses <a href="https://git-scm.com/docs/gitattributes">.gitattributes</a> to determine
 * the appropriate line ending. Falls back to the {@code core.eol} and {@code core.autocrlf} properties in the
 * git config if there are no applicable git attributes, then finally falls
 * back to the platform native.
 */
public final class GitAttributesLineEndings {
	private static final Logger LOGGER = LoggerFactory.getLogger(GitAttributesLineEndings.class);

	// prevent direct instantiation
	private GitAttributesLineEndings() {}

	/**
	 * Creates a line-endings policy whose serialized state is relativized against projectDir,
	 * at the cost of eagerly evaluating the line-ending state of every target file when the
	 * policy is checked for equality with another policy.
	 */
	public static LineEnding.Policy create(File projectDir, Supplier<Iterable<File>> toFormat) {
		return new RelocatablePolicy(projectDir, toFormat);
	}

	static class RelocatablePolicy extends LazyForwardingEquality<CachedEndings> implements LineEnding.Policy {
		private static final long serialVersionUID = 5868522122123693015L;

		transient File projectDir;
		transient Supplier<Iterable<File>> toFormat;

		RelocatablePolicy(File projectDir, Supplier<Iterable<File>> toFormat) {
			this.projectDir = Objects.requireNonNull(projectDir, "projectDir");
			this.toFormat = Objects.requireNonNull(toFormat, "toFormat");
		}

		@Override
		protected CachedEndings calculateState() throws Exception {
			Runtime runtime = new RuntimeInit(projectDir).atRuntime();
			// LazyForwardingEquality guarantees that this will only be called once, and keeping toFormat
			// causes a memory leak, see https://github.com/diffplug/spotless/issues/1194
			CachedEndings state = new CachedEndings(projectDir, runtime, toFormat.get());
			projectDir = null;
			toFormat = null;
			return state;
		}

		@Override
		public String getEndingFor(File file) {
			return state().endingFor(file);
		}
	}

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	static class CachedEndings implements Serializable {
		private static final long serialVersionUID = -2534772773057900619L;

		/** this is transient, to simulate PathSensitive.RELATIVE */
		transient final String rootDir;
		/** the line ending used for most files */
		final String defaultEnding;
		/** any exceptions to that default, in terms of relative path from rootDir */
		final ConcurrentRadixTree<String> hasNonDefaultEnding = new ConcurrentRadixTree<>(new DefaultCharSequenceNodeFactory());

		CachedEndings(File projectDir, Runtime runtime, Iterable<File> toFormat) {
			String rootPath = FileSignature.pathNativeToUnix(projectDir.getAbsolutePath());
			rootDir = rootPath.equals("/") ? rootPath : rootPath + "/";
			defaultEnding = runtime.defaultEnding;
			for (File file : toFormat) {
				String ending = runtime.getEndingFor(file);
				if (!ending.equals(defaultEnding)) {
					String absPath = FileSignature.pathNativeToUnix(file.getAbsolutePath());
					String subPath = FileSignature.subpath(rootDir, absPath);
					hasNonDefaultEnding.put(subPath, ending);
				}
			}
		}

		/** Returns the line ending appropriate for the given file. */
		public String endingFor(File file) {
			String absPath = FileSignature.pathNativeToUnix(file.getAbsolutePath());
			String subpath = FileSignature.subpath(rootDir, absPath);
			String ending = hasNonDefaultEnding.getValueForExactKey(subpath);
			return ending == null ? defaultEnding : ending;
		}
	}

	static class RuntimeInit {
		/** /etc/gitconfig (system-global), ~/.gitconfig (each might-not exist). */
		final FileBasedConfig systemConfig, userConfig;

		/** Repository specific config, can be $GIT_COMMON_DIR/config, project/.git/config or .git/worktrees/<id>/config.worktree if enabled by extension */
		final Config repoConfig;

		/** Global .gitattributes file pointed at by systemConfig or userConfig, and the file in the repo. */
		final @Nullable File globalAttributesFile, repoAttributesFile;

		/** git worktree root, might not exist if we're not in a git repo. */
		final @Nullable File workTree;

		@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
		RuntimeInit(File projectDir) {
			/////////////////////////////////
			// USER AND SYSTEM-WIDE VALUES //
			/////////////////////////////////
			systemConfig = SystemReader.getInstance().openSystemConfig(null, FS.DETECTED);
			Errors.log().run(systemConfig::load);
			userConfig = SystemReader.getInstance().openUserConfig(systemConfig, FS.DETECTED);
			Errors.log().run(userConfig::load);

			// copy-pasted from org.eclipse.jgit.internal.storage.file.GlobalAttributesNode
			String globalAttributesPath = userConfig.get(CoreConfig.KEY).getAttributesFile();
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
			RepositorySpecificResolver repositoryResolver = GitWorkarounds.fileRepositoryResolverForProject(projectDir, userConfig);
			if (repositoryResolver.getGitDir() != null) {
				workTree = repositoryResolver.getWorkTree();
				repoConfig = repositoryResolver.getRepositoryConfig();
				repoAttributesFile = repositoryResolver.resolveWithCommonDir(Constants.INFO_ATTRIBUTES);
			} else {
				workTree = null;
				repoConfig = new Config();
				repoAttributesFile = null;
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
			this.defaultEnding = findDefaultLineEnding(config).str();
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
				LOGGER.warn(".gitattributes file has unspecified eol value: {} for {}, defaulting to platform native", eol, file);
				return LineEnding.PLATFORM_NATIVE.str();
			}
		}

		private LineEnding findDefaultLineEnding(Config config) {
			// handle core.autocrlf, whose values "true" and "input" override core.eol
			AutoCRLF autoCRLF = config.getEnum(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_AUTOCRLF, AutoCRLF.FALSE);
			if (autoCRLF == AutoCRLF.TRUE) {
				// autocrlf=true converts CRLF->LF during commit
				//               and converts LF->CRLF during checkout
				// so CRLF is the default line ending
				return LineEnding.WINDOWS;
			} else if (autoCRLF == AutoCRLF.INPUT) {
				// autocrlf=input converts CRLF->LF during commit
				//                and does no conversion during checkout
				// mostly used on Unix, so LF is the default encoding
				return LineEnding.UNIX;
			} else if (autoCRLF == AutoCRLF.FALSE) {
				// handle core.eol
				EOL eol = config.getEnum(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_EOL, EOL.NATIVE);
				return fromEol(eol);
			} else {
				throw new IllegalStateException("Unexpected value for autoCRLF " + autoCRLF);
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
				LOGGER.warn("Problem parsing {}", file.getAbsolutePath(), e);
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
