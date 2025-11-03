/*
 * Copyright 2016-2025 DiffPlug
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

import static java.lang.System.lineSeparator;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Java virtual machine helper */
public final class Jvm {
	private static final int VERSION;

	static {
		String jre = System.getProperty("java.version");
		if (jre.startsWith("1.8")) {
			VERSION = 8;
		} else {
			Matcher matcher = Pattern.compile("(\\d+)").matcher(jre);
			if (!matcher.find()) {
				throw new IllegalArgumentException("Expected " + jre + " to start with an integer");
			}
			VERSION = Integer.parseInt(matcher.group(1));
			if (VERSION <= 8) {
				throw new IllegalArgumentException("Expected " + jre + " to start with an integer greater than 8");
			}
		}
	}

	/** @return the major version of this VM, e.g. 8, 9, 10, 11, 13, etc. */
	public static int version() {
		return VERSION;
	}

	/**
	 * Utility to map constraints of formatter to this JVM
	 * @param <V> Version type of formatter
	 */
	public static final class Support<V> {
		static final String LINT_CODE = "jvm-version";

		private final String fmtName;
		private final Comparator<? super V> fmtVersionComparator;
		private final NavigableMap<Integer, V> jvm2fmtMaxVersion;
		private final NavigableMap<Integer, V> jvm2fmtMinVersion;
		private final NavigableMap<V, Integer> fmtMaxVersion2jvmVersion;

		private Support(String fromatterName) {
			this(fromatterName, new SemanticVersionComparator<V>());
		}

		private Support(String formatterName, Comparator<? super V> formatterVersionComparator) {
			fmtName = formatterName;
			fmtVersionComparator = formatterVersionComparator;
			jvm2fmtMaxVersion = new TreeMap<>();
			jvm2fmtMinVersion = new TreeMap<>();
			fmtMaxVersion2jvmVersion = new TreeMap<>(formatterVersionComparator);
		}

		/**
		 * Add maximum supported formatter version
		 * @param minimumJvmVersion Minimum Java version required
		 * @param maxFormatterVersion Maximum formatter version supported by the Java version
		 * @return this
		 */
		public Support<V> add(int minimumJvmVersion, V maxFormatterVersion) {
			Objects.requireNonNull(maxFormatterVersion);
			if (jvm2fmtMaxVersion.put(minimumJvmVersion, maxFormatterVersion) != null) {
				throw new IllegalArgumentException("Added duplicate entry for JVM %d+.".formatted(minimumJvmVersion));
			}
			if (fmtMaxVersion2jvmVersion.put(maxFormatterVersion, minimumJvmVersion) != null) {
				throw new IllegalArgumentException("Added duplicate entry for formatter version %s.".formatted(maxFormatterVersion));
			}
			verifyVersionRangesDoNotIntersect(jvm2fmtMaxVersion, minimumJvmVersion, maxFormatterVersion);
			return this;
		}

		public Support<V> addMin(int minimumJvmVersion, V minFormatterVersion) {
			Objects.requireNonNull(minFormatterVersion);
			if (jvm2fmtMinVersion.put(minimumJvmVersion, minFormatterVersion) != null) {
				throw new IllegalArgumentException("Added duplicate entry for JVM %d+.".formatted(minimumJvmVersion));
			}
			verifyVersionRangesDoNotIntersect(jvm2fmtMinVersion, minimumJvmVersion, minFormatterVersion);
			return this;
		}

		private void verifyVersionRangesDoNotIntersect(NavigableMap<Integer, V> jvm2fmtVersion, int minimumJvmVersion, V formatterVersion) {
			Map.Entry<Integer, V> lower = jvm2fmtVersion.lowerEntry(minimumJvmVersion);
			if ((lower != null) && (fmtVersionComparator.compare(formatterVersion, lower.getValue()) <= 0)) {
				throw new IllegalArgumentException("%d/%s should be lower than %d/%s".formatted(minimumJvmVersion, formatterVersion, lower.getKey(), lower.getValue()));
			}
			Map.Entry<Integer, V> higher = jvm2fmtVersion.higherEntry(minimumJvmVersion);
			if ((higher != null) && (fmtVersionComparator.compare(formatterVersion, higher.getValue()) >= 0)) {
				throw new IllegalArgumentException("%d/%s should be higher than %d/%s".formatted(minimumJvmVersion, formatterVersion, higher.getKey(), higher.getValue()));
			}
		}

