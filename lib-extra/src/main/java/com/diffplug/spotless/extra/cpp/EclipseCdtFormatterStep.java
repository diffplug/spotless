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
package com.diffplug.spotless.extra.cpp;

import java.lang.reflect.Method;
import java.util.Properties;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder.State;

/**
 * Formatter step which calls out to the Eclipse CDT formatter.
 *
 * Eclipse-CDT <code>org.eclipse.core.contenttype.contentTypes</code>
 * extension <code>cSource</code>, <code>cHeader</code>, <code>cxxSource</code> and <code>cxxHeader</code>.
 * can handle: "c", "h", "C", "cpp", "cxx", "cc", "c++", "h", "hpp", "hh", "hxx", "inc"
 */
public final class EclipseCdtFormatterStep {
	// prevent direct instantiation
	private EclipseCdtFormatterStep() {}

	private static final String NAME = "eclipse cdt formatter";
	private static final String FORMATTER_CLASS = "com.diffplug.spotless.extra.eclipse.cdt.EclipseCdtFormatterStepImpl";
	private static final String FORMATTER_METHOD = "format";
	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support(NAME).add(8, "4.16.0").add(11, "4.21.0");

	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	/** Provides default configuration */
	public static EclipseBasedStepBuilder createBuilder(Provisioner provisioner) {
		return new EclipseBasedStepBuilder(NAME, provisioner, EclipseCdtFormatterStep::apply);
	}

	private static FormatterFunc apply(State state) throws Exception {
		JVM_SUPPORT.assertFormatterSupported(state.getSemanticVersion());
		Class<?> formatterClazz = state.loadClass(FORMATTER_CLASS);
		Object formatter = formatterClazz.getConstructor(Properties.class).newInstance(state.getPreferences());
		Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
		return JVM_SUPPORT.suggestLaterVersionOnError(state.getSemanticVersion(), input -> (String) method.invoke(formatter, input));
	}

}
