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
package com.diffplug.spotless.extra.wtp;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.extra.EquoBasedStepBuilder;

import dev.equo.solstice.p2.P2Model;

/** Formatter step which calls out to the Groovy-Eclipse formatter. */
public enum EclipseWtpFormatterStep {
	// @formatter:off
	CSS ("EclipseCssFormatterStepImpl",  EclipseWtpFormatterStep::applyWithoutFile),
	HTML("EclipseHtmlFormatterStepImpl", EclipseWtpFormatterStep::applyWithoutFile),
	JS  ("EclipseJsFormatterStepImpl",   EclipseWtpFormatterStep::applyWithoutFile),
	JSON("EclipseJsonFormatterStepImpl", EclipseWtpFormatterStep::applyWithoutFile),
	XML ("EclipseXmlFormatterStepImpl",  EclipseWtpFormatterStep::applyWithFile);
	// @formatter:on

	private static final String NAME = "eclipse wtp formatter";
	private static final String FORMATTER_PACKAGE = "com.diffplug.spotless.extra.glue.wtp.";
	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support(NAME).add(8, "4.18.0").add(11, "4.21.0");
	private static final String FORMATTER_METHOD = "format";

	private final String implementationClassName;
	private final ThrowingEx.BiFunction<String, EquoBasedStepBuilder.State, FormatterFunc> formatterCall;

	EclipseWtpFormatterStep(String implementationClassName, ThrowingEx.BiFunction<String, EquoBasedStepBuilder.State, FormatterFunc> formatterCall) {
		this.implementationClassName = implementationClassName;
		this.formatterCall = formatterCall;
	}

	public EquoBasedStepBuilder createBuilder(Provisioner provisioner) {
		return new EquoBasedStepBuilder(NAME + " - " + name(), provisioner, state -> formatterCall.apply(implementationClassName, state)) {
			@Override
			protected P2Model model(String version) {
				var model = new P2Model();
				addPlatformRepo(model, "4.26");
				model.addP2Repo("https://download.eclipse.org/webtools/repository/2022-12/");
				model.addP2Repo("https://download.eclipse.org/tools/orbit/downloads/drops/R20230302014618/repository/");
				// XML/HTML Formatter  - Dependencies
				model.getInstall().add("org.eclipse.wst.xml.core"); // DefaultXMLPartitionFormatter and XMLAssociationProvider
				model.getInstall().add("org.eclipse.wst.sse.core"); // Structure models
				model.getInstall().add("org.eclipse.wst.common.uriresolver"); // URI resolver for model queries
				model.getInstall().add("org.eclipse.wst.dtd.core"); // Support DTD extensions
				// XML Formatter - Dependencies
				model.getInstall().add("org.eclipse.wst.xsd.core"); // Support XSD extensions
				// JS Formatter - Dependencies
				model.getInstall().add("org.eclipse.wst.jsdt.core"); // DefaultCodeFormatter and related
				model.getInstall().add("org.eclipse.wst.jsdt.ui"); // Functionality to format comments
				// JSON Formatter - Dependencies
				model.getInstall().add("org.eclipse.wst.json.core"); // FormatProcessorJSON and related
				model.getInstall().add("org.eclipse.json"); // Provides JSON node interfaces
				// CSS Formatter - Dependencies
				model.getInstall().add("org.eclipse.wst.css.core"); // FormatProcessorCSS and related
				// HTML Formatter - Dependencies
				model.getInstall().add("org.eclipse.wst.html.core"); // HTMLFormatProcessorImpl and related
				return model;
			}
		};
	}

	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	private static FormatterFunc applyWithoutFile(String className, EquoBasedStepBuilder.State state) throws Exception {
		JVM_SUPPORT.assertFormatterSupported(state.getSemanticVersion());
		Class<?> formatterClazz = state.getJarState().getClassLoader().loadClass(FORMATTER_PACKAGE + className);
		Object formatter = formatterClazz.getConstructor(Properties.class).newInstance(state.getPreferences());
		Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
		return input -> {
			try {
				return (String) method.invoke(formatter, input);
			} catch (InvocationTargetException exceptionWrapper) {
				Throwable throwable = exceptionWrapper.getTargetException();
				Exception exception = (throwable instanceof Exception) ? (Exception) throwable : null;
				throw (null == exception) ? exceptionWrapper : exception;
			}
		};
	}

	private static FormatterFunc applyWithFile(String className, EquoBasedStepBuilder.State state) throws Exception {
		JVM_SUPPORT.assertFormatterSupported(state.getSemanticVersion());
		Class<?> formatterClazz = state.getJarState().getClassLoader().loadClass(FORMATTER_PACKAGE + className);
		Object formatter = formatterClazz.getConstructor(Properties.class).newInstance(state.getPreferences());
		Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class, String.class);
		return JVM_SUPPORT.suggestLaterVersionOnError(state.getSemanticVersion(), new FormatterFunc.NeedsFile() {
			@Override
			public String applyWithFile(String unix, File file) throws Exception {
				try {
					return (String) method.invoke(formatter, unix, file.getAbsolutePath());
				} catch (InvocationTargetException exceptionWrapper) {
					Throwable throwable = exceptionWrapper.getTargetException();
					Exception exception = (throwable instanceof Exception) ? (Exception) throwable : null;
					throw (null == exception) ? exceptionWrapper : exception;
				}
			}
		});
	}
}
