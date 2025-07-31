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
package com.diffplug.spotless.extra.cpp;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EquoBasedStepBuilder;

import dev.equo.solstice.p2.P2Model;

/**
 * Formatter step which calls out to the Eclipse CDT formatter.
 * <p>
 * Eclipse-CDT <code>org.eclipse.core.contenttype.contentTypes</code>
 * extension <code>cSource</code>, <code>cHeader</code>, <code>cxxSource</code> and <code>cxxHeader</code>.
 * can handle: "c", "h", "C", "cpp", "cxx", "cc", "c++", "h", "hpp", "hh", "hxx", "inc"
 */
public final class EclipseCdtFormatterStep {
	// prevent direct instantiation
	private EclipseCdtFormatterStep() {}

	private static final String NAME = "eclipse cdt formatter";
	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support(NAME).add(17, "11.6");

	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	/** Provides default configuration */
	public static EquoBasedStepBuilder createBuilder(Provisioner provisioner) {
		return new EquoBasedStepBuilder(NAME, provisioner, defaultVersion(), EclipseCdtFormatterStep::apply, ImmutableMap.builder()) {
			@Override
			protected P2Model model(String version) {
				var model = new P2Model();
				addPlatformRepo(model, "4.26");
				model.addP2Repo("https://download.eclipse.org/tools/cdt/releases/" + version + "/");
				model.getInstall().add("org.eclipse.cdt.core");
				return model;
			}
		};
	}

	private static FormatterFunc apply(EquoBasedStepBuilder.State state) throws Exception {
		JVM_SUPPORT.assertFormatterSupported(state.getSemanticVersion());
		Class<?> formatterClazz = state.getJarState().getClassLoader().loadClass("com.diffplug.spotless.extra.glue.cdt.EclipseCdtFormatterStepImpl");
		var formatter = formatterClazz.getConstructor(Properties.class).newInstance(state.getPreferences());
		var method = formatterClazz.getMethod("format", String.class);
		return JVM_SUPPORT.suggestLaterVersionOnError(state.getSemanticVersion(),
				input -> {
					try {
						return (String) method.invoke(formatter, input);
					} catch (InvocationTargetException exceptionWrapper) {
						Throwable throwable = exceptionWrapper.getTargetException();
						Exception exception = throwable instanceof Exception ? (Exception) throwable : null;
						throw exception == null ? exceptionWrapper : exception;
					}
				});
	}
}
