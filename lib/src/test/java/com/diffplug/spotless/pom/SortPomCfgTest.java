package com.diffplug.spotless.pom;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SortPomCfgTest {

	@Test
	void testDefaultValues() {
		SortPomCfg cfg = new SortPomCfg();

		// Test default values using AssertJ
		assertThat(cfg.version).isEqualTo("4.0.0");
		assertThat(cfg.encoding).isEqualTo("UTF-8");
		assertThat(cfg.lineSeparator).isEqualTo(System.getProperty("line.separator"));
		assertThat(cfg.expandEmptyElements).isFalse();
		assertThat(cfg.spaceBeforeCloseEmptyElement).isFalse();
		assertThat(cfg.keepBlankLines).isTrue();
		assertThat(cfg.endWithNewline).isTrue();
		assertThat(cfg.nrOfIndentSpace).isEqualTo(2);
		assertThat(cfg.indentBlankLines).isFalse();
		assertThat(cfg.indentSchemaLocation).isFalse();
		assertThat(cfg.indentAttribute).isNull();
		assertThat(cfg.predefinedSortOrder).isEqualTo("recommended_2008_06");
		assertThat(cfg.quiet).isFalse();
		assertThat(cfg.sortOrderFile).isNull();
		assertThat(cfg.sortDependencies).isNull();
		assertThat(cfg.sortDependencyManagement).isNull();
		assertThat(cfg.sortDependencyExclusions).isNull();
		assertThat(cfg.sortPlugins).isNull();
		assertThat(cfg.sortProperties).isFalse();
		assertThat(cfg.sortModules).isFalse();
		assertThat(cfg.sortExecutions).isFalse();
	}

	@Test
	void testFieldSetters() {
		SortPomCfg cfg = new SortPomCfg();

		// Set all fields
		cfg.version = "4.1.0";
		cfg.encoding = "ISO-8859-1";
		cfg.lineSeparator = "\n";
		cfg.expandEmptyElements = true;
		cfg.spaceBeforeCloseEmptyElement = true;
		cfg.keepBlankLines = false;
		cfg.endWithNewline = false;
		cfg.nrOfIndentSpace = 4;
		cfg.indentBlankLines = true;
		cfg.indentSchemaLocation = true;
		cfg.indentAttribute = "attribute";
		cfg.predefinedSortOrder = "custom";
		cfg.quiet = true;
		cfg.sortOrderFile = "sortOrder.xml";
		cfg.sortDependencies = "groupId,artifactId";
		cfg.sortDependencyManagement = "scope,groupId";
		cfg.sortDependencyExclusions = "artifactId";
		cfg.sortPlugins = "groupId";
		cfg.sortProperties = true;
		cfg.sortModules = true;
		cfg.sortExecutions = true;

		// Verify all set values with AssertJ
		assertThat(cfg.version).isEqualTo("4.1.0");
		assertThat(cfg.encoding).isEqualTo("ISO-8859-1");
		assertThat(cfg.lineSeparator).isEqualTo("\n");
		assertThat(cfg.expandEmptyElements).isTrue();
		assertThat(cfg.spaceBeforeCloseEmptyElement).isTrue();
		assertThat(cfg.keepBlankLines).isFalse();
		assertThat(cfg.endWithNewline).isFalse();
		assertThat(cfg.nrOfIndentSpace).isEqualTo(4);
		assertThat(cfg.indentBlankLines).isTrue();
		assertThat(cfg.indentSchemaLocation).isTrue();
		assertThat(cfg.indentAttribute).isEqualTo("attribute");
		assertThat(cfg.predefinedSortOrder).isEqualTo("custom");
		assertThat(cfg.quiet).isTrue();
		assertThat(cfg.sortOrderFile).isEqualTo("sortOrder.xml");
		assertThat(cfg.sortDependencies).isEqualTo("groupId,artifactId");
		assertThat(cfg.sortDependencyManagement).isEqualTo("scope,groupId");
		assertThat(cfg.sortDependencyExclusions).isEqualTo("artifactId");
		assertThat(cfg.sortPlugins).isEqualTo("groupId");
		assertThat(cfg.sortProperties).isTrue();
		assertThat(cfg.sortModules).isTrue();
		assertThat(cfg.sortExecutions).isTrue();
	}

	@Test
	void testNullHandling() {
		SortPomCfg cfg = new SortPomCfg();

		// Set nullable fields to null
		cfg.version = null;
		cfg.encoding = null;
		cfg.lineSeparator = null;
		cfg.indentAttribute = null;
		cfg.predefinedSortOrder = null;
		cfg.sortOrderFile = null;
		cfg.sortDependencies = null;
		cfg.sortDependencyManagement = null;
		cfg.sortDependencyExclusions = null;
		cfg.sortPlugins = null;

		// Verify null values with AssertJ
		assertThat(cfg.version).isNull();
		assertThat(cfg.encoding).isNull();
		assertThat(cfg.lineSeparator).isNull();
		assertThat(cfg.indentAttribute).isNull();
		assertThat(cfg.predefinedSortOrder).isNull();
		assertThat(cfg.sortOrderFile).isNull();
		assertThat(cfg.sortDependencies).isNull();
		assertThat(cfg.sortDependencyManagement).isNull();
		assertThat(cfg.sortDependencyExclusions).isNull();
		assertThat(cfg.sortPlugins).isNull();
	}

	@Test
	void testBooleanFieldsEdgeCases() {
		SortPomCfg cfg = new SortPomCfg();

		// Toggle all boolean fields
		cfg.expandEmptyElements = !cfg.expandEmptyElements;
		cfg.spaceBeforeCloseEmptyElement = !cfg.spaceBeforeCloseEmptyElement;
		cfg.keepBlankLines = !cfg.keepBlankLines;
		cfg.endWithNewline = !cfg.endWithNewline;
		cfg.indentBlankLines = !cfg.indentBlankLines;
		cfg.indentSchemaLocation = !cfg.indentSchemaLocation;
		cfg.quiet = !cfg.quiet;
		cfg.sortProperties = !cfg.sortProperties;
		cfg.sortModules = !cfg.sortModules;
		cfg.sortExecutions = !cfg.sortExecutions;

		// Verify all boolean fields are toggled
		assertThat(cfg.expandEmptyElements).isTrue();
		assertThat(cfg.spaceBeforeCloseEmptyElement).isTrue();
		assertThat(cfg.keepBlankLines).isFalse();
		assertThat(cfg.endWithNewline).isFalse();
		assertThat(cfg.indentBlankLines).isTrue();
		assertThat(cfg.indentSchemaLocation).isTrue();
		assertThat(cfg.quiet).isTrue();
		assertThat(cfg.sortProperties).isTrue();
		assertThat(cfg.sortModules).isTrue();
		assertThat(cfg.sortExecutions).isTrue();
	}

	@Test
	void testNumericFieldEdgeCases() {
		SortPomCfg cfg = new SortPomCfg();

		// Test minimum value
		cfg.nrOfIndentSpace = 0;
		assertThat(cfg.nrOfIndentSpace).isZero();

		// Test negative value
		cfg.nrOfIndentSpace = -1;
		assertThat(cfg.nrOfIndentSpace).isNegative();

		// Test large value
		cfg.nrOfIndentSpace = Integer.MAX_VALUE;
		assertThat(cfg.nrOfIndentSpace).isEqualTo(Integer.MAX_VALUE);
	}
}
