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
package com.diffplug.gradle.spotless;

/**
 * Every {@link FormatExtension} has a method
 * {@link FormatExtension#licenseHeader(String, String) license(licenseContent, licenseDelimiter)},
 * where licenseDelimiter is a regex that separates the license part of the code from the content.
 * For some kinds of format -
 * such as {@link JavaExtension java}, {@link KotlinExtension kotlin}, and {@link GroovyExtension groovy} -
 * we already have a defined delimiter, so users don't have to provide it.
 * By having the java, kotlin, and groovy formats implement this interface,
 * you can write generic code for enforcing whitespace and licenses.
 */
public interface HasBuiltinDelimiterForLicense {

	/**
	 * @param licenseHeader
	 *            Content that should be at the top of every file.
	 */
	FormatExtension.LicenseHeaderConfig licenseHeader(String licenseHeader);

	/**
	 * @param licenseHeaderFile
	 *            Content that should be at the top of every file.
	 */
	FormatExtension.LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile);
}