		/** @return Highest formatter version recommended for this JVM (null, if JVM not supported) */
		@Nullable public V getRecommendedFormatterVersion() {
			Integer configuredJvmVersionOrNull = jvm2fmtMaxVersion.floorKey(Jvm.version());
			return configuredJvmVersionOrNull == null ? null : jvm2fmtMaxVersion.get(configuredJvmVersionOrNull);
		}

		@Nullable public V getMinimumRequiredFormatterVersion() {
			Integer configuredJvmVersionOrNull = jvm2fmtMinVersion.floorKey(Jvm.version());
			return configuredJvmVersionOrNull == null ? null : jvm2fmtMinVersion.get(configuredJvmVersionOrNull);
		}

		/**
		 * Assert the formatter is supported
		 * @param formatterVersion Formatter version
		 * @throws IllegalArgumentException if {@code formatterVersion} not supported
		 */
		public void assertFormatterSupported(V formatterVersion) {
			Objects.requireNonNull(formatterVersion);
			String error = buildUnsupportedFormatterMessage(formatterVersion);
			if (!error.isEmpty()) {
				throw Lint.atUndefinedLine(LINT_CODE, error).shortcut();
			}
		}

		private String buildUnsupportedFormatterMessage(V fmtVersion) {
			// check if the jvm version is to low for the formatter version
			int requiredJvmVersion = getRequiredJvmVersion(fmtVersion);
			if (Jvm.version() < requiredJvmVersion) {
				return buildUpgradeJvmMessage(fmtVersion) + "Upgrade your JVM or try " + toString();
			}
			// check if the formatter version is too low for the jvm version
			V minimumFormatterVersion = getMinimumRequiredFormatterVersion();
			if ((minimumFormatterVersion != null) && (fmtVersionComparator.compare(fmtVersion, minimumFormatterVersion) < 0)) {
				return "You are running Spotless on JVM %d. This requires %s of at least %s (you are using %s).%n".formatted(Jvm.version(), fmtName, minimumFormatterVersion, fmtVersion);
			}
			// otherwise all is well
			return "";
		}

		private String buildUpgradeJvmMessage(V fmtVersion) {
			StringBuilder builder = new StringBuilder();
			builder.append("You are running Spotless on JVM %d".formatted(Jvm.version()));
			V recommendedFmtVersionOrNull = getRecommendedFormatterVersion();
			if (recommendedFmtVersionOrNull != null) {
				builder.append(", which limits you to %s %s.%n".formatted(fmtName, recommendedFmtVersionOrNull));
			} else {
				Entry<V, Integer> nextFmtVersionOrNull = fmtMaxVersion2jvmVersion.ceilingEntry(fmtVersion);
				if (nextFmtVersionOrNull != null) {
					builder.append(". %s %s requires JVM %d+".formatted(fmtName, fmtVersion, nextFmtVersionOrNull.getValue()));
				}
				builder.append(".%n".formatted());
			}
			return builder.toString();
		}

		private int getRequiredJvmVersion(V fmtVersion) {
			Entry<V, Integer> entry = fmtMaxVersion2jvmVersion.ceilingEntry(fmtVersion);
			if (entry == null) {
				entry = fmtMaxVersion2jvmVersion.lastEntry();
			}
			if (entry != null) {
				V maxKnownFmtVersion = jvm2fmtMaxVersion.get(entry.getValue());
				if (fmtVersionComparator.compare(fmtVersion, maxKnownFmtVersion) <= 0) {
					return entry.getValue();
				}
			}
			return 0;
		}

		/**
		 * Suggest to use a different formatter version if formatting fails
		 * @param formatterVersion Formatter version
		 * @param originalFunc Formatter function
		 * @return Wrapped formatter function. Adding hint about later versions to exceptions.
		 */
		public FormatterFunc suggestLaterVersionOnError(V formatterVersion, FormatterFunc originalFunc) {
			Objects.requireNonNull(formatterVersion);
			Objects.requireNonNull(originalFunc);
			final String hintUnsupportedProblem = buildUnsupportedFormatterMessage(formatterVersion);
			final String proposeDifferentFormatter = hintUnsupportedProblem.isEmpty() ? buildUpgradeFormatterMessage(formatterVersion) : hintUnsupportedProblem;
			return proposeDifferentFormatter.isEmpty() ? originalFunc : new FormatterFunc() {

				@Override
				public String apply(String unix, File file) throws Exception {
					try {
						return originalFunc.apply(unix, file);
					} catch (Exception e) {
						throw new Exception(proposeDifferentFormatter, e);
					}
				}

				@Override
				public String apply(String input) throws Exception {
					try {
						return originalFunc.apply(input);
					} catch (Exception e) {
						throw new Exception(proposeDifferentFormatter, e);
					}
				}

			};
		}

