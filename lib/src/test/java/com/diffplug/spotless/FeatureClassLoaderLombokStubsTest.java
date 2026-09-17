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

import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link FeatureClassLoader} resolves the bundled lombok stubs
 * for the class families that ECJ references when lombok is active as a JVM
 * agent, and that unknown {@code lombok.*} classes are synthesised on the fly.
 *
 * <p>The tests instantiate a {@code FeatureClassLoader} with an empty URL array
 * (no feature JARs) and confirm that each stubbed class can be loaded and has
 * the expected members so that ECJ's static initialisers can complete without
 * a {@link NoClassDefFoundError} or {@link NoSuchMethodError}.
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
	void patchFixesHider_patchFixes_isGeneratedOverloadsExist() throws Exception {
		// We cannot use getDeclaredMethods() / getParameterTypes() here because the
		// test JVM would try to resolve ECJ types (IMember, ASTNode, etc.) that are
		// not on the test classpath, causing NoClassDefFoundError.
		//
		// Instead we read the raw .class bytes and parse the constant pool directly.
		// The lombokStubs output directory is on the test classpath, so the resource
		// is reachable via the test classloader — no FeatureClassLoader indirection needed.
		String resourcePath = "lombok/launch/PatchFixesHider$PatchFixes.class";
		URL classUrl = FeatureClassLoaderLombokStubsTest.class.getClassLoader().getResource(resourcePath);
		assertThat(classUrl).as("PatchFixesHider$PatchFixes.class on test classpath").isNotNull();

		List<String> utf8Entries = readConstantPoolUtf8Entries(classUrl);

		// The method name "isGenerated" must appear (once, shared by all overloads).
		assertThat(utf8Entries).contains("isGenerated");

		// The two critical ECJ method descriptors that lombok transplants into ECJ bytecode
		// must be present so the JVM can link the invokestatic call sites at runtime.
		// (The constant pool stores full descriptors, not bare internal class names, when
		// a type only appears in method signatures rather than in new/checkcast instructions.)
		assertThat(utf8Entries).contains("(Lorg/eclipse/jdt/internal/compiler/ast/ASTNode;)Z");
		assertThat(utf8Entries).contains("(Lorg/eclipse/jdt/core/dom/ASTNode;)Z");
		assertThat(utf8Entries).contains("(Lorg/eclipse/jdt/core/IMember;)Z");
	}

	/**
	 * Reads all {@code CONSTANT_Utf8} entries from a class file's constant pool.
	 * Parses only the constant pool (stops before the class declaration), so it
	 * works on any valid class file without needing any external library.
	 */
	private static List<String> readConstantPoolUtf8Entries(URL classUrl) throws Exception {
		List<String> entries = new ArrayList<>();
		try (InputStream raw = classUrl.openStream();
				DataInputStream in = new DataInputStream(raw)) {
			// magic (4) + minor_version (2) + major_version (2)
			in.skipBytes(8);
			int cpCount = in.readUnsignedShort(); // constant_pool_count
			for (int i = 1; i < cpCount; i++) {
				int tag = in.readUnsignedByte();
				switch (tag) {
				case 1: // CONSTANT_Utf8
					entries.add(in.readUTF());
					break;
				case 3: // CONSTANT_Integer
				case 4: // CONSTANT_Float
					in.skipBytes(4);
					break;
				case 5: // CONSTANT_Long
				case 6: // CONSTANT_Double
					in.skipBytes(8);
					i++; // long/double take two slots
					break;
				case 7: // CONSTANT_Class
				case 8: // CONSTANT_String
				case 16: // CONSTANT_MethodType
				case 19: // CONSTANT_Module
				case 20: // CONSTANT_Package
					in.skipBytes(2);
					break;
				case 9: // CONSTANT_Fieldref
				case 10: // CONSTANT_Methodref
				case 11: // CONSTANT_InterfaceMethodref
				case 12: // CONSTANT_NameAndType
				case 17: // CONSTANT_Dynamic
				case 18: // CONSTANT_InvokeDynamic
					in.skipBytes(4);
					break;
				case 15: // CONSTANT_MethodHandle
					in.skipBytes(3);
					break;
				default:
					throw new IllegalStateException("Unknown constant pool tag " + tag + " at index " + i);
				}
			}
		}
		return entries;
	}

	// -------------------------------------------------------------------------
	// lombok.eclipse.agent stubs
	// -------------------------------------------------------------------------

	@Test
	void patchDiagnostics_setSourceRangeCheckIsCallable() throws Exception {
		Class<?> clazz = loader.loadClass("lombok.eclipse.agent.PatchDiagnostics");
		Method m = clazz.getMethod("setSourceRangeCheck", Object.class, int.class, int.class);
		assertThat(Modifier.isStatic(m.getModifiers())).isTrue();
		// Must return false so ECJ applies its normal source-range logic.
		assertThat(m.invoke(null, new Object(), 0, 10)).isEqualTo(false);
	}

	@Test
	void patchValEclipsePortal_copyInitializationMethodsExist() throws Exception {
		Class<?> clazz = loader.loadClass("lombok.eclipse.agent.PatchValEclipsePortal");
		Method m = clazz.getMethod("copyInitializationOfForEachIterable", Object.class);
		assertThat(Modifier.isStatic(m.getModifiers())).isTrue();
		assertThatNoException().isThrownBy(() -> m.invoke(null, new Object()));
	}

	@Test
	void patchDelegatePortal_handleDelegateForTypeReturnsFalse() throws Exception {
		Class<?> clazz = loader.loadClass("lombok.eclipse.agent.PatchDelegatePortal");
		Method m = clazz.getMethod("handleDelegateForType", Object.class);
		assertThat(Modifier.isStatic(m.getModifiers())).isTrue();
		assertThat(m.invoke(null, new Object())).isEqualTo(false);
	}

	@Test
	void patchFixesShadowLoaded_addLombokNotesReturnsOriginal() throws Exception {
		Class<?> clazz = loader.loadClass("lombok.eclipse.agent.PatchFixesShadowLoaded");
		Method m = clazz.getMethod("addLombokNotesToEclipseAboutDialog", String.class, String.class);
		assertThat(Modifier.isStatic(m.getModifiers())).isTrue();
		assertThat(m.invoke(null, "original", "key")).isEqualTo("original");
	}

	@Test
	void patchJavadoc_getHTMLContentReturnsOriginal() throws Exception {
		Class<?> clazz = loader.loadClass("lombok.eclipse.agent.PatchJavadoc");
		Method m = clazz.getMethod("getHTMLContentFromSource", Object.class, String.class, Object.class);
		assertThat(Modifier.isStatic(m.getModifiers())).isTrue();
		assertThat(m.invoke(null, null, "original", null)).isEqualTo("original");
	}

	@Test
	void eclipseLoaderPatcherTransplants_overrideLoadDecideReturnsFalse() throws Exception {
		Class<?> clazz = loader.loadClass("lombok.eclipse.agent.EclipseLoaderPatcherTransplants");
		Method m = clazz.getMethod("overrideLoadDecide", ClassLoader.class, String.class, boolean.class);
		assertThat(Modifier.isStatic(m.getModifiers())).isTrue();
		assertThat(m.invoke(null, getClass().getClassLoader(), "some.Class", false)).isEqualTo(false);
	}

	// -------------------------------------------------------------------------
	// synthesiseEmptyClass fallback (no bundled stub available)
	// -------------------------------------------------------------------------

	@Test
	void unknownLombokClass_isSynthesisedAsEmptyClass() throws ClassNotFoundException {
		// EclipsePatcher has no hand-written stub; the loader must synthesise one.
		Class<?> clazz = loader.loadClass("lombok.eclipse.agent.EclipsePatcher");
		assertThat(clazz).isNotNull();
		assertThat(clazz.getName()).isEqualTo("lombok.eclipse.agent.EclipsePatcher");
		// Synthesised class extends Object and declares no methods beyond <init>.
		assertThat(clazz.getSuperclass()).isEqualTo(Object.class);
		assertThat(clazz.getDeclaredMethods()).isEmpty();
	}

	@Test
	void unknownLombokClass_canBeInstantiated() throws Exception {
		Class<?> clazz = loader.loadClass("lombok.eclipse.agent.EclipsePatcher");
		// The synthesised class has a default public <init>; instantiation must succeed.
		assertThatNoException().isThrownBy(() -> clazz.getDeclaredConstructor().newInstance());
	}
}
