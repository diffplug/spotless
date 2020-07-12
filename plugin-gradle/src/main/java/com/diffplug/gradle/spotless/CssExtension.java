/*
 * Copyright 2016-2020 DiffPlug
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
package com.diffplug.gradle.spotless;

import static com.diffplug.gradle.spotless.PluginGradlePreconditions.requireElementsNonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import org.gradle.api.Project;

import com.diffplug.gradle.spotless.libdeprecated.CssDefaults;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep;

/**
 * The CSS extension is deprecated. Use the generic {@link FormatExtension} instead.
 */
@Deprecated
public class CssExtension extends FormatExtension implements HasBuiltinDelimiterForLicense {
	static final String NAME = "css";

	public CssExtension(SpotlessExtensionBase spotless) {
		super(spotless);
	}

	public EclipseConfig eclipse() {
		return new EclipseConfig(EclipseWtpFormatterStep.defaultVersion());
	}

	public EclipseConfig eclipse(String version) {
		return new EclipseConfig(version);
	}

	private static EclipseBasedStepBuilder createCssBuilder(Provisioner provisioner) {
		return new EclipseBasedStepBuilder(NAME, " - css", provisioner, state -> applyWithoutFile("EclipseCssFormatterStepImpl", state));
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

	private static final String FORMATTER_PACKAGE = "com.diffplug.spotless.extra.eclipse.wtp.";
	private static final String FORMATTER_METHOD = "format";

	/**
	 * The CSS Eclipse configuration is deprecated. Use the {@link FormatExtension.EclipseWtpConfig} instead.
	 */
	@Deprecated
	public class EclipseConfig {
		private final EclipseBasedStepBuilder builder;

		EclipseConfig(String version) {
			builder = createCssBuilder(provisioner());
			builder.setVersion(version);
			addStep(builder.build());
		}

		public void configFile(Object... configFiles) {
			requireElementsNonNull(configFiles);
			Project project = getProject();
			builder.setPreferences(project.files(configFiles).getFiles());
			replaceStep(builder.build());
		}

	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			target(CssDefaults.FILE_FILTER.toArray());
		}
		super.setupTask(task);
	}

	@Override
	public LicenseHeaderConfig licenseHeader(String licenseHeader) {
		return licenseHeader(licenseHeader, CssDefaults.DELIMITER_EXPR);
	}

	@Override
	public LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile) {
		return licenseHeaderFile(licenseHeaderFile, CssDefaults.DELIMITER_EXPR);
	}
}
