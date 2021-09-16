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
package com.diffplug.spotless.extra.eclipse.cdt;

import static com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework.LINE_DELIMITER;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.formatter.DefaultCodeFormatterConstants;
import org.junit.jupiter.api.Test;

/** Eclipse CDT wrapper integration tests */
class EclipseCdtFormatterStepImplTest {

	private final static String CPP_UNFORMATTED = "#include <iostream>\n" +
			"using namespace std;\n" +
			"int main()\n{\n" +
			"    cout <<\n\"Hello, World!\";\n" +
			"    return 0;\n" +
			"}".replaceAll("\n", LINE_DELIMITER);
	private final static String CPP_FORMATTED = "#include <iostream>\n" +
			"using namespace std;\n" +
			"int main() {\n" +
			"\tcout << \"Hello, World!\";\n" +
			"\treturn 0;\n" +
			"}\n".replaceAll("\n", LINE_DELIMITER);

	private final static String DOXYGEN_HTML = "/**\n *<pre>void f() {int a =1;} </pre>\n */\n".replaceAll("\n", LINE_DELIMITER);

	private final static String ILLEGAL_CHAR = Character.toString((char) 254);

	private final static String FUNCT_PTR_UNFORMATTED = "void  (*getFunc(void))  (int);";
	private final static String FUNCT_PTR_FORMATTED = "void (* getFunc(void)) (int);";

	@Test
	void defaultFormat() throws Throwable {
		String output = format(CPP_UNFORMATTED, config -> {});
		assertEquals(CPP_FORMATTED,
				output, "Unexpected formatting with default preferences.");
	}

	@Test
	void invalidFormat() throws Throwable {
		String output = format(CPP_FORMATTED.replace("int main() {", "int main()  "), config -> {});
		assertTrue(output.contains("int main()" + LINE_DELIMITER), "Incomplete CPP not formatted on best effort basis.");
	}

	@Test
	void invalidCharater() throws Throwable {
		String output = format(CPP_FORMATTED.replace("int main() {", "int main()" + ILLEGAL_CHAR + " {"), config -> {});
		assertTrue(output.contains("int main()" + LINE_DELIMITER), "Invalid charater not formatted on best effort basis.");
	}

	@Test
	void invalidConfiguration() throws Throwable {
		String output = format(CPP_FORMATTED, config -> {
			config.setProperty(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, CCorePlugin.SPACE);
			config.setProperty(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "noInteger");
		});
		assertEquals(CPP_FORMATTED.replace("\t", "    "),
				output, "Invalid indentation configuration not replaced by default value (4 spaces)");
	}

	@Test
	void htmlCommentFormat() throws Throwable {
		String output = format(DOXYGEN_HTML + CPP_FORMATTED, config -> {});
		assertEquals(DOXYGEN_HTML + CPP_FORMATTED,
				output, "HTML comments not ignored by formatter.");
	}

	@Test
	void regionWarning() throws Throwable {
		String output = format(FUNCT_PTR_UNFORMATTED, config -> {});
		assertEquals(FUNCT_PTR_FORMATTED, output, "Code not formatted at all due to regional error.");
	}

	private static String format(final String input, final Consumer<Properties> config) throws Exception {
		Properties properties = new Properties();
		config.accept(properties);
		EclipseCdtFormatterStepImpl formatter = new EclipseCdtFormatterStepImpl(properties);
		return formatter.format(input);
	}
}
