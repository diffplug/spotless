/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.cli.core;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.diffplug.common.hash.Hashing;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.cli.SpotlessAction;
import com.diffplug.spotless.cli.steps.SpotlessCLIFormatterStep;

import picocli.CommandLine;

public class ChecksumCalculator {

	public String calculateChecksum(SpotlessCLIFormatterStep step) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			writeObjectDataTo(step, out);
			return toHashedHexBytes(out.toByteArray());
		} catch (Exception e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	private void writeObjectDataTo(Object object, OutputStream outputStream) {
		ThrowingEx.run(() -> outputStream.write(object.getClass().getName().getBytes(StandardCharsets.UTF_8)));
		options(object)
				.map(Object::toString)
				.map(str -> str.getBytes(StandardCharsets.UTF_8))
				.forEachOrdered(bytes -> ThrowingEx.run(() -> outputStream.write(bytes)));

	}

	public String calculateChecksum(SpotlessCommandLineStream commandLineStream) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			calculateChecksumOfActions(commandLineStream.actions(), out);

			calculateChecksumOfSteps(commandLineStream.formatterSteps(), out);
			return toHashedHexBytes(out.toByteArray());
		} catch (Exception e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	private void calculateChecksumOfSteps(Stream<SpotlessCLIFormatterStep> spotlessCLIFormatterStepStream, ByteArrayOutputStream out) {
		spotlessCLIFormatterStepStream.forEachOrdered(step -> writeObjectDataTo(step, out));
	}

	private void calculateChecksumOfActions(Stream<SpotlessAction> actions, ByteArrayOutputStream out) {
		actions.forEachOrdered(action -> writeObjectDataTo(action, out));
	}

	private static Stream<Object> options(Object step) {
		List<Class<?>> classHierarchy = classHierarchy(step);
		return classHierarchy.stream()
				.flatMap(clazz -> Arrays.stream(clazz.getDeclaredFields()))
				.flatMap(field -> expandOptionField(field, step))
				.map(FieldOnObject::getValue)
				.filter(Objects::nonNull);
	}

	private static List<Class<?>> classHierarchy(Object obj) {
		List<Class<?>> hierarchy = new ArrayList<>();
		Class<?> clazz = obj.getClass();
		while (clazz != null) {
			hierarchy.add(clazz);
			clazz = clazz.getSuperclass();
		}
		return hierarchy;
	}

	private static Stream<FieldOnObject> expandOptionField(Field field, Object obj) {
		if (field.isAnnotationPresent(CommandLine.Option.class) || field.isAnnotationPresent(CommandLine.Parameters.class)) {
			return Stream.of(new FieldOnObject(field, obj));
		}
		if (field.isAnnotationPresent(CommandLine.ArgGroup.class)) {
			Object fieldValue = new FieldOnObject(field, obj).getValue();
			return Arrays.stream(fieldValue.getClass().getDeclaredFields())
					.flatMap(subField -> expandOptionField(subField, fieldValue));
		}
		return Stream.empty(); // nothing to expand
	}

	private static String toHashedHexBytes(byte[] bytes) {
		byte[] hash = Hashing.murmur3_128().hashBytes(bytes).asBytes();
		StringBuilder builder = new StringBuilder();
		for (byte b : hash) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}

	private static class FieldOnObject {
		private final Field field;
		private final Object obj;

		FieldOnObject(Field field, Object obj) {
			this.field = field;
			this.obj = obj;
		}

		Object getValue() {
			ThrowingEx.run(() -> field.setAccessible(true));
			return ThrowingEx.get(() -> field.get(obj));
		}
	}
}
