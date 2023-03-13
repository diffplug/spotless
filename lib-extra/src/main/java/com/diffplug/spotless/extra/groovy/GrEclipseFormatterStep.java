/*
 * Copyright 2016-2023 DiffPlug
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
import java.util.List;
import java.util.Properties;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EquoBasedStepBuilder;

import dev.equo.solstice.p2.P2Model;

/** Formatter step which calls out to the Groovy-Eclipse formatter. */
public final class GrEclipseFormatterStep {
	// prevent direct instantiation
	private GrEclipseFormatterStep() {}

	private static final String NAME = "eclipse groovy formatter";
	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support(NAME).add(11, "4.26");
	private static final String FORMATTER_METHOD = "format";

	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	public static EquoBasedStepBuilder createBuilder(Provisioner provisioner) {
		return new EquoBasedStepBuilder(NAME, provisioner, GrEclipseFormatterStep::apply) {
			@Override
			protected P2Model model(String version) {
				if (!version.startsWith("4.")) {
					throw new IllegalArgumentException("Expected version 4.x");
				}
				int eVersion = Integer.parseInt(version.substring("4.".length()));
				if (eVersion < 8) {
					throw new IllegalArgumentException("4.8 is the oldest version we support, this was " + version);
				}
				String greclipseVersion;
				if (eVersion >= 18) {
					greclipseVersion = "4." + (eVersion - 18) + ".0";
				} else {
					greclipseVersion = "3." + (eVersion - 8) + ".0";
				}
				var model = new P2Model();
				addPlatformRepo(model, version);
				model.addP2Repo("https://groovy.jfrog.io/artifactory/plugins-release/org/codehaus/groovy/groovy-eclipse-integration/" + greclipseVersion + "/e" + version + "/");
				model.getInstall().addAll(List.of(
						"org.codehaus.groovy.eclipse.refactoring",
						"org.codehaus.groovy.eclipse.core",
						"org.eclipse.jdt.groovy.core",
						"org.codehaus.groovy"));
				model.addFilterAndValidate("no-debug", filter -> {
					filter.exclude("org.eclipse.jdt.debug");
				});
				return model;
			}

			@Override
			public void setVersion(String version) {
				if (version.endsWith(".0")) {
					String newVersion = version.substring(0, version.length() - 2);
					System.err.println("Recommend replacing '" + version + "' with '" + newVersion + "' for eclipse JDT");
					version = newVersion;
				}
				super.setVersion(version);
			}
		};
	}

	private static FormatterFunc apply(EquoBasedStepBuilder.State state) throws Exception {
		JVM_SUPPORT.assertFormatterSupported(state.getSemanticVersion());
		Class<?> formatterClazz = state.getJarState().getClassLoader().loadClass("com.diffplug.spotless.extra.glue.groovy.GrEclipseFormatterStepImpl");
		var formatter = formatterClazz.getConstructor(Properties.class).newInstance(state.getPreferences());
		var method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
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
}
