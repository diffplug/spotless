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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Utilities for dumping class info, helpful for
 * debugging reflection code.  Probably easiest to
 * just copy-paste these methods where you need
 * them.
 */
public class ReflectionUtil {
	public static void dumpAllInfo(String name, Object obj) {
		System.out.println(name + " of type " + obj.getClass());
		for (Method method : obj.getClass().getMethods()) {
			dumpMethod(method);
		}
	}

	public static void dumpMethod(Method method) {
		System.out.print(Modifier.toString(method.getModifiers()));
		System.out.print(" " + method.getReturnType());
		System.out.print(" " + method.getName() + "(");
		Iterator<Parameter> paramIter = Arrays.asList(method.getParameters()).iterator();
		while (paramIter.hasNext()) {
			Parameter param = paramIter.next();

			System.out.print(param.getType().getName());
			if (paramIter.hasNext()) {
				System.out.print(", ");
			}
		}
		System.out.println(")");
	}
}
