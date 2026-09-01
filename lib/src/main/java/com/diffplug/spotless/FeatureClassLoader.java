/*
 * Copyright 2016-2026 DiffPlug
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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.Objects;

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
		} else if (name.startsWith("lombok.")) {
			// When lombok is active as a JVM agent it patches ECJ classes (Parser,
			// ASTConverter, DefaultCodeFormatter, etc.) to reference lombok.* classes
			// that live in lombok's shadow class-loader (SCL.lombok/) and are never
			// reachable from this isolated FeatureClassLoader.
			//
			// Strategy:
			//  1. Serve a hand-written stub if one is bundled in the lib jar
			//     (covers FieldAugment, EcjAugments, PatchFixesHider, Symbols, ...).
			//  2. Otherwise synthesise a minimal empty public class on the fly so that
			//     ECJ's patched static initialisers can complete without
			//     NoClassDefFoundError.  The synthesised class has no fields or methods
			//     and performs no work; it purely satisfies the JVM's class-loading.
			String path = name.replace('.', '/') + ".class";
			URL url = findResource(path);
			if (url != null) {
				try {
					return defineClass(name, urlToByteBuffer(url), (ProtectionDomain) null);
				} catch (IOException e) {
					throw new ClassNotFoundException(name, e);
				}
			}
			// No bundled stub – synthesise a minimal empty class.
			try {
				return defineClass(name, ByteBuffer.wrap(synthesiseEmptyClass(name)), (ProtectionDomain) null);
			} catch (IOException e) {
				throw new ClassNotFoundException(name, e);
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

	/**
	 * Synthesises a minimal valid class file for a public class with the given
	 * binary name, no superclass fields or methods beyond the default constructor
	 * inherited from {@link Object}.  The result satisfies the JVM class-loader
	 * contract so that references to the class can be resolved without a
	 * {@link NoClassDefFoundError}, even though the class does nothing.
	 *
	 * <p>The bytecode is hand-assembled following the Java Virtual Machine
	 * Specification §4 (class file format).  It requires no external libraries.
	 */
	private static byte[] synthesiseEmptyClass(String binaryName) throws IOException {
		// Internal name uses '/' as separator; e.g. "lombok/eclipse/agent/PatchDiagnostics"
		String internalName = binaryName.replace('.', '/');

		ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
		DataOutputStream out = new DataOutputStream(baos);

		// ---- Constant pool ----
		// We need:
		//  #1  Class_info  → #2   (this class)
		//  #2  Utf8        internalName
		//  #3  Class_info  → #4   (superclass: java/lang/Object)
		//  #4  Utf8        "java/lang/Object"
		//  #5  Utf8        "Code"       (attribute name used by <init>)
		//  #6  Utf8        "<init>"
		//  #7  Utf8        "()V"
		//  #8  MethodRef   → #3, #9
		//  #9  NameAndType → #6, #7
		int constantPoolCount = 10; // indices 1..9

		// magic
		out.writeInt(0xCAFEBABE);
		// minor version, major version (52 = Java 8, safe floor for --release 17 output)
		out.writeShort(0);
		out.writeShort(52);
		// constant pool count (number of entries + 1)
		out.writeShort(constantPoolCount);

		// #1 CONSTANT_Class → #2
		out.writeByte(7);
		out.writeShort(2);
		// #2 CONSTANT_Utf8  internalName
		out.writeByte(1);
		writeUtf8(out, internalName);
		// #3 CONSTANT_Class → #4
		out.writeByte(7);
		out.writeShort(4);
		// #4 CONSTANT_Utf8  "java/lang/Object"
		out.writeByte(1);
		writeUtf8(out, "java/lang/Object");
		// #5 CONSTANT_Utf8  "Code"
		out.writeByte(1);
		writeUtf8(out, "Code");
		// #6 CONSTANT_Utf8  "<init>"
		out.writeByte(1);
		writeUtf8(out, "<init>");
		// #7 CONSTANT_Utf8  "()V"
		out.writeByte(1);
		writeUtf8(out, "()V");
		// #8 CONSTANT_Methodref → class=#3, nameAndType=#9
		out.writeByte(10);
		out.writeShort(3);
		out.writeShort(9);
		// #9 CONSTANT_NameAndType → name=#6, descriptor=#7
		out.writeByte(12);
		out.writeShort(6);
		out.writeShort(7);

		// ---- Class declaration ----
		// access flags: ACC_PUBLIC | ACC_SUPER
		out.writeShort(0x0021);
		// this_class = #1
		out.writeShort(1);
		// super_class = #3
		out.writeShort(3);
		// interfaces count
		out.writeShort(0);
		// fields count
		out.writeShort(0);
		// methods count: one method (<init>)
		out.writeShort(1);

		// ---- <init>()V method ----
		// access: ACC_PUBLIC
		out.writeShort(0x0001);
		// name_index = #6 "<init>"
		out.writeShort(6);
		// descriptor_index = #7 "()V"
		out.writeShort(7);
		// attributes count: 1 (Code)
		out.writeShort(1);

		// Code attribute
		// attribute_name_index = #5 "Code"
		out.writeShort(5);
		// attribute_length: max_stack(2) + max_locals(2) + code_length(4) + code(5) + exception_table_length(2) + attributes_count(2) = 17
		out.writeInt(17);
		// max_stack
		out.writeShort(1);
		// max_locals
		out.writeShort(1);
		// code_length
		out.writeInt(5);
		// aload_0, invokespecial #8, return
		out.writeByte(0x2A); // aload_0
		out.writeByte(0xB7); // invokespecial
		out.writeShort(8);   // → #8 Object.<init>
		out.writeByte(0xB1); // return
		// exception_table_length
		out.writeShort(0);
		// attributes_count (for Code attribute)
		out.writeShort(0);

		// ---- Class attributes ----
		out.writeShort(0);

		out.flush();
		return baos.toByteArray();
	}

	private static void writeUtf8(DataOutputStream out, String s) throws IOException {
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		out.writeShort(bytes.length);
		out.write(bytes);
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
