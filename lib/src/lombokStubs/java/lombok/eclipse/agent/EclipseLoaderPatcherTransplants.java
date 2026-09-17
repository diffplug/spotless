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
package lombok.eclipse.agent;

/**
 * Stub implementation of {@code lombok.eclipse.agent.EclipseLoaderPatcherTransplants}
 * used only within the {@code FeatureClassLoader} isolation boundary.
 */
@SuppressWarnings("unused")
public class EclipseLoaderPatcherTransplants {

	/** Stub – returns {@code false} so the normal class-loading path is used. */
	public static boolean overrideLoadDecide(ClassLoader classLoader, String name, boolean resolve) {
		return false;
	}

	/** Stub – returns {@code null} so the caller falls through to normal loading. */
	public static Class<?> overrideLoadResult(ClassLoader classLoader, String name, boolean resolve) throws ClassNotFoundException {
		return null;
	}
}
