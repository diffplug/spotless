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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.jgit.attributes.Attribute;
import org.eclipse.jgit.attributes.AttributesNode;
import org.eclipse.jgit.attributes.AttributesNodeProvider;
import org.eclipse.jgit.attributes.AttributesRule;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.CoreConfig.EOL;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Unhandled;

/**
 * Uses [.gitattributes](https://git-scm.com/docs/gitattributes) to determine
 * the appropriate line ending. Falls back to the `core.eol` property in the
 * git config if there are no applicable git attributes, then finally falls
 * back to the platform native.
 */
class GitAttributesLineEndingPolicy implements LineEnding.Policy {
	public static GitAttributesLineEndingPolicy create(File rootFolder) {
		return Errors.rethrow().get(() -> {
			FileRepositoryBuilder builder = new FileRepositoryBuilder();
			builder.findGitDir(rootFolder);
			if (builder.getGitDir() != null) {
				// we found a repository, so we can grab all the values we need from it
				Repository repo = builder.build();
				AttributesNodeProvider nodeProvider = repo.createAttributesNodeProvider();
				Function<AttributesNode, List<AttributesRule>> getRules = node -> node == null ? Collections.emptyList() : node.getRules();
				return new GitAttributesLineEndingPolicy(repo.getConfig(),
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
				return new GitAttributesLineEndingPolicy(userConfig,
						// no git info file
						Collections.emptyList(), null,
						globalRules);
			}
		});
	}

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

	private GitAttributesLineEndingPolicy(Config config, List<AttributesRule> infoRules, File workTree, List<AttributesRule> globalRules) {
		this.infoRules = Objects.requireNonNull(infoRules);
		this.workTree = workTree;
		this.globalRules = Objects.requireNonNull(globalRules);
		this.defaultEnding = fromEol(config.getEnum(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_EOL, EOL.NATIVE)).str();
	}

	private static final String KEY_EOL = "eol";
	private static final boolean IS_FOLDER = false;

	@Override
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

	private String convertEolToLineEnding(String eol, File file) {
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
	static LineEnding fromEol(EOL eol) {
		// @formatter:off
		switch (eol) {
		case CRLF:    return LineEnding.WINDOWS;
		case LF:      return LineEnding.UNIX;
		case NATIVE:  return LineEnding.PLATFORM_NATIVE;
		default: throw Unhandled.enumException(eol);
		}
		// @formatter:on
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
	static String findAttributeInRules(String subpath, boolean isFolder, String key, List<AttributesRule> rules) {
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
