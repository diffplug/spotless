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
package lombok.patcher;

import java.util.Collections;
import java.util.List;

/**
 * Stub implementation of {@code lombok.patcher.Symbols} used only within the
 * {@code FeatureClassLoader} isolation boundary.
 *
 * <p>When lombok is active as a JVM agent it patches ECJ's
 * {@code DefaultCodeFormatter} (and related classes) to call methods on
 * {@code lombok.patcher.Symbols}.  That class lives inside the lombok jar
 * under a shadow class-loader prefix ({@code SCL.lombok/}) and is never
 * reachable as a normal class from {@code FeatureClassLoader}.  This stub
 * satisfies the reference so that ECJ's patched code can load without a
 * {@link NoClassDefFoundError}.  Every method is a safe no-op.
 */
@SuppressWarnings("unused")
public class Symbols {

	private Symbols() {}

	/** Stub – no-op. */
	public static void push(String symbol) {}

	/** Stub – no-op. */
	public static void pop() {}

	/** Stub – always returns {@code true} (no symbols active). */
	public static boolean isEmpty() {
		return true;
	}

	/** Stub – always returns {@code 0}. */
	public static int size() {
		return 0;
	}

	/** Stub – always returns {@code false}. */
	public static boolean hasSymbol(String symbol) {
		return false;
	}

	/** Stub – always returns {@code false}. */
	public static boolean hasTail(String symbol) {
		return false;
	}

	/** Stub – always returns an empty list. */
	public static List<String> getCopy() {
		return Collections.emptyList();
	}
}
