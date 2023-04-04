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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.eclipse.EquoResourceHarness;

public class EclipseWtpFormatterStepTest {
	private final static Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support("Oldest Version").add(8, "4.8.0");

	private static class NestedTests extends EquoResourceHarness {
		public NestedTests(String unformatted, String formatted, EclipseWtpFormatterStep kind) {
			super(kind.createBuilder(TestProvisioner.mavenCentral()), unformatted, formatted);
		}

		@ParameterizedTest
		@MethodSource
		void formatWithVersion(String version) throws Exception {
			assertFormatted(version);
		}

		private static Stream<String> formatWithVersion() {
			return Stream.of(JVM_SUPPORT.getRecommendedFormatterVersion(), EclipseWtpFormatterStep.defaultVersion());
		}

		/**
		 * Check that configuration change is supported by all WTP formatters.
		 * Some of the formatters only support static workspace configuration.
		 * Hence separated class loaders are required for different configurations.
		 */
		@Test
		void multipleConfigurations() throws Exception {
			File tabPropertyFile = createPropertyFile(config -> {
				config.setProperty("indentationChar", "tab");
				config.setProperty("indentationSize", "1");
			});
			File spacePropertyFile = createPropertyFile(config -> {
				config.setProperty("indentationChar", "space");
				config.setProperty("indentationSize", "5");
			});
			String defaultFormatted = assertFormatted(EclipseWtpFormatterStep.defaultVersion(), tabPropertyFile);
			assertThat(format(EclipseWtpFormatterStep.defaultVersion(), spacePropertyFile)).as("Space formatting output unexpected").isEqualTo(defaultFormatted.replace("\t", "     "));
		}

		private File createPropertyFile(Consumer<Properties> config) throws IOException {
			Properties configProps = new Properties();
			config.accept(configProps);
			File tempFile = File.createTempFile("EclipseWtpFormatterStepTest-", ".properties");
			OutputStream tempOut = new FileOutputStream(tempFile);
			configProps.store(tempOut, "test properties");
			tempOut.flush();
			return tempFile;
		}
	}

	@Nested
	class CSS extends NestedTests {
		public CSS() {
			super("body {\na: v;   b:   \nv;\n}  \n", "body {\n\ta: v;\n\tb: v;\n}", EclipseWtpFormatterStep.CSS);
		}
	}

	@Nested
	class HTML extends NestedTests {
		public HTML() {
			super("<!DOCTYPE html> <html>\t<head> <meta   charset=\"UTF-8\"></head>\n</html>  ", "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n</head>\n</html>\n", EclipseWtpFormatterStep.HTML);
		}
	}

	@Nested
	class JS extends NestedTests {
		public JS() {
			super("function f(  )   {\na.b(1,\n2);}", "function f() {\n    a.b(1, 2);\n}", EclipseWtpFormatterStep.JS);
		}
	}

	@Nested
	class JSON extends NestedTests {
		public JSON() {
			super("{\"a\": \"b\",	\"c\":   { \"d\": \"e\",\"f\": \"g\"}}", "{\n\t\"a\": \"b\",\n\t\"c\": {\n\t\t\"d\": \"e\",\n\t\t\"f\": \"g\"\n\t}\n}", EclipseWtpFormatterStep.JSON);
		}
	}

	@Nested
	class XML extends NestedTests {
		public XML() {
			super("<a><b>   c</b></a>", "<a>\n\t<b> c</b>\n</a>", EclipseWtpFormatterStep.XML);
		}
	}
}
