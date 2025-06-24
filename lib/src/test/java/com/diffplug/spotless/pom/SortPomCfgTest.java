package com.diffplug.spotless.pom;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SortPomCfgTest {

	@Test
	void testDefaultValues() {
		SortPomCfg cfg = new SortPomCfg();

		// Test default values
		assertEquals("4.0.0", cfg.version);
		assertEquals("UTF-8", cfg.encoding);
		assertEquals(System.getProperty("line.separator"), cfg.lineSeparator);
		assertFalse(cfg.expandEmptyElements);
		assertFalse(cfg.spaceBeforeCloseEmptyElement);
		assertTrue(cfg.keepBlankLines);
		assertTrue(cfg.endWithNewline);
		assertEquals(2, cfg.nrOfIndentSpace);
		assertFalse(cfg.indentBlankLines);
		assertFalse(cfg.indentSchemaLocation);
		assertNull(cfg.indentAttribute);
		assertEquals("recommended_2008_06", cfg.predefinedSortOrder);
		assertFalse(cfg.quiet);
		assertNull(cfg.sortOrderFile);
		assertNull(cfg.sortDependencies);
		assertNull(cfg.sortDependencyManagement);
		assertNull(cfg.sortDependencyExclusions);
		assertNull(cfg.sortPlugins);
		assertFalse(cfg.sortProperties);
		assertFalse(cfg.sortModules);
		assertFalse(cfg.sortExecutions);
	}

	@Test
	void testFieldSetters() {
		SortPomCfg cfg = new SortPomCfg();

		// Test setting all fields
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

		// Verify all set values
		assertEquals("4.1.0", cfg.version);
		assertEquals("ISO-8859-1", cfg.encoding);
		assertEquals("\n", cfg.lineSeparator);
		assertTrue(cfg.expandEmptyElements);
		assertTrue(cfg.spaceBeforeCloseEmptyElement);
		assertFalse(cfg.keepBlankLines);
		assertFalse(cfg.endWithNewline);
		assertEquals(4, cfg.nrOfIndentSpace);
		assertTrue(cfg.indentBlankLines);
		assertTrue(cfg.indentSchemaLocation);
		assertEquals("attribute", cfg.indentAttribute);
		assertEquals("custom", cfg.predefinedSortOrder);
		assertTrue(cfg.quiet);
		assertEquals("sortOrder.xml", cfg.sortOrderFile);
		assertEquals("groupId,artifactId", cfg.sortDependencies);
		assertEquals("scope,groupId", cfg.sortDependencyManagement);
		assertEquals("artifactId", cfg.sortDependencyExclusions);
		assertEquals("groupId", cfg.sortPlugins);
		assertTrue(cfg.sortProperties);
		assertTrue(cfg.sortModules);
		assertTrue(cfg.sortExecutions);
	}

	@Test
	void testNullHandling() {
		SortPomCfg cfg = new SortPomCfg();

		// Test setting nullable fields to null
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

		assertNull(cfg.version);
		assertNull(cfg.encoding);
		assertNull(cfg.lineSeparator);
		assertNull(cfg.indentAttribute);
		assertNull(cfg.predefinedSortOrder);
		assertNull(cfg.sortOrderFile);
		assertNull(cfg.sortDependencies);
		assertNull(cfg.sortDependencyManagement);
		assertNull(cfg.sortDependencyExclusions);
		assertNull(cfg.sortPlugins);
	}

	@Test
	void testBooleanFieldsEdgeCases() {
		SortPomCfg cfg = new SortPomCfg();

		// Verify all boolean fields can be toggled
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

		assertTrue(cfg.expandEmptyElements);
		assertTrue(cfg.spaceBeforeCloseEmptyElement);
		assertFalse(cfg.keepBlankLines);
		assertFalse(cfg.endWithNewline);
		assertTrue(cfg.indentBlankLines);
		assertTrue(cfg.indentSchemaLocation);
		assertTrue(cfg.quiet);
		assertTrue(cfg.sortProperties);
		assertTrue(cfg.sortModules);
		assertTrue(cfg.sortExecutions);
	}

	@Test
	void testNumericFieldEdgeCases() {
		SortPomCfg cfg = new SortPomCfg();

		// Test minimum value
		cfg.nrOfIndentSpace = 0;
		assertEquals(0, cfg.nrOfIndentSpace);

		// Test negative value (though probably not valid in practice)
		cfg.nrOfIndentSpace = -1;
		assertEquals(-1, cfg.nrOfIndentSpace);

		// Test large value
		cfg.nrOfIndentSpace = Integer.MAX_VALUE;
		assertEquals(Integer.MAX_VALUE, cfg.nrOfIndentSpace);
	}
}
