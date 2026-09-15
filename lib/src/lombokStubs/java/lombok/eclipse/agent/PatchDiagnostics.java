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
 * Stub implementation of {@code lombok.eclipse.agent.PatchDiagnostics} used
 * only within the {@code FeatureClassLoader} isolation boundary.
 *
 * <p>When lombok is active as a JVM agent it patches ECJ's
 * {@code ASTNode.setSourceRange} to call
 * {@code PatchDiagnostics.setSourceRangeCheck}.  The real class lives in
 * lombok's shadow class-loader and is never reachable from
 * {@code FeatureClassLoader}.  This stub satisfies the call site so that
 * source-range setting proceeds without a {@link NoSuchMethodError}.
 * Returning {@code false} means "no override" — ECJ proceeds with its
 * normal source-range assignment.
 */
@SuppressWarnings("unused")
public class PatchDiagnostics {

	/** Stub – always returns {@code false} so ECJ applies its normal source-range logic. */
	public static boolean setSourceRangeCheck(Object node, int start, int end) {
		return false;
	}
}
