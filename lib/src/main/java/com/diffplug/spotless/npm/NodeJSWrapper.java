/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless.npm;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.diffplug.spotless.ThrowingEx;

class NodeJSWrapper extends ReflectiveObjectWrapper {

	public static final String V8_RUNTIME_CLASS = "com.eclipsesource.v8.V8";
	public static final String V8_VALUE_CLASS = "com.eclipsesource.v8.V8Value";

	public static final String WRAPPED_CLASS = "com.eclipsesource.v8.NodeJS";

	private static final Set<ClassLoader> alreadySetup = new HashSet<>();

	public NodeJSWrapper(ClassLoader classLoader) {
		super(Reflective.withClassLoader(classLoader),
				reflective -> {
					if (alreadySetup.add(classLoader)) {
						// the bridge to node.js needs a .dll/.so/.dylib which gets loaded through System.load
						// the problem is that when the JVM loads that DLL, it is bound to the specific classloader that called System.load
						// no other classloaders have access to it, and if any other classloader tries to load it, you get an error
						//
						// ...but, you can copy that DLL as many times as you want, and each classloader can load its own copy of the DLL, and
						// that is fine
						//
						// ...but the in order to do that, we have to manually load the DLL per classloader ourselves, which involves some
						// especially hacky reflection into J2V8 *and* JVM internal
						//
						// so this is bad code, but it fixes our problem, and so far we don't have a better way...

						// here we get the name of the DLL within the jar, and we get our own copy of it on disk
						String resource = (String) reflective.invokeStaticMethodPrivate("com.eclipsesource.v8.LibraryLoader", "computeLibraryFullName");
						File file = NodeJsGlobal.sharedLibs.nextDynamicLib(classLoader, resource);

						// ideally, we would call System.load, but the JVM does some tricky stuff to
						// figure out who actually called this, and it realizes it was Reflective, which lives
						// outside the J2V8 classloader, so System.load doesn't work.  Soooo, we have to dig
						// into JVM internals and manually tell it "this class from J2V8 called you"
						Class<?> libraryLoaderClass = ThrowingEx.get(() -> classLoader.loadClass("com.eclipsesource.v8.LibraryLoader"));
						reflective.invokeStaticMethodPrivate("java.lang.ClassLoader", "loadLibrary0", libraryLoaderClass, file);

						// and now we set the flag in J2V8 which says "the DLL is loaded, don't load it again"
						reflective.staticFieldPrivate("com.eclipsesource.v8.V8", "nativeLibraryLoaded", true);
					}
					return reflective.invokeStaticMethod(WRAPPED_CLASS, "createNodeJS");
				});
	}

	public V8ObjectWrapper require(File npmModulePath) {
		Objects.requireNonNull(npmModulePath);
		Object v8Object = invoke("require", npmModulePath);
		return new V8ObjectWrapper(reflective(), v8Object);
	}

	public V8ObjectWrapper createNewObject() {
		Object v8Object = reflective().invokeConstructor(V8ObjectWrapper.WRAPPED_CLASS, nodeJsRuntime());
		V8ObjectWrapper objectWrapper = new V8ObjectWrapper(reflective(), v8Object);
		return objectWrapper;
	}

	public V8ObjectWrapper createNewObject(Map<String, Object> values) {
		Objects.requireNonNull(values);
		V8ObjectWrapper obj = createNewObject();
		values.forEach(obj::add);
		return obj;
	}

	public V8ArrayWrapper createNewArray(Object... elements) {
		final V8ArrayWrapper v8ArrayWrapper = this.createNewArray();
		for (Object element : elements) {
			v8ArrayWrapper.push(element);
		}
		return v8ArrayWrapper;
	}

	public V8ArrayWrapper createNewArray() {
		Object v8Array = reflective().invokeConstructor(V8ArrayWrapper.WRAPPED_CLASS, nodeJsRuntime());
		V8ArrayWrapper arrayWrapper = new V8ArrayWrapper(reflective(), v8Array);
		return arrayWrapper;
	}

	public V8FunctionWrapper createNewFunction(V8FunctionWrapper.WrappedJavaCallback callback) {
		Object v8Function = reflective().invokeConstructor(V8FunctionWrapper.WRAPPED_CLASS,
				reflective().typed(
						V8_RUNTIME_CLASS,
						nodeJsRuntime()),
				reflective().typed(
						V8FunctionWrapper.CALLBACK_WRAPPED_CLASS,
						V8FunctionWrapper.proxiedCallback(callback, reflective())));
		V8FunctionWrapper functionWrapper = new V8FunctionWrapper(reflective(), v8Function);
		return functionWrapper;
	}

	public void handleMessage() {
		invoke("handleMessage");
	}

	private Object nodeJsRuntime() {
		return invoke("getRuntime");
	}

	public Object v8NullValue(Object value) {
		if (value == null) {
			return reflective().staticField(V8_VALUE_CLASS, "NULL");
		}
		return value;
	}

	public boolean isV8NullValue(Object v8Object) {
		return reflective().staticField(V8_VALUE_CLASS, "NULL") == v8Object;
	}
}
