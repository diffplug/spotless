/*
 * Copyright 2016-2021 DiffPlug
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
	private static final int JAVA_VERSION;

	static {
		String jre = System.getProperty("java.version");
		if (jre.startsWith("1.8")) {
			JAVA_VERSION = 8;
		} else {
			Matcher matcher = Pattern.compile("(\\d+)").matcher(jre);
			if (!matcher.find()) {
				throw new IllegalArgumentException("Expected " + jre + " to start with an integer");
			}
			JAVA_VERSION = Integer.parseInt(matcher.group(1));
			if (JAVA_VERSION <= 8) {
				throw new IllegalArgumentException("Expected " + jre + " to start with an integer greater than 8");
			}
		}
	}

	/** @return the major version of this VM, e.g. 8, 9, 10, 11, 13, etc. */
	public static int version() {
		return JAVA_VERSION;
	}

	/**
	 * Utility to map constraints of formatter to this JVM
	 * @param <V> Version type of formatter
	 */
	public static class Support<V> {
		private final NavigableMap<Integer, V> java2formatterVersion;
		private final String formatterName;
		private final Comparator<? super V> formatterVersionComparator;
		private Integer requiredJavaVersion;

		private Support(String fromatterName) {
			this(fromatterName, null);
		}

		private Support(String formatterName, @Nullable Comparator<? super V> formatterVersionComparator) {
			java2formatterVersion = new TreeMap<Integer, V>();
			this.formatterName = formatterName;
			if (null == formatterVersionComparator) {
				this.formatterVersionComparator = new SemanticVersionComparator<V>();
			} else {
				this.formatterVersionComparator = formatterVersionComparator;
			}
			requiredJavaVersion = 0;
		}

		/**
		 * Add supported formatter version
		 * @param minimumJavaVersion Minimum Java version required
		 * @param maxFormatterVersion Maximum formatter version supported by the Java version
		 * @return this
		 */
		public Support<V> add(int minimumJavaVersion, V maxFormatterVersion) {
			Objects.requireNonNull(maxFormatterVersion);
			java2formatterVersion.put(minimumJavaVersion, maxFormatterVersion);
			requiredJavaVersion = java2formatterVersion.floorKey(Jvm.version());
			return this;
		}

		/** @return Latest formatter version supported by this JVM */
		public V getLatestFormatterVersion() {
			V latestFormatterVersion = java2formatterVersion.get(requiredJavaVersion);
			if (null == latestFormatterVersion) {
				throw new UnsupportedClassVersionError("Unsupported JVM: " + toString());
			}
			return latestFormatterVersion;
		}

		/**
		 * Assert the formatter is supported
		 * @param formatterVersion Formatter version
		 */
		public void assertFormatterSupported(V formatterVersion) throws Exception {
			Objects.requireNonNull(formatterVersion);
			if (formatterVersionComparator.compare(formatterVersion, getLatestFormatterVersion()) > 0) {
				throw new UnsupportedClassVersionError(String.format("Unsupported formatter version %s: %s", formatterVersion, toString()));
			}
		}

		/**
		 * Suggest to use a later formatter version if formatting fails
		 * @param formatterVersion Formatter version
		 * @param originalFunc Formatter function
		 * @return Wrapped formatter function. Adding hint about later versions to exceptions.
		 */
		public FormatterFunc suggestLaterVersionOnError(V formatterVersion, FormatterFunc originalFunc) {
			if (formatterVersionComparator.compare(formatterVersion, getLatestFormatterVersion()) < 0) {
				return new FormatterFuncWrapper(String.format("You are not using latest version for Java %d and later. Try to upgrade to %s version %s, which may have fixed this problem.", requiredJavaVersion, formatterName, getLatestFormatterVersion()), originalFunc);
			} else if (java2formatterVersion.lastKey() > Jvm.version()) {
				Integer nextJavaVersion = java2formatterVersion.higherKey(Jvm.version());
				V nextFormatterVersion = java2formatterVersion.get(nextJavaVersion);
				return new FormatterFuncWrapper(String.format("You are running Spotless on JRE %1$d, which limits you to %2$s %3$s.%nIf you upgrade your build JVM to %4$d+, then you can use %2$s %5$s, which may have fixed this problem.", Jvm.version(), formatterName, formatterVersion, nextJavaVersion, nextFormatterVersion), originalFunc);
			}
			return originalFunc;
		}

		@Override
		public String toString() {
			return String.format("Java version %d not supported by %s %s.%n", Jvm.version(), formatterName) +
					java2formatterVersion.entrySet().stream().map(
							entry -> String.format("- Version %s requires Java version %d", entry.getValue().toString(), entry.getKey())).collect(Collectors.joining(System.lineSeparator()));
		}

		@SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
		private static class SemanticVersionComparator<V> implements Comparator<V> {

			@Override
			public int compare(V version0, V version1) {
				Integer[] version0Items = convert(version0);
				Integer[] version1Items = convert(version1);
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

			private static <V> Integer[] convert(V versionObject) {
				try {
					Objects.requireNonNull(versionObject);
					return Arrays.asList(versionObject.toString().split("\\.")).stream().map(s -> Integer.valueOf(s)).toArray(Integer[]::new);
				} catch (Exception e) {
					throw new IllegalArgumentException(String.format("Not a semantic version: %s", versionObject), e);
				}
			}
		};

		private static class FormatterFuncWrapper implements FormatterFunc {
			private final String hintLaterVersion;
			private final FormatterFunc originalFunc;

			FormatterFuncWrapper(String hintLaterVersion, FormatterFunc originalFunc) {
				this.hintLaterVersion = hintLaterVersion;
				this.originalFunc = originalFunc;
			}

			@Override
			public String apply(String unix, File file) throws Exception {
				try {
					return originalFunc.apply(unix, file);
				} catch (Exception e) {
					throw new Exception(hintLaterVersion, e);
				}
			}

			@Override
			public String apply(String input) throws Exception {
				try {
					return originalFunc.apply(input);
				} catch (Exception e) {
					throw new Exception(hintLaterVersion, e);
				}
			}

		};

	}

	/**
	 * Creates a map of JVM requirements for a formatter
	 * @param <V> Version type of the formatter (V#toString() must correspond to a semantic version, separated by dots)
	 * @param formatterName Name of the formatter
	 * @return Empty map of supported formatters
	 */
	public static <V> Support<V> support(String formatterName) {
		return new Support<V>(formatterName);
	}
}
