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
 * Stub implementation of {@code lombok.eclipse.agent.PatchValEclipsePortal}
 * used only within the {@code FeatureClassLoader} isolation boundary.
 * All methods are no-ops.
 */
@SuppressWarnings("unused")
public class PatchValEclipsePortal {

	public static void copyInitializationOfForEachIterable(Object parser) {}

	public static void copyInitializationOfLocalDeclaration(Object parser) {}

	public static void addFinalAndValAnnotationToVariableDeclarationStatement(Object converter, Object out, Object in) {}

	public static void addFinalAndValAnnotationToSingleVariableDeclaration(Object converter, Object out, Object in) {}
}
