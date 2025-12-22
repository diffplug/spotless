/*
 * Copyright 2016-2025 DiffPlug
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Objects;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * This class loader is used to load classes of Spotless features from a search
 * path of URLs.<br/>
 * Features shall be independent from build tools. Hence the class loader of the
 * underlying build tool is e.g. skipped during the search for classes.<br/>
 *
 * For `com.diffplug.spotless.glue.`, classes are redefined from within the lib jar
 * but linked against the `Url[]`. This allows us to ship classfiles which function as glue
 * code but delay linking/definition to runtime after the user has specified which version
 * of the formatter they want.
 * <p>
 *  For `"org.slf4j.` and (`com.diffplug.spotless.` but not `com.diffplug.spotless.extra.`)
 * 	the classes are loaded from the buildToolClassLoader.
 */
class FeatureClassLoader extends URLClassLoader {
	static {
		ClassLoader.registerAsParallelCapable();
	}

	private final ClassLoader buildToolClassLoader;

	/**
	 * Constructs a new FeatureClassLoader for the given URLs, based on an {@code URLClassLoader},
	 * using the system class loader as parent.
	 *
	 * @param urls the URLs from which to load classes and resources
	 * @param buildToolClassLoader The build tool class loader
	 * @exception  SecurityException  If a security manager exists and prevents the creation of a class loader.
	 * @exception  NullPointerException if {@code urls} is {@code null}.
	 */
	FeatureClassLoader(URL[] urls, ClassLoader buildToolClassLoader) {
		super(urls, getParentClassLoader());
		Objects.requireNonNull(buildToolClassLoader);
		this.buildToolClassLoader = buildToolClassLoader;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		if (name.startsWith("com.diffplug.spotless.glue.") || name.startsWith("com.diffplug.spotless.extra.glue.")) {
			String path = name.replace('.', '/') + ".class";
			URL url = findResource(path);
			if (url == null) {
				throw new ClassNotFoundException(name);
			}
			try {
				return defineClass(name, urlToByteBuffer(url), (ProtectionDomain) null);
			} catch (IOException e) {
				throw new ClassNotFoundException(name, e);
			}
		} else if (name.equals("lombok.core.FieldAugment")) {
			return new ByteBuddy()
					.subclass(Object.class)
					.name(name)
					.defineMethod("augment", Object.class, Visibility.PUBLIC, Ownership.STATIC)
					.withParameters(Class.class, Class.class, String.class)
					.intercept(StubMethod.INSTANCE)
					.defineMethod("get", Object.class, Visibility.PUBLIC)
					.withParameters(Object.class).intercept(StubMethod.INSTANCE)
					.defineMethod("set", void.class, Visibility.PUBLIC)
					.withParameters(Object.class, Object.class).intercept(StubMethod.INSTANCE)
					.defineMethod("getAndSet", Object.class, Visibility.PUBLIC)
					.withParameters(Object.class, Object.class).intercept(StubMethod.INSTANCE)
					.defineMethod("clear", Object.class, Visibility.PUBLIC)
					.withParameters(Object.class).intercept(StubMethod.INSTANCE)
					.defineMethod("compareAndClear", Object.class, Visibility.PUBLIC)
					.withParameters(Object.class, Object.class).intercept(StubMethod.INSTANCE)
					.defineMethod("setIfAbsent", Object.class, Visibility.PUBLIC)
					.withParameters(Object.class, Object.class).intercept(StubMethod.INSTANCE)
					.defineMethod("compareAndSet", Object.class, Visibility.PUBLIC)
					.withParameters(Object.class, Object.class, Object.class).intercept(StubMethod.INSTANCE)
					.make()
					.load(this, ClassLoadingStrategy.Default.INJECTION)
					.getLoaded();
		} else if (name.equals("lombok.eclipse.EcjAugments")) {
			Class<?> fieldAugmentClass = loadClass("lombok.core.FieldAugment");
			Class<?> ecjAugmentsClass = new ByteBuddy()
					.subclass(Object.class)
					.name(name)
					.defineField("ASTNode_generatedBy", fieldAugmentClass, Visibility.PUBLIC, Ownership.STATIC)
					.defineField("ASTNode_tokens", fieldAugmentClass, Visibility.PUBLIC, Ownership.STATIC)
					.make()
					.load(this, ClassLoadingStrategy.Default.INJECTION)
					.getLoaded();

			try {
				Object object = fieldAugmentClass.getDeclaredConstructor().newInstance();
				ecjAugmentsClass.getField("ASTNode_generatedBy").set(null, object);
				ecjAugmentsClass.getField("ASTNode_tokens").set(null, object);
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
			return ecjAugmentsClass;
		} else if (name.startsWith("lombok.")) {
			try {
				return super.findClass(name);
			} catch (ClassNotFoundException e) {
				Class<?> astNodeClass = loadClass("org.eclipse.jdt.internal.compiler.ast.ASTNode");
				Class<?> astNodeDomClass = loadClass("org.eclipse.jdt.core.dom.ASTNode");
				Class<?> astVisitorClass = loadClass("org.eclipse.jdt.core.dom.ASTVisitor");
				Class<?> rewriteEventClass = loadClass("org.eclipse.jdt.internal.core.dom.rewrite.RewriteEvent");
				// RewriteEvent[]
				TypeDescription rewriteEventArrayType = TypeDescription.Generic.Builder
						.rawType(rewriteEventClass)
						.asArray()
						.build()
						.asErasure();
				Method nullArray;
				try {
					nullArray = Array.class.getMethod("newInstance", Class.class, int.class);
				} catch (NoSuchMethodException e1) {
					throw new IllegalArgumentException(e1);
				}
				Class<? extends Object> c = new ByteBuddy()
						.subclass(Object.class)
						.name(name)
						.defineMethod("parserClinit", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("setLine", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class, int.class, Object.class)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("setRange", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class, int.class, int.class)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("transform_swapped", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class, Object.class)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("transform", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class, Object.class)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("copyInitializationOfLocalDeclaration", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class).intercept(StubMethod.INSTANCE)
						.defineMethod("copyInitializationOfForEachIterable", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class).intercept(StubMethod.INSTANCE)
						.defineMethod("addFinalAndValAnnotationToVariableDeclaration", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class).intercept(StubMethod.INSTANCE)
						.defineMethod("isStatic", boolean.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class).intercept(StubMethod.INSTANCE)
						.defineMethod("onMethodEnter", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class, Object.class).intercept(StubMethod.INSTANCE)
						.defineMethod("onMethodExit", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class, Object.class).intercept(StubMethod.INSTANCE)
						.defineMethod("setSourceRangeCheck", boolean.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class, int.class, int.class)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("isGenerated", boolean.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(astNodeClass)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("isGenerated", boolean.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(astNodeDomClass)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("addFinalAndValAnnotationToVariableDeclarationStatement", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class, Object.class, Object.class)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("isBlockedVisitorAndGenerated", boolean.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(astNodeDomClass, astVisitorClass)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("pop", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("push", void.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(String.class)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("hasSymbol", boolean.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(String.class)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("returnFalse", boolean.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("returnTrue", boolean.class, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(Object.class)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("isEmpty", boolean.class, Visibility.PUBLIC, Ownership.STATIC)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("size", int.class, Visibility.PUBLIC, Ownership.STATIC)
						.intercept(StubMethod.INSTANCE)
						.defineMethod("listRewriteHandleGeneratedMethods", rewriteEventArrayType, Visibility.PUBLIC, Ownership.STATIC)
						.withParameters(rewriteEventClass)
						.intercept(MethodCall.invoke(nullArray)
								.with(rewriteEventClass)
								.with(0)
								.withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
						.make()
						.load(this, ClassLoadingStrategy.Default.INJECTION)
						.getLoaded();
				return c;
			}
		} else if (useBuildToolClassLoader(name)) {
			return buildToolClassLoader.loadClass(name);
		} else {
			return super.findClass(name);
		}
	}

	private static boolean useBuildToolClassLoader(String name) {
		if (name.startsWith("org.slf4j.")) {
			return true;
		} else if (!name.startsWith("com.diffplug.spotless.extra") && name.startsWith("com.diffplug.spotless.")) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public URL findResource(String name) {
		URL resource = super.findResource(name);
		if (resource != null) {
			return resource;
		}
		return buildToolClassLoader.getResource(name);
	}

	private static ByteBuffer urlToByteBuffer(URL url) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (InputStream inputStream = url.openStream()) {
			inputStream.transferTo(buffer);
		}
		buffer.flush();
		return ByteBuffer.wrap(buffer.toByteArray());
	}

	private static ClassLoader getParentClassLoader() {
		return ThrowingEx.get(() -> (ClassLoader) ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null));
	}
}
