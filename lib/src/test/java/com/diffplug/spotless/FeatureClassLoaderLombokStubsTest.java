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
package com.diffplug.spotless;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link FeatureClassLoader} resolves the bundled lombok stubs
 * for the three class families that ECJ references when lombok is active as a
 * JVM agent.
 *
 * <p>The tests instantiate a {@code FeatureClassLoader} with an empty URL array
 * (no feature JARs) and confirm that each stubbed class can be loaded and has
 * the expected members so that ECJ's static initializers can complete without
 * a {@link NoClassDefFoundError}.
 */
class FeatureClassLoaderLombokStubsTest {

	private FeatureClassLoader loader;

	@BeforeEach
	void setUp() {
		// Empty URL array – we only need the stubs that are bundled in the lib jar itself.
		loader = new FeatureClassLoader(new URL[0], FeatureClassLoaderLombokStubsTest.class.getClassLoader());
	}

	// -------------------------------------------------------------------------
	// lombok.core.FieldAugment
	// -------------------------------------------------------------------------

	@Test
	void fieldAugment_canBeLoaded() throws ClassNotFoundException {
		Class<?> clazz = loader.loadClass("lombok.core.FieldAugment");
		assertThat(clazz).isNotNull();
	}

	@Test
	void fieldAugment_augmentMethodExists() throws Exception {
		Class<?> clazz = loader.loadClass("lombok.core.FieldAugment");
		Method augment = clazz.getMethod("augment", Class.class, Class.class, String.class);
		assertThat(Modifier.isStatic(augment.getModifiers())).isTrue();
		// Must return a non-null instance so callers can safely invoke .get() etc.
		assertThat(augment.invoke(null, String.class, String.class, "x")).isNotNull();
	}

	// -------------------------------------------------------------------------
	// lombok.eclipse.EcjAugments
	// -------------------------------------------------------------------------

	@Test
	void ecjAugments_canBeLoaded() throws ClassNotFoundException {
		Class<?> clazz = loader.loadClass("lombok.eclipse.EcjAugments");
		assertThat(clazz).isNotNull();
	}

	@Test
	void ecjAugments_astNodeGeneratedByFieldExists() throws Exception {
		Class<?> clazz = loader.loadClass("lombok.eclipse.EcjAugments");
		Field field = clazz.getField("ASTNode_generatedBy");
		assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
		// Must be non-null so ECJ's patched ASTConverter can call .get() on it without NPE.
		assertThat(field.get(null)).isNotNull();
	}

	@Test
	void ecjAugments_innerClassEclipseAugmentsExists() throws ClassNotFoundException {
		assertThatNoException().isThrownBy(() -> loader.loadClass("lombok.eclipse.EcjAugments$EclipseAugments"));
	}

	// -------------------------------------------------------------------------
	// lombok.launch.PatchFixesHider and inner classes
	// -------------------------------------------------------------------------

	@Test
	void patchFixesHider_moduleClassLoadingCanBeLoaded() throws ClassNotFoundException {
		Class<?> clazz = loader.loadClass("lombok.launch.PatchFixesHider$ModuleClassLoading");
		assertThat(clazz).isNotNull();
	}

	@Test
	void patchFixesHider_moduleClassLoading_parserClinitIsCallable() throws Exception {
		Class<?> clazz = loader.loadClass("lombok.launch.PatchFixesHider$ModuleClassLoading");
		Method m = clazz.getMethod("parserClinit");
		assertThat(Modifier.isStatic(m.getModifiers())).isTrue();
		// Must not throw – this is what ECJ's Parser.<clinit> calls.
		assertThatNoException().isThrownBy(() -> m.invoke(null));
	}

	@Test
	void patchFixesHider_transformCanBeLoaded() throws ClassNotFoundException {
		assertThatNoException().isThrownBy(() -> loader.loadClass("lombok.launch.PatchFixesHider$Transform"));
	}

	@Test
	void patchFixesHider_patchFixesCanBeLoaded() throws ClassNotFoundException {
		assertThatNoException().isThrownBy(() -> loader.loadClass("lombok.launch.PatchFixesHider$PatchFixes"));
	}

	// -------------------------------------------------------------------------
	// synthesiseEmptyClass fallback (no bundled stub available)
	// -------------------------------------------------------------------------

	@Test
	void unknownLombokClass_isSynthesisedAsEmptyClass() throws ClassNotFoundException {
		// PatchDiagnostics has no hand-written stub; the loader must synthesise one.
		Class<?> clazz = loader.loadClass("lombok.eclipse.agent.PatchDiagnostics");
		assertThat(clazz).isNotNull();
		assertThat(clazz.getName()).isEqualTo("lombok.eclipse.agent.PatchDiagnostics");
		// Synthesised class extends Object and declares no methods beyond <init>.
		assertThat(clazz.getSuperclass()).isEqualTo(Object.class);
		assertThat(clazz.getDeclaredMethods()).isEmpty();
	}

	@Test
	void unknownLombokClass_canBeInstantiated() throws Exception {
		Class<?> clazz = loader.loadClass("lombok.eclipse.agent.PatchDiagnostics");
		// The synthesised class has a default public <init>; instantiation must succeed.
		assertThatNoException().isThrownBy(() -> clazz.getDeclaredConstructor().newInstance());
	}
}
