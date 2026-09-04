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
package lombok.core;

/**
 * Stub implementation of {@code lombok.core.FieldAugment} used only within the
 * {@code FeatureClassLoader} isolation boundary.  The real FieldAugment is
 * loaded by the lombok java-agent and is never reachable from Spotless's
 * feature class-loader; this stub satisfies the static references that ECJ's
 * patched classes make when lombok is active as a JVM agent.
 *
 * <p>{@link #augment} returns a no-op instance rather than {@code null} so that
 * callers such as {@code EcjAugments} can safely call {@code .get()},
 * {@code .set()}, etc. on the returned object without a {@link NullPointerException}.
 */
@SuppressWarnings("unused")
public abstract class FieldAugment<T, F> {

	/** Returns a non-null no-op augment so callers can safely invoke instance methods on it. */
	@SuppressWarnings("unchecked")
	public static <T, F> FieldAugment<T, F> augment(Class<T> type, Class<? super F> fieldType, String name) {
		return (FieldAugment<T, F>) NoopFieldAugment.INSTANCE;
	}

	/** Returns a non-null no-op augment so callers can safely invoke instance methods on it. */
	@SuppressWarnings("unchecked")
	public static <T, F> FieldAugment<T, F> circularSafeAugment(Class<T> type, Class<? super F> fieldType, String name) {
		return (FieldAugment<T, F>) NoopFieldAugment.INSTANCE;
	}

	public abstract F get(T object);

	public abstract void set(T object, F value);

	public abstract F getAndSet(T object, F value);

	public abstract F clear(T object);

	public abstract F compareAndClear(T object, F expected);

	public abstract F setIfAbsent(T object, F value);

	public abstract F compareAndSet(T object, F expected, F value);

	/** Singleton no-op implementation returned by {@link #augment} and {@link #circularSafeAugment}. */
	@SuppressWarnings("rawtypes")
	private static final class NoopFieldAugment extends FieldAugment {
		static final NoopFieldAugment INSTANCE = new NoopFieldAugment();

		@Override
		public Object get(Object object) {
			return null;
		}

		@Override
		public void set(Object object, Object value) {}

		@Override
		public Object getAndSet(Object object, Object value) {
			return null;
		}

		@Override
		public Object clear(Object object) {
			return null;
		}

		@Override
		public Object compareAndClear(Object object, Object expected) {
			return null;
		}

		@Override
		public Object setIfAbsent(Object object, Object value) {
			return null;
		}

		@Override
		public Object compareAndSet(Object object, Object expected, Object value) {
			return null;
		}
	}
}
