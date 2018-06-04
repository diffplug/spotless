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
package com.diffplug.spotless.extra.groovy;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.config.EclipseConfiguration;

/** Formatter step which calls out to the Groovy-Eclipse formatter. */
public class GrEclipseFormatterStep {
	private static final String NAME = "groovy eclipse formatter";
	private static final String FORMATTER_CLASS = "com.diffplug.gradle.spotless.groovy.eclipse.GrEclipseFormatterStepImpl";
	private static final String FORMATTER_METHOD = "format";
	static final String VERSIONS[] = {"2.3.0", "4.6.3"};

	/**
	 * Creates a formatter step using the default version for the given settings file.
	 */
	@Deprecated
	public static FormatterStep create(Iterable<File> settingsFiles, Provisioner provisioner) {
		return create(defaultVersion(), settingsFiles, provisioner);
	}

	/** Creates a formatter step for the given version and settings file. */
	@Deprecated
	public static FormatterStep create(String version, Iterable<File> settingsFiles, Provisioner provisioner) {
		EclipseConfiguration config = createConfig(provisioner);
		config.setVersion(version);
		config.setPreferences(settingsFiles);
		return config.build();
	}

	@Deprecated
	public static String defaultVersion() {
		return "2.3.0";
	}

	/** Constructs a formatter step adapted for a certain Groovy Eclipse formatter version */
	private GrEclipseFormatterStep() {
		//Currently all supported versions behaves equally. No adaptation required.
	}

	/** Provides default configuration */
	public static EclipseConfiguration createConfig(Provisioner provisioner) {
		return new EclipseConfiguration(NAME, provisioner, GrEclipseFormatterStep::apply, VERSIONS);
	}

	private static FormatterFunc apply(EclipseConfiguration.State state) throws Exception {
		Class<?> formatterClazz = state.loadClass(FORMATTER_CLASS);
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

}
