/*
 * Copyright 2026 DiffPlug
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
package com.diffplug.spotless.kotlin;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.ThrowingEx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Configuration-cache-safe promise for a generated classpath.
 * <p>
 * {@link JarState.Promised} and {@link FileSignature.Promised} represent signed file contents when
 * materialized. That is too early for project artifacts, which might not exist while Gradle serializes
 * formatter state. This promise serializes only the resolved file paths; Gradle tracks their contents as
 * a separate {@code @Classpath} task input, and {@link KtLintStep.State#createFormat()} signs them after the producer
 * tasks have run.
 * <p>
 * Resolution is synchronized because Gradle can serialize the roundtrip and equality views concurrently.
 */
final class PromisedClasspath implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	@SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "Serialized file paths replace the supplier after a configuration-cache roundtrip")
	private final transient ThrowingEx.Supplier<? extends Iterable<File>> supplier;
	@Nullable private volatile List<File> files;

	PromisedClasspath(ThrowingEx.Supplier<? extends Iterable<File>> supplier) {
		this.supplier = supplier;
	}

	List<File> get() {
		List<File> result = files;
		if (result == null) {
			synchronized (this) {
				result = files;
				if (result == null) {
					ThrowingEx.Supplier<? extends Iterable<File>> availableSupplier = Objects.requireNonNull(supplier, "supplier");
					List<File> suppliedFiles = new ArrayList<>();
					ThrowingEx.get(availableSupplier).forEach(suppliedFiles::add);
					files = result = List.copyOf(suppliedFiles);
				}
			}
		}
		return result;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		get();
		out.defaultWriteObject();
	}
}
