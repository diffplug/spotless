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
package com.diffplug.spotless.extra.wtp;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;

/** Formatter step which calls out to the Groovy-Eclipse formatter. */
public final class WtpEclipseFormatterStep {
	// prevent direct instantiation
	private WtpEclipseFormatterStep() {}

	private static final String NAME = "eclipse wtp formatters";
	private static final String FORMATTER_PACKAGE = "com.diffplug.spotless.extra.eclipse.wtp.";
	private static final String DEFAULT_VERSION = "4.7.3a";
	private static final String FORMATTER_METHOD = "format";

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	/** Provides default configuration for CSSformatter */
	public static EclipseBasedStepBuilder createCssBuilder(Provisioner provisioner) {
		return new EclipseBasedStepBuilder(NAME, " - css", provisioner, state -> applyWithoutFile("EclipseCssFormatterStepImpl", state));
	}

	/** Provides default configuration for HTML formatter */
	public static EclipseBasedStepBuilder createHtmlBuilder(Provisioner provisioner) {
		return new EclipseBasedStepBuilder(NAME, " - html", provisioner, state -> applyWithoutFile("EclipseHtmlFormatterStepImpl", state));
	}

	/** Provides default configuration for Java Script formatter */
	public static EclipseBasedStepBuilder createJsBuilder(Provisioner provisioner) {
		return new EclipseBasedStepBuilder(NAME, " - js", provisioner, state -> applyWithoutFile("EclipseJsFormatterStepImpl", state));
	}

	/** Provides default configuration for JSON formatter */
	public static EclipseBasedStepBuilder createJsonBuilder(Provisioner provisioner) {
		return new EclipseBasedStepBuilder(NAME, " - json", provisioner, state -> applyWithoutFile("EclipseJsonFormatterStepImpl", state));
	}

	/** Provides default configuration for XML formatter */
	public static EclipseBasedStepBuilder createXmlBuilder(Provisioner provisioner) {
		return new EclipseBasedStepBuilder(NAME, " - xml", provisioner, state -> applyWithFile("EclipseXmlFormatterStepImpl", state));
	}

	private static FormatterFunc applyWithoutFile(String className, EclipseBasedStepBuilder.State state) throws Exception {
		Class<?> formatterClazz = state.loadClass(FORMATTER_PACKAGE + className);
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

	private static FormatterFuncWithFile applyWithFile(String className, EclipseBasedStepBuilder.State state) throws Exception {
		Class<?> formatterClazz = state.loadClass(FORMATTER_PACKAGE + className);
		Object formatter = formatterClazz.getConstructor(Properties.class).newInstance(state.getPreferences());
		Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class, String.class);
		return (input, source) -> {
			try {
				return (String) method.invoke(formatter, input, source.getAbsolutePath());
			} catch (InvocationTargetException exceptionWrapper) {
				Throwable throwable = exceptionWrapper.getTargetException();
				Exception exception = (throwable instanceof Exception) ? (Exception) throwable : null;
				throw (null == exception) ? exceptionWrapper : exception;
			}
		};
	}

	private static interface FormatterFuncWithFile extends FormatterFunc {
		@Override
		default String apply(String input) throws Exception {
			throw new UnsupportedOperationException("Formatter requires file path of source.");
		}

		public String apply(String input, File source) throws Exception;
	}
}
