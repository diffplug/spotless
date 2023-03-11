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
package com.diffplug.spotless.extra.java;

import java.io.File;
import java.util.Properties;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EquoBasedStepBuilder;

import dev.equo.solstice.p2.P2Model;

/** Formatter step which calls out to the Eclipse JDT formatter. */
public final class EclipseJdtFormatterStep {
	// prevent direct instantiation
	private EclipseJdtFormatterStep() {}

	private static final String NAME = "eclipse jdt formatter";
	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support(NAME).add(11, "4.26");

	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	public static EquoBasedStepBuilder createBuilder(Provisioner provisioner) {
		return new EquoBasedStepBuilder(NAME, provisioner, EclipseJdtFormatterStep::apply) {
			@Override
			protected P2Model model(String version) {
				var model = new P2Model();
				model.addP2Repo("https://download.eclipse.org/eclipse/updates/" + version + "/");
				model.getInstall().add("org.eclipse.jdt.core");
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
		Class<?> formatterClazz = state.getJarState().getClassLoader().loadClass("com.diffplug.spotless.extra.glue.jdt.EclipseJdtFormatterStepImpl");
		var formatter = formatterClazz.getConstructor(Properties.class).newInstance(state.getPreferences());
		var method = formatterClazz.getMethod("format", String.class, File.class);
		FormatterFunc formatterFunc = (FormatterFunc.NeedsFile) (input, file) -> (String) method.invoke(formatter, input, file);
		return JVM_SUPPORT.suggestLaterVersionOnError(state.getSemanticVersion(), formatterFunc);
	}
}
