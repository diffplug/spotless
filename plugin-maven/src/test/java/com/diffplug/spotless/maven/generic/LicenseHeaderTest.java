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
package com.diffplug.spotless.maven.generic;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class LicenseHeaderTest extends MavenIntegrationHarness {
	private static final String KEY_LICENSE = "license/TestLicense";
	private static final String KOTLIN_LICENSE_HEADER = "// Hello, I'm Kotlin license header";

	@Test
	void fromFileJava() throws Exception {
		setFile("license.txt").toResource(KEY_LICENSE);
		writePomWithJavaSteps(
				"<licenseHeader>",
				"  <file>${basedir}/license.txt</file>",
				"</licenseHeader>");
		runTest();
	}

	@Test
	void fromContentCpp() throws Exception {
		String cppLicense = "//my license";
		writePomWithCppSteps(
				"<includes><include>src/**</include></includes>",
				"<licenseHeader>",
				"  <content>",
				cppLicense,
				"  </content>",
				"</licenseHeader>");

		String path = "src/test/cpp/file.c++";
		String cppContent = "#include <whatsoever.h>";
		setFile(path).toContent(cppContent);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).hasContent(cppLicense + '\n' + cppContent);
	}

	@Test
	void fromContentGroovy() throws Exception {
		writePomWithGroovySteps(
				"<licenseHeader>",
				"  <content>",
				"// If you can't trust a man's word",
				"// Does it help to have it in writing?",
				"  </content>",
				"</licenseHeader>");

		String path = "src/main/groovy/test.groovy";
		setFile(path).toResource("license/MissingLicense.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("license/HasLicense.test");
	}

	@Test
	void fromContentJava() throws Exception {
		writePomWithJavaSteps(
				"<licenseHeader>",
				"  <content>",
				"// If you can't trust a man's word",
				"// Does it help to have it in writing?",
				"  </content>",
				"</licenseHeader>");
		runTest();
	}

	@Test
	void fromFileGlobal() throws Exception {
		setFile("license.txt").toResource(KEY_LICENSE);
		writePom("<licenseHeader>",
				"  <file>${basedir}/license.txt</file>",
				"</licenseHeader>",
				"<java/>");

		runTest();
	}

	@Test
	void fromFileFormat() throws Exception {
		setFile("license.txt").toResource(KEY_LICENSE);
		writePomWithFormatSteps(
				"<licenseHeader>",
				"  <file>${basedir}/license.txt</file>",
				"  <delimiter>package</delimiter>",
				"</licenseHeader>");
		runTest();
	}

	@Test
	void fromContentFormat() throws Exception {
		writePomWithFormatSteps(
				"<licenseHeader>",
				"  <content>",
				"// If you can't trust a man's word",
				"// Does it help to have it in writing?",
				"  </content>",
				"  <delimiter>package</delimiter>",
				"</licenseHeader>");
		runTest();
	}

	@Test
	void fromContentKotlin() throws Exception {
		writePomWithKotlinSteps(
				"<licenseHeader>",
				"  <content>",
				KOTLIN_LICENSE_HEADER,
				"  </content>",
				"</licenseHeader>");

		String path = "src/main/kotlin/test.kt";
		String noLicenseHeader = getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test");

		setFile(path).toContent(noLicenseHeader);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).hasContent(KOTLIN_LICENSE_HEADER + '\n' + noLicenseHeader);
	}

	@Test
	void unsupportedPackageInfo() throws Exception {
		testUnsupportedFile("package-info.java");
	}

	@Test
	void unsupportedModuleInfo() throws Exception {
		testUnsupportedFile("module-info.java");
	}

	private void runTest() throws Exception {
		String path = "src/main/java/test.java";
		setFile(path).toResource("license/MissingLicense.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("license/HasLicense.test");
	}

	private void testUnsupportedFile(String file) throws Exception {
		writePomWithJavaSteps(
				"<licenseHeader>",
				"  <content>",
				"// Hello!",
				"  </content>",
				"</licenseHeader>");

		String path = "src/main/java/com/diffplug/spotless/" + file;
		setFile(path).toResource("license/" + file + ".test");

		mavenRunner().withArguments("spotless:apply").runNoError();

		// file should remain the same
		assertFile(path).sameAsResource("license/" + file + ".test");
	}
}
