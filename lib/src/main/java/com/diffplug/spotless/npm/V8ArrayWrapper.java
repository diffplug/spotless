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

public class V8ArrayWrapper extends ReflectiveObjectWrapper {

	public static final String WRAPPED_CLASS = "com.eclipsesource.v8.V8Array";

	public V8ArrayWrapper(Reflective reflective, Object v8Array) {
		super(reflective, v8Array);
	}

	public V8ArrayWrapper push(Object object) {
		if (object instanceof ReflectiveObjectWrapper) {
			ReflectiveObjectWrapper objectWrapper = (ReflectiveObjectWrapper) object;
			object = objectWrapper.wrappedObj();
		}
		if (reflective().clazz(NodeJSWrapper.V8_VALUE_CLASS).isAssignableFrom(object.getClass())) {
			invoke("push", reflective().typed(NodeJSWrapper.V8_VALUE_CLASS, object));
		} else {
			invoke("push", object);
		}
		return this;
	}

	public V8ArrayWrapper pushNull() {
		invoke("pushNull");
		return this;
	}

	public V8ObjectWrapper getObject(Integer index) {
		Object v8Object = invoke("getObject", index);
		if (v8Object == null) {
			return null;
		}
		return new V8ObjectWrapper(this.reflective(), v8Object);
	}
}
