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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Stub implementation of {@code lombok.eclipse.agent.PatchFixesShadowLoaded}
 * used only within the {@code FeatureClassLoader} isolation boundary.
 * Methods return their input unchanged; post-compiler steps are no-ops.
 */
@SuppressWarnings("unused")
public class PatchFixesShadowLoaded {

	/** Stub – returns {@code origReturnValue} unchanged. */
	public static String addLombokNotesToEclipseAboutDialog(String origReturnValue, String key) {
		return origReturnValue;
	}

	/** Stub – returns {@code bytes} unchanged. */
	public static byte[] runPostCompiler(byte[] bytes, String fileName) {
		return bytes;
	}

	/** Stub – returns {@code out} unchanged. */
	public static OutputStream runPostCompiler(OutputStream out) throws IOException {
		return out;
	}

	/** Stub – returns {@code out} unchanged. */
	public static BufferedOutputStream runPostCompiler(BufferedOutputStream out, String path, String name) throws IOException {
		return out;
	}
}
