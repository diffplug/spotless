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
package lombok.eclipse;

import lombok.core.FieldAugment;

/**
 * Stub implementation of {@code lombok.eclipse.EcjAugments} used only within
 * the {@code FeatureClassLoader} isolation boundary.
 *
 * <p>Each field is initialised via {@link FieldAugment#augment} so that callers
 * (such as ECJ's patched {@code ASTConverter}) can safely invoke instance
 * methods like {@code .get()} and {@code .set()} on them without a
 * {@link NullPointerException}.  The augment instances are no-ops that always
 * return {@code null}.
 */
@SuppressWarnings({"unused", "rawtypes"})
public final class EcjAugments {

	private EcjAugments() {
		// prevent instantiation
	}

	public static final FieldAugment ASTNode_generatedBy = FieldAugment.augment(Object.class, Object.class, "$generatedBy");
	public static final FieldAugment ASTNode_handled = FieldAugment.augment(Object.class, boolean.class, "lombok$handled");
	public static final FieldAugment ASTNode_tokens = FieldAugment.augment(Object.class, Object.class, "lombok$tokens");
	public static final FieldAugment FieldDeclaration_booleanLazyGetter = FieldAugment.augment(Object.class, boolean.class, "lombok$booleanLazyGetter");
	public static final FieldAugment Annotation_applied = FieldAugment.augment(Object.class, boolean.class, "lombok$applied");
	public static final FieldAugment CompilationUnit_javadoc = FieldAugment.augment(Object.class, Object.class, "$javadoc");
	public static final FieldAugment CompilationUnitDeclaration_transformationState = FieldAugment.augment(Object.class, Object.class, "$transformationState");

	/** Stub inner class mirroring {@code EcjAugments.EclipseAugments}. */
	public static final class EclipseAugments {
		private EclipseAugments() {}

		public static final FieldAugment CompilationUnit_delegateMethods = FieldAugment.augment(Object.class, Object.class, "$delegateMethods");
	}
}
