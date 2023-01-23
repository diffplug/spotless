/*
 * Copyright 2023 DiffPlug
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RingBufferByteArrayOutputStreamTest {

	private final byte[] bytes = new byte[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

	@ParameterizedTest(name = "{index} writeStrategy: {0}")
	@MethodSource("writeStrategies")
	void toStringBehavesNormallyWithinLimit(String name, ByteWriteStrategy writeStrategy) {
		RingBufferByteArrayOutputStream stream = new RingBufferByteArrayOutputStream(12, 1);
		writeStrategy.write(stream, bytes);
		Assertions.assertThat(stream.toString()).isEqualTo("0123456789");
	}

	@ParameterizedTest(name = "{index} writeStrategy: {0}")
	@MethodSource("writeStrategies")
	void toStringBehavesOverwritingOverLimit(String name, ByteWriteStrategy writeStrategy) {
		RingBufferByteArrayOutputStream stream = new RingBufferByteArrayOutputStream(4, 1);
		writeStrategy.write(stream, bytes);
		Assertions.assertThat(stream.toString()).hasSize(4);
		Assertions.assertThat(stream.toString()).isEqualTo("6789");
	}

	@ParameterizedTest(name = "{index} writeStrategy: {0}")
	@MethodSource("writeStrategies")
	void toStringBehavesNormallyAtExactlyLimit(String name, ByteWriteStrategy writeStrategy) {
		RingBufferByteArrayOutputStream stream = new RingBufferByteArrayOutputStream(bytes.length, 1);
		writeStrategy.write(stream, bytes);
		Assertions.assertThat(stream.toString()).isEqualTo("0123456789");
	}

	@ParameterizedTest(name = "{index} writeStrategy: {0}")
	@MethodSource("writeStrategies")
	void toByteArrayBehavesNormallyWithinLimit(String name, ByteWriteStrategy writeStrategy) {
		RingBufferByteArrayOutputStream stream = new RingBufferByteArrayOutputStream(12, 1);
		writeStrategy.write(stream, bytes);
		Assertions.assertThat(stream.toByteArray()).isEqualTo(bytes);
	}

	@ParameterizedTest(name = "{index} writeStrategy: {0}")
	@MethodSource("writeStrategies")
	void toByteArrayBehavesOverwritingOverLimit(String name, ByteWriteStrategy writeStrategy) {
		RingBufferByteArrayOutputStream stream = new RingBufferByteArrayOutputStream(4, 1);
		writeStrategy.write(stream, bytes);
		Assertions.assertThat(stream.toByteArray()).hasSize(4);
		Assertions.assertThat(stream.toByteArray()).isEqualTo(new byte[]{'6', '7', '8', '9'});
	}

	@ParameterizedTest(name = "{index} writeStrategy: {0}")
	@MethodSource("writeStrategies")
	void toByteArrayBehavesOverwritingAtExactlyLimit(String name, ByteWriteStrategy writeStrategy) {
		RingBufferByteArrayOutputStream stream = new RingBufferByteArrayOutputStream(bytes.length, 1);
		writeStrategy.write(stream, bytes);
		Assertions.assertThat(stream.toByteArray()).isEqualTo(bytes);
	}

	@ParameterizedTest(name = "{index} writeStrategy: {0}")
	@MethodSource("writeStrategies")
	void writeToBehavesNormallyWithinLimit(String name, ByteWriteStrategy writeStrategy) throws IOException {
		RingBufferByteArrayOutputStream stream = new RingBufferByteArrayOutputStream(12, 1);
		writeStrategy.write(stream, bytes);
		ByteArrayOutputStream target = new ByteArrayOutputStream();
		stream.writeTo(target);
		Assertions.assertThat(target.toByteArray()).isEqualTo(bytes);
	}

	@ParameterizedTest(name = "{index} writeStrategy: {0}")
	@MethodSource("writeStrategies")
	void writeToBehavesOverwritingOverLimit(String name, ByteWriteStrategy writeStrategy) throws IOException {
		RingBufferByteArrayOutputStream stream = new RingBufferByteArrayOutputStream(4, 1);
		writeStrategy.write(stream, bytes);
		ByteArrayOutputStream target = new ByteArrayOutputStream();
		stream.writeTo(target);
		Assertions.assertThat(target.toByteArray()).hasSize(4);
		Assertions.assertThat(target.toByteArray()).isEqualTo(new byte[]{'6', '7', '8', '9'});
	}

	@ParameterizedTest(name = "{index} writeStrategy: {0}")
	@MethodSource("writeStrategies")
	void writeToBehavesNormallyAtExactlyLimit(String name, ByteWriteStrategy writeStrategy) throws IOException {
		RingBufferByteArrayOutputStream stream = new RingBufferByteArrayOutputStream(bytes.length, 1);
		writeStrategy.write(stream, bytes);
		ByteArrayOutputStream target = new ByteArrayOutputStream();
		stream.writeTo(target);
		Assertions.assertThat(target.toByteArray()).isEqualTo(bytes);
	}

	@Test
	void writeToBehavesCorrectlyWhenOverLimitMultipleCalls() {
		// this test explicitly captures a border case where the buffer is not empty but can exactly fit what we are writing
		RingBufferByteArrayOutputStream stream = new RingBufferByteArrayOutputStream(2, 1);
		stream.write('0');
		stream.write(new byte[]{'1', '2'}, 0, 2);
		Assertions.assertThat(stream.toString()).hasSize(2);
		Assertions.assertThat(stream.toString()).isEqualTo("12");
	}

	private static Stream<Arguments> writeStrategies() {
		return Stream.of(
				Arguments.of("writeAllAtOnce", allAtOnce()),
				Arguments.of("writeOneByteAtATime", oneByteAtATime()),
				Arguments.of("writeTwoBytesAtATime", twoBytesAtATime()),
				Arguments.of("writeOneAndThenTwoBytesAtATime", oneAndThenTwoBytesAtATime()),
				Arguments.of("firstFourBytesAndThenTheRest", firstFourBytesAndThenTheRest()));
	}

	private static ByteWriteStrategy allAtOnce() {
		return (stream, bytes) -> stream.write(bytes, 0, bytes.length);
	}

	private static ByteWriteStrategy oneByteAtATime() {
		return (stream, bytes) -> {
			for (byte b : bytes) {
				stream.write(b);
			}
		};
	}

	private static ByteWriteStrategy twoBytesAtATime() {
		return (stream, bytes) -> {
			for (int i = 0; i < bytes.length; i += 2) {
				stream.write(bytes, i, 2);
			}
		};
	}

	private static ByteWriteStrategy oneAndThenTwoBytesAtATime() {
		return (stream, bytes) -> {
			int written = 0;
			for (int i = 0; i + 3 < bytes.length; i += 3) {
				stream.write(bytes, i, 1);
				stream.write(bytes, i + 1, 2);
				written += 3;
			}
			if (written < bytes.length) {
				stream.write(bytes, written, bytes.length - written);
			}

		};
	}

	private static ByteWriteStrategy firstFourBytesAndThenTheRest() {
		return (stream, bytes) -> {
			stream.write(bytes, 0, 4);
			stream.write(bytes, 4, bytes.length - 4);
		};
	}

	@FunctionalInterface
	private interface ByteWriteStrategy {
		void write(RingBufferByteArrayOutputStream stream, byte[] bytes);
	}

}
