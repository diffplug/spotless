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
package com.diffplug.spotless.extra.java;

import java.lang.reflect.Method;
import java.util.Properties;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder.State;

/** Formatter step which calls out to the Eclipse JDT formatter. */
public final class EclipseJdtFormatterStep {
	// prevent direct instantiation
	private EclipseJdtFormatterStep() {}

	private static final String NAME = "eclipse jdt formatter";
	private static final String FORMATTER_CLASS_OLD = "com.diffplug.gradle.spotless.java.eclipse.EclipseFormatterStepImpl";
	private static final String FORMATTER_CLASS = "com.diffplug.spotless.extra.eclipse.java.EclipseJdtFormatterStepImpl";
	private static final String MAVEN_GROUP_ARTIFACT = "com.diffplug.spotless:spotless-eclipse-jdt";
	private static final String DEFAULT_VERSION = "4.7.3a";
	private static final String FORMATTER_METHOD = "format";

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	/** Provides default configuration */
	public static EclipseBasedStepBuilder createBuilder(Provisioner provisioner) {
		return new EclipseBasedStepBuilder(NAME, provisioner, EclipseJdtFormatterStep::apply);
	}

	private static FormatterFunc apply(State state) throws Exception {
		Class<?> formatterClazz = getClass(state);
		Object formatter = formatterClazz.getConstructor(Properties.class).newInstance(state.getPreferences());
		Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
		return input -> (String) method.invoke(formatter, input);
	}

	private static Class<?> getClass(State state) {
		if (state.getMavenCoordinate(MAVEN_GROUP_ARTIFACT).isPresent()) {
			return state.loadClass(FORMATTER_CLASS);
		}
		return state.loadClass(FORMATTER_CLASS_OLD);
	}
}
