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
package lombok.launch;

/**
 * Stub implementation of {@code lombok.launch.PatchFixesHider} used only
 * within the {@code FeatureClassLoader} isolation boundary.
 *
 * <p>When lombok is loaded as a JVM agent (e.g. {@code -javaagent:lombok.jar}),
 * it patches ECJ's {@code Parser} class so that its static initializer
 * references inner classes of {@code PatchFixesHider} such as
 * {@code ModuleClassLoading} and {@code Transform}.  Spotless's
 * {@code FeatureClassLoader} isolates formatter JARs from the build-tool
 * class-loader, so it cannot see the real {@code PatchFixesHider} that was
 * injected by the agent.  Loading this stub instead allows ECJ's
 * {@code Parser.<clinit>} to complete without a {@link NoClassDefFoundError}.
 *
 * <p>Every method in every inner class is a no-op stub.  No real formatting
 * logic lives here.
 */
@SuppressWarnings("unused")
final class PatchFixesHider {

	private PatchFixesHider() {}

	/** Stub for {@code PatchFixesHider.ModuleClassLoading}. */
	public static final class ModuleClassLoading {
		private ModuleClassLoading() {}

		/** Stub – performs no class-loader manipulation. */
		public static void parserClinit() {
			// no-op stub
		}
	}

	/** Stub for {@code PatchFixesHider.Transform}. */
	public static final class Transform {
		private Transform() {}

		/** Stub – performs no AST transformation. */
		public static void transform(Object parser, Object ast) {
			// no-op stub
		}

		/** Stub – performs no AST transformation. */
		public static void transform_swapped(Object ast, Object parser) {
			// no-op stub
		}
	}

	/** Stub for {@code PatchFixesHider.PatchFixes}. */
	public static final class PatchFixes {
		private PatchFixes() {}

		/** Stub – always returns {@code false}. */
		public static boolean isGenerated(Object node) {
			return false;
		}

		/** Stub – always returns {@code false}. */
		public static boolean returnFalse(Object object) {
			return false;
		}

		/** Stub – always returns {@code true}. */
		public static boolean returnTrue(Object object) {
			return true;
		}

		/** Stub – always returns {@code false}. */
		public static boolean isBlockedVisitorAndGenerated(Object node, Object visitor) {
			return false;
		}

		/** Stub – returns 0-length array. */
		public static Object[] listRewriteHandleGeneratedMethods(Object rewriteEvent) {
			return new Object[0];
		}

		/** Stub – returns {@code sourceEnd} unchanged. */
		public static int getSourceEndFixed(int sourceEnd, Object node) {
			return sourceEnd;
		}

		/** Stub – returns {@code original} unchanged. */
		public static int fixRetrieveStartingCatchPosition(int original, int start) {
			return original == -1 ? start : original;
		}

		/** Stub – returns {@code original} unchanged. */
		public static int fixRetrieveRightBraceOrSemiColonPosition(int original, int end) {
			return original == -1 ? end : original;
		}
	}

	/** Stub for {@code PatchFixesHider.ValPortal}. */
	public static final class ValPortal {
		private ValPortal() {}

		/** Stub – no-op. */
		public static void copyInitializationOfForEachIterable(Object parser) {}

		/** Stub – no-op. */
		public static void copyInitializationOfLocalDeclaration(Object parser) {}

		/** Stub – no-op. */
		public static void addFinalAndValAnnotationToVariableDeclarationStatement(Object converter, Object out, Object in) {}

		/** Stub – no-op. */
		public static void addFinalAndValAnnotationToSingleVariableDeclaration(Object converter, Object out, Object in) {}
	}

	/** Stub for {@code PatchFixesHider.Val}. */
	public static final class Val {
		private Val() {}

		/** Stub – always returns {@code false}. */
		public static boolean handleValForLocalDeclaration(Object local, Object scope) {
			return false;
		}

		/** Stub – always returns {@code false}. */
		public static boolean handleValForForEach(Object forEach, Object scope) {
			return false;
		}
	}

	/** Stub for {@code PatchFixesHider.ExtensionMethod}. */
	public static final class ExtensionMethod {
		private ExtensionMethod() {}

		/** Stub – returns {@code resolvedType} unchanged. */
		public static Object resolveType(Object resolvedType, Object methodCall, Object scope) {
			return resolvedType;
		}

		/** Stub – no-op. */
		public static void errorNoMethodFor(Object problemReporter, Object messageSend, Object recType, Object params) {}

		/** Stub – no-op. */
		public static void invalidMethod(Object problemReporter, Object messageSend, Object method) {}

		/** Stub – no-op. */
		public static void invalidMethod(Object problemReporter, Object messageSend, Object method, Object scope) {}

		/** Stub – no-op. */
		public static void nonStaticAccessToStaticMethod(Object problemReporter, Object location, Object method, Object messageSend) {}

		/** Stub – returns {@code original} unchanged. */
		public static Object modifyMethodPattern(Object original) {
			return original;
		}
	}

	/** Stub for {@code PatchFixesHider.Delegate}. */
	public static final class Delegate {
		private Delegate() {}

		/** Stub – always returns {@code false}. */
		public static boolean handleDelegateForType(Object classScope) {
			return false;
		}

		/** Stub – returns an empty array. */
		public static Object[] addGeneratedDelegateMethods(Object returnValue, Object javaElement) {
			return new Object[0];
		}

		/** Stub – always returns {@code false}. */
		public static boolean isDelegateSourceMethod(Object sourceMethod) {
			return false;
		}

		/** Stub – always returns {@code null}. */
		public static Object returnElementInfo(Object delegateSourceMethod) {
			return null;
		}
	}

	/** Stub for {@code PatchFixesHider.Util}. */
	public static final class Util {
		private Util() {}
	}

	/** Stub for {@code PatchFixesHider.LombokDeps}. */
	public static final class LombokDeps {
		private LombokDeps() {}
	}

	/** Stub for {@code PatchFixesHider.Javadoc}. */
	public static final class Javadoc {
		private Javadoc() {}

		/** Stub – returns {@code original} unchanged. */
		public static String getHTMLContentFromSource(String original, Object member) {
			return original;
		}
	}
}
