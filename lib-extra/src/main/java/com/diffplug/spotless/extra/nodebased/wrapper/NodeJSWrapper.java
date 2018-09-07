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
package com.diffplug.spotless.extra.nodebased.wrapper;

import java.io.File;
import java.util.Objects;

public class NodeJSWrapper extends ReflectiveObjectWrapper {

	public static final String V8_RUNTIME_CLASS = "com.eclipsesource.v8.V8";
	public static final String V8_VALUE_CLASS = "com.eclipsesource.v8.V8Value";

	public static final String WRAPPED_CLASS = "com.eclipsesource.v8.NodeJS";

	public NodeJSWrapper(ClassLoader classLoader) {
		super(Reflective.withClassLoader(classLoader),
				reflective -> reflective.invokeStaticMethod(WRAPPED_CLASS, "createNodeJS"));
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
