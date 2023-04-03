/*
 * Copyright 2021-2023 DiffPlug
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

import static org.assertj.core.api.Assumptions.assumeThat;

import javax.script.ScriptEngineManager;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

public class Jsr223Test extends MavenIntegrationHarness {

	@Test
	public void buildInNashorn() throws Exception {
		// This will only work for JDKs that bundle nashorn (8-14)
		assumeThat(new ScriptEngineManager().getEngineByName("nashorn")).isNotNull();
		writePomWithFormatSteps(
				"<jsr223>",
				"  <name>Greetings to Mars</name>",
				"  <engine>nashorn</engine>",
				"  <script>source.replace('World','Mars');</script>",
				"</jsr223>");
		runTest("Hello World", "Hello Mars");
	}

	@Test
	public void groovyFromJarState() throws Exception {
		writePomWithFormatSteps(
				"<jsr223>",
				"  <name>Greetings to Mars</name>",
				"  <dependency>org.codehaus.groovy:groovy-jsr223:3.0.9</dependency>",
				"  <engine>groovy</engine>",
				"  <script>source.replace('World','Mars')</script>",
				"</jsr223>");
		runTest("Hello World", "Hello Mars");
	}

	private void runTest(String sourceContent, String targetContent) throws Exception {
		var path = "src/main/java/test.java";
		setFile(path).toContent(sourceContent);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).hasContent(targetContent);
	}
}
