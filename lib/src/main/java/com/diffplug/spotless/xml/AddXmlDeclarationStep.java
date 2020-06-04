package com.diffplug.spotless.xml;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;

public class AddXmlDeclarationStep {

	public static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	public static FormatterStep create() {
		return FormatterStep.create("addXmlDeclaration",
			AddXmlDeclarationStep.class,
			unused -> AddXmlDeclarationStep::format);
	}

	private static String format(String rawString) {
		String replacement = rawString;
		if (!(rawString.startsWith(XML_DECLARATION))) {
			replacement = XML_DECLARATION + LineEnding.PLATFORM_NATIVE.str() + replacement;
		}
		return replacement;
	}
}

