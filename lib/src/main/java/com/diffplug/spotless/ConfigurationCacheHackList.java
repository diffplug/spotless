/*
 * Copyright 2024-2026 DiffPlug
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import com.diffplug.spotless.yaml.SerializeToByteArrayHack;

/**
 * Gradle requires three things:
 * - Gradle defines cache equality based on your serialized representation
 * - Combined with remote build cache, you cannot have any absolute paths in
 * your serialized representation
 * - Combined with configuration cache, you must be able to roundtrip yourself
 * through serialization
 *
 * These requirements are at odds with each other, as described in these issues
 * - Gradle issue to define custom equality
 * https://github.com/gradle/gradle/issues/29816
 * - Spotless plea for developer cache instead of configuration cache
 * https://github.com/diffplug/spotless/issues/987
 * - Spotless cache miss bug fixed by this class
 * https://github.com/diffplug/spotless/issues/2168
 *
 * This class is a `List<FormatterStep>` which can optimize the
 * serialized representation for either
 * - roundtrip integrity
 * - OR
 * - equality
 *
 * Because it is not possible to provide both at the same time.
 * It is a horrific hack, but it works, and it's the only way I can figure
 * to make Spotless work with all of Gradle's cache systems at once.
 */
public final class ConfigurationCacheHackList implements Serializable {
	@Serial
	private static final long serialVersionUID = 6914178791997323870L;

	private boolean optimizeForEquality;
	private ArrayList<Object> backingList = new ArrayList<>();

	/**
	 * The failure from the most recent serialization attempt, if any. Not part of the
	 * serialized form - it exists only so {@link #toString()} can report why serialization
	 * failed without re-evaluating any step state.
	 */
	@Nullable private transient volatile String serializationFailure;

	private boolean shouldWeSerializeToByteArrayFirst() {
		return backingList.stream().anyMatch(SerializeToByteArrayHack.class::isInstance);
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		try {
			writeSteps(out);
		} catch (IOException | RuntimeException e) {
			// Gradle reports a fingerprinting failure as "value '<toString()>' cannot be
			// serialized" and discards the cause, so stash it where toString() can report
			// it. Otherwise the actionable message (e.g. "P2 dependencies not predeclared")
			// is lost and the user only sees "cannot be serialized". See #3004.
			serializationFailure = describeFailure(e);
			throw e;
		}
	}

	/** Walks the cause chain so nested messages survive into {@link #toString()}. */
	private static String describeFailure(Throwable e) {
		StringBuilder causes = new StringBuilder();
		Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Throwable t = e; t != null && seen.add(t); t = t.getCause()) {
			String message = t.getMessage();
			if (message != null && !message.isEmpty()) {
				if (causes.length() > 0) {
					causes.append(" > ");
				}
				causes.append(message);
			}
		}
		return causes.length() == 0 ? e.getClass().getName() : causes.toString();
	}

	private void writeSteps(ObjectOutputStream out) throws IOException {
		boolean serializeToByteArrayFirst = shouldWeSerializeToByteArrayFirst();
		out.writeBoolean(serializeToByteArrayFirst);
		out.writeBoolean(optimizeForEquality);
		out.writeInt(backingList.size());
		for (Object obj : backingList) {
			// if write out the list on its own, we'll get java's non-deterministic object-graph serialization
			// by writing each object to raw bytes independently, we avoid this
			if (serializeToByteArrayFirst) {
				out.writeObject(LazyForwardingEquality.toBytes((Serializable) obj));
			} else {
				out.writeObject(obj);
			}
		}
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		boolean serializeToByteArrayFirst = in.readBoolean();
		optimizeForEquality = in.readBoolean();
		backingList = new ArrayList<>();
		int size = in.readInt();
		for (int i = 0; i < size; i++) {
			if (serializeToByteArrayFirst) {
				backingList.add(LazyForwardingEquality.fromBytes((byte[]) in.readObject()));
			} else {
				backingList.add(in.readObject());
			}
		}
	}

	public static ConfigurationCacheHackList forEquality() {
		return new ConfigurationCacheHackList(true);
	}

	public static ConfigurationCacheHackList forRoundtrip() {
		return new ConfigurationCacheHackList(false);
	}

	private ConfigurationCacheHackList(boolean optimizeForEquality) {
		this.optimizeForEquality = optimizeForEquality;
	}

	public void clear() {
		backingList.clear();
	}

	public void addAll(Collection<? extends FormatterStep> c) {
		for (FormatterStep step : c) {
			if (step instanceof FormatterStepSerializationRoundtrip roundtrip) {
				var clone = roundtrip.hackClone(optimizeForEquality);
				backingList.add(clone);
			} else {
				backingList.add(step);
			}
		}
	}

	public List<FormatterStep> getSteps() {
		var result = new ArrayList<FormatterStep>(backingList.size());
		for (Object obj : backingList) {
			if (obj instanceof FormatterStepSerializationRoundtrip.HackClone clone) {
				result.add(clone.rehydrate());
			} else {
				result.add((FormatterStep) obj);
			}
		}
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ConfigurationCacheHackList stepList = (ConfigurationCacheHackList) o;
		return optimizeForEquality == stepList.optimizeForEquality
				&& backingList.equals(stepList.backingList);
	}

	@Override
	public int hashCode() {
		return Objects.hash(optimizeForEquality, backingList);
	}

	/**
	 * Must not call {@link #hashCode()} — that fingerprints every step and may provision
	 * P2/Maven deps. Gradle includes this value in "cannot be serialized" messages, so a
	 * side-effecting {@code toString} re-triggers provisioning while the build is already
	 * failing (see <a href="https://github.com/diffplug/spotless/issues/3004">#3004</a>).
	 * <p>
	 * Gradle builds that message only after serialization has already thrown, so any
	 * failure is reported from {@link #serializationFailure} rather than by re-evaluating
	 * the steps. That keeps actionable errors such as "P2 dependencies not predeclared"
	 * visible to the user.
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getName())
				.append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append("[optimizeForEquality=").append(optimizeForEquality)
				.append(", size=").append(backingList.size());
		String failure = serializationFailure;
		if (failure != null) {
			builder.append(", failure=").append(failure);
		}
		return builder.append(']').toString();
	}
}
