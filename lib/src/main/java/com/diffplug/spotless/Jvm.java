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
package com.diffplug.spotless;

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
	public static class Support<V> {
		private final String fmtName;
		private final Comparator<? super V> fmtVersionComparator;
		private final NavigableMap<Integer, V> jvm2fmtVersion;
		private final NavigableMap<V, Integer> fmt2jvmVersion;

		private Support(String fromatterName) {
			this(fromatterName, new SemanticVersionComparator<>());
		}

		private Support(String formatterName, Comparator<? super V> formatterVersionComparator) {
			fmtName = formatterName;
			fmtVersionComparator = formatterVersionComparator;
			jvm2fmtVersion = new TreeMap<>();
			fmt2jvmVersion = new TreeMap<>(formatterVersionComparator);
		}

		/**
		 * Add supported formatter version
		 * @param minimumJvmVersion Minimum Java version required
		 * @param maxFormatterVersion Maximum formatter version supported by the Java version
		 * @return this
		 */
		public Support<V> add(int minimumJvmVersion, V maxFormatterVersion) {
			Objects.requireNonNull(maxFormatterVersion);
			if (null != jvm2fmtVersion.put(minimumJvmVersion, maxFormatterVersion)) {
				throw new IllegalArgumentException(String.format("Added duplicate entry for JVM %d+.", minimumJvmVersion));
			}
			if (null != fmt2jvmVersion.put(maxFormatterVersion, minimumJvmVersion)) {
				throw new IllegalArgumentException(String.format("Added duplicate entry for formatter version %s.", maxFormatterVersion));
			}
			Map.Entry<Integer, V> lower = jvm2fmtVersion.lowerEntry(minimumJvmVersion);
			if ((null != lower) && (fmtVersionComparator.compare(maxFormatterVersion, lower.getValue()) <= 0)) {
				throw new IllegalArgumentException(String.format("%d/%s should be lower than %d/%s", minimumJvmVersion, maxFormatterVersion, lower.getKey(), lower.getValue()));
			}
			Map.Entry<Integer, V> higher = jvm2fmtVersion.higherEntry(minimumJvmVersion);
			if ((null != higher) && (fmtVersionComparator.compare(maxFormatterVersion, higher.getValue()) >= 0)) {
				throw new IllegalArgumentException(String.format("%d/%s should be higher than %d/%s", minimumJvmVersion, maxFormatterVersion, higher.getKey(), higher.getValue()));
			}
			return this;
		}

		/** @return Highest formatter version recommended for this JVM (null, if JVM not supported) */
		@Nullable
		public V getRecommendedFormatterVersion() {
			Integer configuredJvmVersionOrNull = jvm2fmtVersion.floorKey(Jvm.version());
			return (null == configuredJvmVersionOrNull) ? null : jvm2fmtVersion.get(configuredJvmVersionOrNull);
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
				throw new IllegalArgumentException(error);
			}
		}

		private String buildUnsupportedFormatterMessage(V fmtVersion) {
			int requiredJvmVersion = getRequiredJvmVersion(fmtVersion);
			if (Jvm.version() < requiredJvmVersion) {
				return buildUpgradeJvmMessage(fmtVersion) + "Upgrade your JVM or try " + this;
			}
			return "";
		}

		private String buildUpgradeJvmMessage(V fmtVersion) {
			StringBuilder builder = new StringBuilder();
			builder.append(String.format("You are running Spotless on JVM %d", Jvm.version()));
			V recommendedFmtVersionOrNull = getRecommendedFormatterVersion();
			if (null != recommendedFmtVersionOrNull) {
				builder.append(String.format(", which limits you to %s %s.%n", fmtName, recommendedFmtVersionOrNull));
			} else {
				Entry<V, Integer> nextFmtVersionOrNull = fmt2jvmVersion.ceilingEntry(fmtVersion);
				if (null != nextFmtVersionOrNull) {
					builder.append(String.format(". %s %s requires JVM %d+", fmtName, fmtVersion, nextFmtVersionOrNull.getValue()));
				}
				builder.append(String.format(".%n"));
			}
			return builder.toString();
		}

		private int getRequiredJvmVersion(V fmtVersion) {
			Entry<V, Integer> entry = fmt2jvmVersion.ceilingEntry(fmtVersion);
			if (null == entry) {
				entry = fmt2jvmVersion.lastEntry();
			}
			if (null != entry) {
				V maxKnownFmtVersion = jvm2fmtVersion.get(entry.getValue());
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
			final String proposeDiffererntFormatter = hintUnsupportedProblem.isEmpty() ? buildUpgradeFormatterMessage(formatterVersion) : hintUnsupportedProblem;
			return proposeDiffererntFormatter.isEmpty() ? originalFunc : new FormatterFunc() {

				@Override
				public String apply(String unix, File file) throws Exception {
					try {
						return originalFunc.apply(unix, file);
					} catch (Exception e) {
						throw new Exception(proposeDiffererntFormatter, e);
					}
				}

				@Override
				public String apply(String input) throws Exception {
					try {
						return originalFunc.apply(input);
					} catch (Exception e) {
						throw new Exception(proposeDiffererntFormatter, e);
					}
				}

			};
		}

		private String buildUpgradeFormatterMessage(V fmtVersion) {
			StringBuilder builder = new StringBuilder();
			V recommendedFmtVersionOrNull = getRecommendedFormatterVersion();
			if (null != recommendedFmtVersionOrNull && (fmtVersionComparator.compare(fmtVersion, recommendedFmtVersionOrNull) < 0)) {
				builder.append(String.format("%s %s is currently being used, but outdated.%n", fmtName, fmtVersion));
				builder.append(String.format("%s %s is the recommended version, which may have fixed this problem.%n", fmtName, recommendedFmtVersionOrNull));
				builder.append(String.format("%s %s requires JVM %d+.", fmtName, recommendedFmtVersionOrNull, getRequiredJvmVersion(recommendedFmtVersionOrNull)));
			} else {
				V higherFormatterVersionOrNull = fmt2jvmVersion.higherKey(fmtVersion);
				if (null != higherFormatterVersionOrNull) {
					builder.append(buildUpgradeJvmMessage(fmtVersion));
					Integer higherJvmVersion = fmt2jvmVersion.get(higherFormatterVersionOrNull);
					builder.append(String.format("If you upgrade your JVM to %d+, then you can use %s %s, which may have fixed this problem.", higherJvmVersion, fmtName, higherFormatterVersionOrNull));
				}
			}
			return builder.toString();
		}

		@Override
		public String toString() {
			return String.format("%s alternatives:%n", fmtName) +
					jvm2fmtVersion.entrySet().stream().map(
							e -> String.format("- Version %s requires JVM %d+", e.getValue(), e.getKey())).collect(Collectors.joining(System.lineSeparator()));
		}

		@SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
		private static class SemanticVersionComparator<V> implements Comparator<V> {

			@Override
			public int compare(V version0, V version1) {
				Objects.requireNonNull(version0);
				Objects.requireNonNull(version1);
				int[] version0Items = convert(version0);
				int[] version1Items = convert(version1);
				int numberOfElements = Math.max(version0Items.length, version1Items.length);
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
					return Arrays.stream(versionObject.toString().split("\\.")).mapToInt(Integer::parseInt).toArray();
				} catch (Exception e) {
					throw new IllegalArgumentException(String.format("Not a semantic version: %s", versionObject), e);
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
}
