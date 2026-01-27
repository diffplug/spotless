/*
 * Copyright 2016-2026 DiffPlug
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
import java.util.Map;
import java.util.Properties;

import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.SerializedFunction;
import com.diffplug.spotless.extra.EquoBasedStepBuilder;
import com.diffplug.spotless.extra.P2Provisioner;

import dev.equo.solstice.p2.P2Model;

/** Formatter step which calls out to the Eclipse JDT formatter. */
public final class EclipseJdtFormatterStep {
	// prevent direct instantiation
	private EclipseJdtFormatterStep() {}

	private static final String NAME = "eclipse jdt formatter";
	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support(NAME).add(17, "4.35");

	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	public static EclipseJdtFormatterStep.Builder createBuilder(Provisioner provisioner, P2Provisioner p2Provisioner) {
		return new EclipseJdtFormatterStep.Builder(NAME, provisioner, p2Provisioner, defaultVersion(), EclipseJdtFormatterStep::apply, ImmutableMap.builder());
	}

	private static FormatterFunc apply(EquoBasedStepBuilder.State state) throws Exception {
		JVM_SUPPORT.assertFormatterSupported(state.getSemanticVersion());
		Class<?> formatterClazz = state.getJarState().getClassLoader().loadClass("com.diffplug.spotless.extra.glue.jdt.EclipseJdtFormatterStepImpl");
		var formatter = formatterClazz.getConstructor(Properties.class, Map.class).newInstance(state.getPreferences(), state.getStepProperties());
		var method = formatterClazz.getMethod("format", String.class, File.class);
		FormatterFunc formatterFunc = (FormatterFunc.NeedsFile) (input, file) -> (String) method.invoke(formatter, input, file);
		return JVM_SUPPORT.suggestLaterVersionOnError(state.getSemanticVersion(), formatterFunc);
	}

	public static class Builder extends EquoBasedStepBuilder {
		private final ImmutableMap.Builder<String, String> stepProperties;

		Builder(
				String formatterName,
				Provisioner mavenProvisioner,
				P2Provisioner p2Provisioner,
				String defaultVersion,
				SerializedFunction<State, FormatterFunc> stateToFormatter,
				ImmutableMap.Builder<String, String> stepProperties) {
			super(formatterName, mavenProvisioner, p2Provisioner, defaultVersion, stateToFormatter, stepProperties);
			this.stepProperties = stepProperties;
		}

		@Override
		protected P2Model model(String version) {
			var model = new P2Model();
			addPlatformRepo(model, version);
			model.getInstall().add("org.eclipse.jdt.core");
			return model;
		}

		@Override
		public void setVersion(String version) {
			if (version.endsWith(".0")) {
				String newVersion = version.substring(0, version.length() - 2);
				System.err.println("Recommend replacing '" + version + "' with '" + newVersion + "' for Eclipse JDT");
				version = newVersion;
			}
			super.setVersion(version);
		}

		public void sortMembersDoNotSortFields(boolean doNotSortFields) {
			boolean sortAllMembers = !doNotSortFields;
			stepProperties.put("sp_cleanup.sort_members_all", String.valueOf(sortAllMembers));
		}

		public void sortMembersEnabled(boolean enabled) {
			stepProperties.put("sp_cleanup.sort_members", String.valueOf(enabled));
		}

		public void sortMembersOrder(String order) {
			stepProperties.put("outlinesortoption", order);
		}

		public void sortMembersVisibilityOrder(String order) {
			stepProperties.put("org.eclipse.jdt.ui.visibility.order", order);
		}

		public void sortMembersVisibilityOrderEnabled(boolean enabled) {
			stepProperties.put("org.eclipse.jdt.ui.enable.visibility.order", String.valueOf(enabled));
		}
	}
}
