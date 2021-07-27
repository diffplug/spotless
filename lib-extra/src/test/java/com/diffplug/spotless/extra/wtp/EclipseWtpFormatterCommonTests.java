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
package com.diffplug.spotless.extra.wtp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.JRE.JAVA_11;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.eclipse.EclipseCommonTests;

@EnabledForJreRange(min = JAVA_11)
abstract class EclipseWtpFormatterCommonTests extends EclipseCommonTests {

	abstract EclipseBasedStepBuilder createBuilder();

	abstract String getTestInput();

	abstract String getTestExpectation();

	@Override
	protected String[] getSupportedVersions() {
		String[] oldVersions = {"4.7.3a", "4.7.3b", "4.8.0", "4.12.0", "4.13.0", "4.14.0", "4.15.0",
				"4.16.0", "4.17.0", "4.18.0"};
		String[] newVersions = {"4.19.0"};
		return Stream.concat(Stream.of(oldVersions), Stream.of(newVersions)).toArray(String[]::new);
	}

	@Override
	protected String getTestInput(String version) {
		return getTestInput();
	}

	@Override
	protected String getTestExpectation(String version) {
		return getTestExpectation();
	}

	@Override
	protected FormatterStep createStep(String version) {
		EclipseBasedStepBuilder builder = createBuilder();
		builder.setVersion(version);
		return builder.build();
	}

	/**
	 * Check that configuration change is supported by all WTP formatters.
	 * Some of the formatters only support static workspace configuration.
	 * Hence separated class loaders are required for different configurations.
	 */
	@Test
	void multipleConfigurations() throws Exception {
		FormatterStep tabFormatter = createStepForDefaultVersion(config -> {
			config.setProperty("indentationChar", "tab");
			config.setProperty("indentationSize", "1");
		});
		FormatterStep spaceFormatter = createStepForDefaultVersion(config -> {
			config.setProperty("indentationChar", "space");
			config.setProperty("indentationSize", "5");
		});

		assertThat(formatWith(tabFormatter)).as("Tab formatting output unexpected").isEqualTo(getTestExpectation()); //This is the default configuration
		assertThat(formatWith(spaceFormatter)).as("Space formatting output unexpected").isEqualTo(getTestExpectation().replace("\t", "     "));
	}

	private String formatWith(FormatterStep formatter) throws Exception {
		File baseLocation = File.createTempFile("EclipseWtpFormatterStepTest-", ".xml"); //Only required for relative path lookup
		return formatter.format(getTestInput(), baseLocation);
	}

	private FormatterStep createStepForDefaultVersion(Consumer<Properties> config) throws IOException {
		Properties configProps = new Properties();
		config.accept(configProps);
		File tempFile = File.createTempFile("EclipseWtpFormatterStepTest-", ".properties");
		OutputStream tempOut = new FileOutputStream(tempFile);
		configProps.store(tempOut, "test properties");
		EclipseBasedStepBuilder builder = createBuilder();
		builder.setVersion(EclipseWtpFormatterStep.defaultVersion());
		builder.setPreferences(Arrays.asList(tempFile));
		return builder.build();
	}
}