		private String buildUpgradeFormatterMessage(V fmtVersion) {
			StringBuilder builder = new StringBuilder();
			// check if the formatter is not supported on this jvm
			V minimumFormatterVersion = getMinimumRequiredFormatterVersion();
			V recommendedFmtVersionOrNull = getRecommendedFormatterVersion();
			if ((minimumFormatterVersion != null) && (fmtVersionComparator.compare(fmtVersion, minimumFormatterVersion) < 0)) {
				builder.append("You are running Spotless on JVM %d. This requires %s of at least %s.%n".formatted(Jvm.version(), fmtName, minimumFormatterVersion));
				builder.append("You are using %s %s.%n".formatted(fmtName, fmtVersion));
				if (recommendedFmtVersionOrNull != null) {
					builder.append("%s %s is the recommended version, which may have fixed this problem.%n".formatted(fmtName, recommendedFmtVersionOrNull));
				}
				// check if the formatter is outdated on this jvm
			} else if (recommendedFmtVersionOrNull != null && (fmtVersionComparator.compare(fmtVersion, recommendedFmtVersionOrNull) < 0)) {
				builder.append("%s %s is currently being used, but outdated.%n".formatted(fmtName, fmtVersion));
				builder.append("%s %s is the recommended version, which may have fixed this problem.%n".formatted(fmtName, recommendedFmtVersionOrNull));
				builder.append("%s %s requires JVM %d+.".formatted(fmtName, recommendedFmtVersionOrNull, getRequiredJvmVersion(recommendedFmtVersionOrNull)));
			} else {
				V higherFormatterVersionOrNull = fmtMaxVersion2jvmVersion.higherKey(fmtVersion);
				if (higherFormatterVersionOrNull != null) {
					builder.append(buildUpgradeJvmMessage(fmtVersion));
					Integer higherJvmVersion = fmtMaxVersion2jvmVersion.get(higherFormatterVersionOrNull);
					builder.append("If you upgrade your JVM to %d+, then you can use %s %s, which may have fixed this problem.".formatted(higherJvmVersion, fmtName, higherFormatterVersionOrNull));
				}
			}
			return builder.toString();
		}

		@Override
		public String toString() {
			return "%s alternatives:%n".formatted(fmtName)
					+ jvm2fmtMaxVersion.entrySet().stream().map(
							e -> "- Version %s requires JVM %d+".formatted(e.getValue(), e.getKey())).collect(Collectors.joining(lineSeparator()));
		}

		@SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
		private static class SemanticVersionComparator<V> implements Comparator<V> {

			@Override
			public int compare(V version0, V version1) {
				Objects.requireNonNull(version0);
				Objects.requireNonNull(version1);
				int[] version0Items = convert(version0);
				int[] version1Items = convert(version1);
				int numberOfElements = version0Items.length > version1Items.length ? version0Items.length : version1Items.length;
				version0Items = Arrays.copyOf(version0Items, numberOfElements);
				version1Items = Arrays.copyOf(version1Items, numberOfElements);
				for (int i = 0; i < numberOfElements; i++) {
					if (version0Items[i] > version1Items[i]) {
						return 1;
					} else if (version1Items[i] > version0Items[i]) {
						return -1;
					}
				}
				return 0;
			}

			private static <V> int[] convert(V versionObject) {
				try {
					String versionString = versionObject.toString();
					if (versionString.endsWith("-SNAPSHOT")) {
						versionString = versionString.substring(0, versionString.length() - "-SNAPSHOT".length());
					}
					return Arrays.asList(versionString.split("\\.")).stream().mapToInt(Integer::parseInt).toArray();
				} catch (Exception e) {
					throw new IllegalArgumentException("Not a semantic version: %s".formatted(versionObject), e);
				}
			}
		}
	}

	/**
	 * Creates a map of JVM requirements for a formatter
	 * @param <V> Version type of the formatter (V#toString() must correspond to a semantic version, separated by dots)
	 * @param formatterName Name of the formatter
	 * @return Empty map of supported formatters
	 */
	public static <V> Support<V> support(String formatterName) {
		Objects.requireNonNull(formatterName);
		return new Support<>(formatterName);
	}

	private Jvm() {}
}
