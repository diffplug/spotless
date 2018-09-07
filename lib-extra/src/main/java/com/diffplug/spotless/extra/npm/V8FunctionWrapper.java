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
package com.diffplug.spotless.extra.npm;

import java.lang.reflect.Method;

public class V8FunctionWrapper extends ReflectiveObjectWrapper {

	public static final String WRAPPED_CLASS = "com.eclipsesource.v8.V8Function";
	public static final String CALLBACK_WRAPPED_CLASS = "com.eclipsesource.v8.JavaCallback";

	public V8FunctionWrapper(Reflective reflective, Object v8Function) {
		super(reflective, v8Function);
	}

	public static Object proxiedCallback(WrappedJavaCallback callback, Reflective reflective) {
		Object proxy = reflective.createDynamicProxy((proxyInstance, method, args) -> {
			if (isCallbackFunction(reflective, method, args)) {
				V8ObjectWrapper receiver = new V8ObjectWrapper(reflective, args[0]);
				V8ArrayWrapper parameters = new V8ArrayWrapper(reflective, args[1]);
				return callback.invoke(receiver, parameters);
			}
			return null;
		}, CALLBACK_WRAPPED_CLASS);
		return reflective.clazz(CALLBACK_WRAPPED_CLASS).cast(proxy);
	}

	private static boolean isCallbackFunction(Reflective reflective, Method method, Object[] args) {
		if (!"invoke".equals(method.getName())) {
			return false;
		}
		final Class<?>[] types = reflective.types(args);
		if (types.length != 2) {
			return false;
		}

		return V8ObjectWrapper.WRAPPED_CLASS.equals(types[0].getName()) &&
				V8ArrayWrapper.WRAPPED_CLASS.equals(types[1].getName());
	}

	@FunctionalInterface
	public interface WrappedJavaCallback {
		Object invoke(V8ObjectWrapper receiver, V8ArrayWrapper parameters);
	}
}
