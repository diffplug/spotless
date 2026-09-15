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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;

/**
 * Stub implementation of {@code lombok.launch.PatchFixesHider} used only
 * within the {@code FeatureClassLoader} isolation boundary.
 *
 * <p>When lombok is active as a JVM agent it patches ECJ classes so that their
 * methods call into inner classes of {@code PatchFixesHider}.  The real class
 * lives in lombok's shadow class-loader and is never reachable from
 * {@code FeatureClassLoader}.  This stub satisfies those call sites so that
 * ECJ's patched initialisers can complete without a {@link NoClassDefFoundError}
 * or {@link NoSuchMethodError}.
 *
 * <p>Method signatures must exactly match the transplanted descriptors that
 * lombok injects into ECJ bytecode; hence several methods use concrete ECJ
 * types rather than {@code Object}.
 */
@SuppressWarnings("unused")
final class PatchFixesHider {

	private PatchFixesHider() {}

	/** Stub for {@code PatchFixesHider.ModuleClassLoading}. */
	public static final class ModuleClassLoading {
		private ModuleClassLoading() {}

		/** Stub – no-op. */
		public static void parserClinit() {}
	}

	/** Stub for {@code PatchFixesHider.Transform}. */
	public static final class Transform {
		private Transform() {}

		/** Stub – no-op. */
		public static void transform(Object parser, Object ast) throws IOException {}

		/** Stub – no-op. */
		public static void transform_swapped(Object ast, Object parser) throws IOException {}
	}

	/** Stub for {@code PatchFixesHider.PatchFixes}. */
	public static final class PatchFixes {
		private PatchFixes() {}

		/**
		 * Stub – always returns {@code false}.
		 * Matches transplanted descriptor: {@code (Lorg/eclipse/jdt/core/dom/ASTNode;)Z}
		 */
		public static boolean isGenerated(org.eclipse.jdt.core.dom.ASTNode node) {
			return false;
		}

		/**
		 * Stub – always returns {@code false}.
		 * Matches transplanted descriptor: {@code (Lorg/eclipse/jdt/internal/compiler/ast/ASTNode;)Z}
		 */
		public static boolean isGenerated(ASTNode node) {
			return false;
		}

		/**
		 * Stub – always returns {@code false}.
		 * Matches transplanted descriptor: {@code (Lorg/eclipse/jdt/core/IMember;)Z}
		 */
		public static boolean isGenerated(org.eclipse.jdt.core.IMember member) {
			return false;
		}

		/**
		 * Stub – always returns {@code false}.
		 * Matches transplanted descriptor for isBlockedVisitorAndGenerated.
		 */
		public static boolean isBlockedVisitorAndGenerated(org.eclipse.jdt.core.dom.ASTNode node, ASTVisitor visitor) {
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

		/** Stub – returns {@code sourceEnd} unchanged. */
		public static int getSourceEndFixed(int sourceEnd, ASTNode node) throws Exception {
			return sourceEnd;
		}

		/** Stub – returns {@code original} unchanged. */
		public static int fixRetrieveStartingCatchPosition(int original, int start) {
			return original == -1 ? start : original;
		}

		/** Stub – returns {@code original} unchanged. */
		public static int fixRetrieveIdentifierEndPosition(int original, int start, int end) {
			if (original == -1)
				return end;
			if (original < start)
				return end;
			return original;
		}

		/** Stub – returns {@code original} unchanged. */
		public static int fixRetrieveEllipsisStartPosition(int original, int end) {
			return original == -1 ? end : original;
		}

		/** Stub – returns {@code original} unchanged. */
		public static int fixRetrieveStartBlockPosition(int original, int start) {
			return original == -1 ? start : original;
		}

		/** Stub – returns {@code original} unchanged. */
		public static int fixRetrieveRightBraceOrSemiColonPosition(int original, int end) {
			return original == -1 ? end : original;
		}

		/** Stub – returns 0-length array. */
		public static Object[] listRewriteHandleGeneratedMethods(Object rewriteEvent) {
			return new Object[0];
		}

		/** Stub – returns {@code original} unchanged. */
		public static String getRealNodeSource(String original, ASTNode node) {
			return original;
		}

		/** Stub – returns {@code original} unchanged. */
		public static String getRealNodeSource(String original, org.eclipse.jdt.core.dom.ASTNode node) throws Exception {
			return original;
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

	/** Stub for {@code PatchFixesHider.Javadoc}. */
	public static final class Javadoc {
		private Javadoc() {}

		/** Stub – returns {@code original} unchanged. */
		public static String getHTMLContentFromSource(String original, Object member) {
			return original;
		}
	}
}
