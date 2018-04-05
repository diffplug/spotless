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
package com.diffplug.gradle.spotless.xml.eclipse;

/** Extension of org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames */
public interface EclipseXmlFormatterPreferenceNames {

	/**
	 * Optional XML catalog for XSD/DTD lookup.
	 * Catalog versions 1.0 and 1.1 are supported.
	 * <p>
	 * Value is of type <code>Path</code>.
	 * </p>
	 */
	public static final String USER_CATALOG = "userCatalog";
}
