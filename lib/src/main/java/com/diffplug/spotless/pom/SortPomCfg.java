/*
 * Copyright 2021-2025 DiffPlug
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
package com.diffplug.spotless.pom;

import java.io.Serializable;

// Class and members must be public, otherwise we get failed to access class com.diffplug.spotless.pom.SortPomInternalState from class com.diffplug.spotless.pom.SortPomFormatterFunc (com.diffplug.spotless.pom.SortPomInternalState is in unnamed module of loader org.codehaus.plexus.classworlds.realm.ClassRealm @682bd3c4; com.diffplug.spotless.pom.SortPomFormatterFunc is in unnamed module of loader com.diffplug.spotless.pom.DelegatingClassLoader @573284a5)
public class SortPomCfg implements Serializable {
	private static final long serialVersionUID = 1L;

	public String version = "4.0.0";

	public String encoding = "UTF-8";

	public String lineSeparator = System.getProperty("line.separator");

	public boolean expandEmptyElements;

	public boolean spaceBeforeCloseEmptyElement = false;

	public boolean keepBlankLines = true;

	public boolean endWithNewline = true;

	public int nrOfIndentSpace = 2;

	public boolean indentBlankLines = false;

	public boolean indentSchemaLocation = false;

	public String indentAttribute = null;

	public String predefinedSortOrder = "recommended_2008_06";

	public boolean quiet = false;

	public String sortOrderFile = null;

	public String sortDependencies = null;

	public String sortDependencyManagement = null;

	public String sortDependencyExclusions = null;

	public String sortPlugins = null;

	public boolean sortProperties = false;

	public boolean sortModules = false;

	public boolean sortExecutions = false;
}
