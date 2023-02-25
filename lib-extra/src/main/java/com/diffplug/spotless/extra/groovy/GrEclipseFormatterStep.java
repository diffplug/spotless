/*
 * Copyright 2016-2021 DiffPlug
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
package com.diffplug.spotless.extra.groovy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder.State;

/** Formatter step which calls out to the Groovy-Eclipse formatter. */
public final class GrEclipseFormatterStep {
	// prevent direct instantiation
	private GrEclipseFormatterStep() {}

	private static final String NAME = "eclipse groovy formatter";
	private static final String FORMATTER_CLASS = "com.diffplug.spotless.extra.eclipse.groovy.GrEclipseFormatterStepImpl";
	private static final String FORMATTER_CLASS_OLD = "com.diffplug.gradle.spotless.groovy.eclipse.GrEclipseFormatterStepImpl";
	private static final String MAVEN_GROUP_ARTIFACT = "com.diffplug.spotless:spotless-eclipse-groovy";
	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support(NAME).add(8, "4.19.0").add(11, "4.21.0");
	private static final String FORMATTER_METHOD = "format";

	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	/** Provides default configuration */
	public static EclipseBasedStepBuilder createBuilder(Provisioner provisioner) {
		return new EclipseBasedStepBuilder(NAME, provisioner, GrEclipseFormatterStep::apply);
	}

	private static FormatterFunc apply(EclipseBasedStepBuilder.State state) throws Exception {
		JVM_SUPPORT.assertFormatterSupported(state.getSemanticVersion());
		Class<?> formatterClazz = getClass(state);
		Object formatter = formatterClazz.getConstructor(Properties.class).newInstance(state.getPreferences());
		Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
		return JVM_SUPPORT.suggestLaterVersionOnError(state.getSemanticVersion(),
				input -> {
					try {
						return (String) method.invoke(formatter, input);
					} catch (InvocationTargetException exceptionWrapper) {
						Throwable throwable = exceptionWrapper.getTargetException();
						Exception exception = (throwable instanceof Exception) ? (Exception) throwable : null;
						throw (null == exception) ? exceptionWrapper : exception;
					}
				});
	}

	private static Class<?> getClass(State state) {
		if (state.getMavenCoordinate(MAVEN_GROUP_ARTIFACT).isPresent()) {
			return state.loadClass(FORMATTER_CLASS);
		}
		return state.loadClass(FORMATTER_CLASS_OLD);
	}

}
