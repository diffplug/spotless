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
import java.io.Serializable;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * Represents the line endings which should be written by the tool.
 */
public enum LineEnding {
	// @formatter:off
	/** Uses the same line endings as Git, using {@code .gitattributes} and the {@code core.eol} property. */
	GIT_ATTRIBUTES {
		/** .gitattributes is path-specific, so you must use {@link LineEnding#createPolicy(File, Supplier)}. */
		@Override @Deprecated
		public Policy createPolicy() {
			return super.createPolicy();
		}
	},
	/** {@code \n} on unix systems, {@code \r\n} on windows systems. */
	PLATFORM_NATIVE,
	/** {@code \r\n} */
	WINDOWS,
    /** {@code \n} */
    UNIX,
    /** {@code \r} */
    MAC_CLASSIC;
	// @formatter:on

	/** Returns a {@link Policy} appropriate for files which are contained within the given rootFolder. */
	public Policy createPolicy(File projectDir, Supplier<Iterable<File>> toFormat) {
		Objects.requireNonNull(projectDir, "projectDir");
		Objects.requireNonNull(toFormat, "toFormat");
		if (this != GIT_ATTRIBUTES) {
			return createPolicy();
		} else {
			if (gitAttributesPolicyCreator == null) {
				try {
					Class<?> clazz = Class.forName("com.diffplug.spotless.extra.GitAttributesLineEndings");
					var method = clazz.getMethod("create", File.class, Supplier.class);
					gitAttributesPolicyCreator = (proj, target) -> ThrowingEx.get(() -> (Policy) method.invoke(null, proj, target));
				} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
					throw new IllegalStateException("LineEnding.GIT_ATTRIBUTES requires the spotless-lib-extra library, but it is not on the classpath", e);
				}
			}
			// gitAttributesPolicyCreator will always be nonnull at this point
			return gitAttributesPolicyCreator.apply(projectDir, toFormat);
		}
	}

	private static volatile @Nullable BiFunction<File, Supplier<Iterable<File>>, Policy> gitAttributesPolicyCreator;

	// @formatter:off
	/** Should use {@link #createPolicy(File, Supplier)} instead, but this will work iff its a path-independent LineEnding policy. */
	public Policy createPolicy() {
		switch (this) {
		case PLATFORM_NATIVE:	return _platformNativePolicy;
		case WINDOWS:			return WINDOWS_POLICY;
		case UNIX:				return UNIX_POLICY;
        case MAC_CLASSIC:       return MAC_CLASSIC_POLICY;
		default:	throw new UnsupportedOperationException(this + " is a path-specific line ending.");
		}
	}

	static class ConstantLineEndingPolicy extends NoLambda.EqualityBasedOnSerialization implements Policy {
		private static final long serialVersionUID = 1L;

		final String lineEnding;

		ConstantLineEndingPolicy(String lineEnding) {
			this.lineEnding = lineEnding;
		}

		@Override
		public String getEndingFor(File file) {
			return lineEnding;
		}
	}

	private static final Policy WINDOWS_POLICY = new ConstantLineEndingPolicy(WINDOWS.str());
	private static final Policy UNIX_POLICY = new ConstantLineEndingPolicy(UNIX.str());
    private static final Policy MAC_CLASSIC_POLICY = new ConstantLineEndingPolicy(MAC_CLASSIC.str());
	private static final String _platformNative = System.getProperty("line.separator");
	private static final Policy _platformNativePolicy = new ConstantLineEndingPolicy(_platformNative);
	private static final boolean nativeIsWin = _platformNative.equals(WINDOWS.str());

	/**
	 * @deprecated Using the system-native line endings to detect the windows operating system has turned out
	 * to be unreliable.  Use {@link FileSignature#machineIsWin()} instead.
	 *
	 * @see FileSignature#machineIsWin()
	 */
	@Deprecated
	public static boolean nativeIsWin() {
		return nativeIsWin;
	}

	/** Returns the standard line ending for this policy. */
	public String str() {
		switch (this) {
		case PLATFORM_NATIVE:	return _platformNative;
		case WINDOWS:			return "\r\n";
		case UNIX:				return "\n";
		case MAC_CLASSIC:       return "\r";
		default:	throw new UnsupportedOperationException(this + " is a path-specific line ending.");
		}
	}
	// @formatter:on

	/** A policy for line endings which can vary based on the specific file being requested. */
	public interface Policy extends Serializable, NoLambda {
		/** Returns the line ending appropriate for the given file. */
		String getEndingFor(File file);

		/** Returns true iff this file has unix line endings. */
		public default boolean isUnix(File file) {
			Objects.requireNonNull(file);
			var ending = getEndingFor(file);
			return ending.equals(UNIX.str());
		}
	}

	/** Returns a string with exclusively unix line endings. */
	public static String toUnix(String input) {
		var lastCarriageReturn = input.lastIndexOf('\r');
		if (lastCarriageReturn == -1) {
			return input;
		} else {
			if (input.lastIndexOf("\r\n") == -1) {
				// it is MAC_CLASSIC \r
				return input.replace('\r', '\n');
			} else {
				// it is WINDOWS \r\n
				return input.replace("\r", "");
			}
		}
	}
}
