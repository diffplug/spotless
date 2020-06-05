/*
 * Copyright 2018-2019 DiffPlug
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Consumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.eclipse.EclipseCommonTests;

@RunWith(value = Parameterized.class)
public class EclipseWtpFormatterStepTest extends EclipseCommonTests {

	private enum WTP {
		// @formatter:off
		CSS(	"body {\na: v;   b:   \nv;\n}  \n",
				"body {\n\ta: v;\n\tb: v;\n}"),
		HTML(	"<!DOCTYPE html> <html>\t<head> <meta   charset=\"UTF-8\"></head>\n</html>  ",
				"<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n</head>\n</html>\n"),
		JS(		"function f(  )   {\na.b(1,\n2);}",
				"function f() {\n    a.b(1, 2);\n}"),
		JSON(	"{\"a\": \"b\",	\"c\":   { \"d\": \"e\",\"f\": \"g\"}}",
				"{\n\t\"a\": \"b\",\n\t\"c\": {\n\t\t\"d\": \"e\",\n\t\t\"f\": \"g\"\n\t}\n}"),
		XML(	"<a><b>   c</b></a>", "<a>\n\t<b> c</b>\n</a>");
		// @formatter:on

		public final String input;
		public final String expectation;

		private WTP(String input, final String expectation) {
			this.input = input;
			this.expectation = expectation;
		}

		public EclipseBasedStepBuilder createBuilder() {
			EclipseWtpFormatterStep stepType = EclipseWtpFormatterStep.valueOf(this.toString());
			return stepType.createBuilder(TestProvisioner.mavenCentral());
		}
	}

	@Parameters(name = "{0}")
	public static Iterable<WTP> data() {
		return Arrays.asList(WTP.values());
	}

	@Parameter(0)
	public WTP wtp;

	@Override
	protected String[] getSupportedVersions() {
		return new String[]{"4.7.3a", "4.7.3b", "4.8.0", "4.12.0", "4.13.0"};
	}

	@Override
	protected String getTestInput(String version) {
		return wtp.input;
	}

	@Override
	protected String getTestExpectation(String version) {
		return wtp.expectation;
	}

	@Override
	protected FormatterStep createStep(String version) {
		EclipseBasedStepBuilder builder = wtp.createBuilder();
		builder.setVersion(version);
		return builder.build();
	}

	/**
	 * Check that configuration change is supported by all WTP formatters.
	 * Some of the formatters only support static workspace configuration.
	 * Hence separated class loaders are required for different configurations.
	 */
	@Test
	public void multipleConfigurations() throws Exception {
		FormatterStep tabFormatter = createStepForDefaultVersion(config -> {
			config.setProperty("indentationChar", "tab");
			config.setProperty("indentationSize", "1");
		});
		FormatterStep spaceFormatter = createStepForDefaultVersion(config -> {
			config.setProperty("indentationChar", "space");
			config.setProperty("indentationSize", "5");
		});

		assertThat(formatWith(tabFormatter)).as("Tab formatting output unexpected").isEqualTo(wtp.expectation); //This is the default configuration
		assertThat(formatWith(spaceFormatter)).as("Space formatting output unexpected").isEqualTo(wtp.expectation.replace("\t", "     "));
	}

	private String formatWith(FormatterStep formatter) throws Exception {
		File baseLocation = File.createTempFile("EclipseWtpFormatterStepTest-", ".xml"); //Only required for relative path lookup
		return formatter.format(wtp.input, baseLocation);
	}

	private FormatterStep createStepForDefaultVersion(Consumer<Properties> config) throws IOException {
		Properties configProps = new Properties();
		config.accept(configProps);
		File tempFile = File.createTempFile("EclipseWtpFormatterStepTest-", ".properties");
		OutputStream tempOut = new FileOutputStream(tempFile);
		configProps.store(tempOut, "test properties");
		EclipseBasedStepBuilder builder = wtp.createBuilder();
		builder.setVersion(EclipseWtpFormatterStep.defaultVersion());
		builder.setPreferences(Arrays.asList(tempFile));
		return builder.build();
	}
}
