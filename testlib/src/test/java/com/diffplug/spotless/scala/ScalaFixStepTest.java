/*
 * Copyright 2016-2020 DiffPlug
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
package com.diffplug.spotless.scala;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.diffplug.spotless.*;

/**
 * The rules used in these tests can run directly on source code without compilation.
 * Other tests that need compilation are written in ScalaExtensionTest.
 */
public class ScalaFixStepTest extends ResourceHarness {
	private ScalaCompiler scalaCompiler;
	private String originalDir;
	private File testFile;

	@Before
	public void before() throws IOException {
		scalaCompiler = new ScalaCompiler("2.13.2", "");
		originalDir = System.getProperty("user.dir");
		final File configFile = createTestFile("scala/scalafix/.scalafix.conf");
		// Scalafix uses "user.dir" as the working directory
		System.setProperty("user.dir", configFile.getParentFile().getAbsolutePath());
		testFile = newFile("basic.sc");
		testFile.createNewFile();
	}

	@After
	public void after() {
		System.setProperty("user.dir", originalDir);
	}

	@Test
	public void behaviorDefaultConfigFilename() throws Exception {
		StepHarnessWithFile.forStep(ScalaFixStep.create("0.9.16", "2.12.11", TestProvisioner.mavenCentral(), scalaCompiler, null))
				.test(testFile, "scala/scalafix/basic.dirty", "scala/scalafix/basic.clean");
	}

	@Test
	public void behaviorCustomConfigFilename() throws Exception {
		StepHarnessWithFile.forStep(ScalaFixStep.create("0.9.16", "2.12.11", TestProvisioner.mavenCentral(), scalaCompiler, createTestFile("scala/scalafix/.scalafix2.conf")))
				.test(testFile, "scala/scalafix/basic.dirty", "scala/scalafix/basic.clean2");
	}

	/*
	@Test
	public void behaviorDefaultConfigFilename_0_9_1() throws Exception {
		StepHarnessWithFile.forStep(ScalaFixStep.create("0.9.1", "2.11.12", TestProvisioner.mavenCentral(), scalaCompiler, null))
				.test(testFile, "scala/scalafix/basic.dirty", "scala/scalafix/basic.clean");
	}

	@Test
	public void behaviorCustomConfigFilename_0_9_1() throws Exception {
		StepHarnessWithFile.forStep(ScalaFixStep.create("0.9.1", "2.11.12", TestProvisioner.mavenCentral(), scalaCompiler, createTestFile("scala/scalafix/.scalafix2.conf")))
				.test(testFile, "scala/scalafix/basic.dirty", "scala/scalafix/basic.clean2");
	}

	The tests above are broken because their transitive dependencies include multiple
	libraries with the same name, specifically:

	- com.lihaoyi:fastparse_2.11:1.0.0
	- org.scalameta:fastparse_2.11:1.0.0

	Seems like dropping support for these old versions wouldn't be a big drawback, no?

	\--- ch.epfl.scala:scalafix-cli_2.11.12:0.9.1
	 +--- org.scala-lang:scala-library:2.11.12
	 +--- ch.epfl.scala:scalafix-reflect_2.11.12:0.9.1
	 |    +--- org.scala-lang:scala-compiler:2.11.12
	 |    |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    +--- org.scala-lang:scala-reflect:2.11.12
	 |    |    |    \--- org.scala-lang:scala-library:2.11.12
	 |    |    +--- org.scala-lang.modules:scala-xml_2.11:1.0.5
	 |    |    |    \--- org.scala-lang:scala-library:2.11.7 -> 2.11.12
	 |    |    \--- org.scala-lang.modules:scala-parser-combinators_2.11:1.0.4
	 |    |         \--- org.scala-lang:scala-library:2.11.6 -> 2.11.12
	 |    +--- org.scala-lang:scala-library:2.11.12
	 |    +--- ch.epfl.scala:scalafix-core_2.11:0.9.1
	 |    |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    +--- org.scalameta:contrib_2.11:4.1.0
	 |    |    |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    |    \--- org.scalameta:scalameta_2.11:4.1.0
	 |    |    |         +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         +--- org.scalameta:common_2.11:4.1.0
	 |    |    |         |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |    +--- org.scalameta:semanticdb_2.11:4.1.0
	 |    |    |         |    |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |    |    \--- com.thesamet.scalapb:scalapb-runtime_2.11:0.8.0-RC1
	 |    |    |         |    |         +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |    |         +--- com.thesamet.scalapb:lenses_2.11:0.8.0-RC1
	 |    |    |         |    |         |    \--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |    |         +--- com.lihaoyi:fastparse_2.11:1.0.0
	 |    |    |         |    |         |    +--- org.scala-lang:scala-library:2.11.11 -> 2.11.12
	 |    |    |         |    |         |    +--- com.lihaoyi:fastparse-utils_2.11:1.0.0
	 |    |    |         |    |         |    |    +--- org.scala-lang:scala-library:2.11.11 -> 2.11.12
	 |    |    |         |    |         |    |    \--- com.lihaoyi:sourcecode_2.11:0.1.4
	 |    |    |         |    |         |    |         \--- org.scala-lang:scala-library:2.11.11 -> 2.11.12
	 |    |    |         |    |         |    \--- com.lihaoyi:sourcecode_2.11:0.1.4 (*)
	 |    |    |         |    |         \--- com.google.protobuf:protobuf-java:3.5.1
	 |    |    |         |    \--- com.lihaoyi:sourcecode_2.11:0.1.4 (*)
	 |    |    |         +--- org.scalameta:dialects_2.11:4.1.0
	 |    |    |         |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |    \--- org.scalameta:common_2.11:4.1.0 (*)
	 |    |    |         +--- org.scalameta:parsers_2.11:4.1.0
	 |    |    |         |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |    +--- org.scalameta:common_2.11:4.1.0 (*)
	 |    |    |         |    +--- org.scalameta:dialects_2.11:4.1.0 (*)
	 |    |    |         |    +--- org.scalameta:inputs_2.11:4.1.0
	 |    |    |         |    |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |    |    +--- org.scalameta:common_2.11:4.1.0 (*)
	 |    |    |         |    |    \--- org.scalameta:io_2.11:4.1.0
	 |    |    |         |    |         +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |    |         \--- org.scalameta:common_2.11:4.1.0 (*)
	 |    |    |         |    +--- org.scalameta:tokens_2.11:4.1.0
	 |    |    |         |    |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |    |    +--- org.scalameta:common_2.11:4.1.0 (*)
	 |    |    |         |    |    +--- org.scalameta:dialects_2.11:4.1.0 (*)
	 |    |    |         |    |    \--- org.scalameta:inputs_2.11:4.1.0 (*)
	 |    |    |         |    +--- org.scalameta:tokenizers_2.11:4.1.0
	 |    |    |         |    |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |    |    +--- org.scalameta:common_2.11:4.1.0 (*)
	 |    |    |         |    |    +--- org.scalameta:dialects_2.11:4.1.0 (*)
	 |    |    |         |    |    +--- org.scalameta:inputs_2.11:4.1.0 (*)
	 |    |    |         |    |    +--- org.scalameta:tokens_2.11:4.1.0 (*)
	 |    |    |         |    |    \--- org.scalameta:fastparse_2.11:1.0.0
	 |    |    |         |    |         +--- org.scala-lang:scala-library:2.11.11 -> 2.11.12
	 |    |    |         |    |         +--- org.scalameta:fastparse-utils_2.11:1.0.0
	 |    |    |         |    |         |    +--- org.scala-lang:scala-library:2.11.11 -> 2.11.12
	 |    |    |         |    |         |    \--- com.lihaoyi:sourcecode_2.11:0.1.4 (*)
	 |    |    |         |    |         \--- com.lihaoyi:sourcecode_2.11:0.1.4 (*)
	 |    |    |         |    \--- org.scalameta:trees_2.11:4.1.0
	 |    |    |         |         +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |         +--- org.scalameta:common_2.11:4.1.0 (*)
	 |    |    |         |         +--- org.scalameta:dialects_2.11:4.1.0 (*)
	 |    |    |         |         +--- org.scalameta:inputs_2.11:4.1.0 (*)
	 |    |    |         |         +--- org.scalameta:tokens_2.11:4.1.0 (*)
	 |    |    |         |         \--- org.scalameta:tokenizers_2.11:4.1.0 (*)
	 |    |    |         +--- org.scalameta:quasiquotes_2.11:4.1.0
	 |    |    |         |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |    +--- org.scalameta:common_2.11:4.1.0 (*)
	 |    |    |         |    +--- org.scalameta:dialects_2.11:4.1.0 (*)
	 |    |    |         |    +--- org.scalameta:inputs_2.11:4.1.0 (*)
	 |    |    |         |    +--- org.scalameta:trees_2.11:4.1.0 (*)
	 |    |    |         |    \--- org.scalameta:parsers_2.11:4.1.0 (*)
	 |    |    |         +--- org.scalameta:tokenizers_2.11:4.1.0 (*)
	 |    |    |         +--- org.scalameta:transversers_2.11:4.1.0
	 |    |    |         |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         |    +--- org.scalameta:common_2.11:4.1.0 (*)
	 |    |    |         |    \--- org.scalameta:trees_2.11:4.1.0 (*)
	 |    |    |         +--- org.scalameta:trees_2.11:4.1.0 (*)
	 |    |    |         +--- org.scalameta:inputs_2.11:4.1.0 (*)
	 |    |    |         \--- org.scalameta:io_2.11:4.1.0 (*)
	 |    |    +--- org.scalameta:symtab_2.11:4.1.0
	 |    |    |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    |    \--- org.scalameta:metacp_2.11:4.1.0
	 |    |    |         +--- org.scala-lang:scala-library:2.11.12
	 |    |    |         +--- org.scalameta:semanticdb_2.11:4.1.0 (*)
	 |    |    |         +--- org.scalameta:cli_2.11:4.1.0
	 |    |    |         |    \--- org.scala-lang:scala-library:2.11.12
	 |    |    |         +--- org.scalameta:io_2.11:4.1.0 (*)
	 |    |    |         \--- org.scala-lang:scalap:2.11.12
	 |    |    |              \--- org.scala-lang:scala-compiler:2.11.12 (*)
	 |    |    +--- org.scalameta:metap_2.11:4.1.0
	 |    |    |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    |    +--- org.scalameta:semanticdb_2.11:4.1.0 (*)
	 |    |    |    +--- org.scalameta:cli_2.11:4.1.0 (*)
	 |    |    |    \--- org.scalameta:inputs_2.11:4.1.0 (*)
	 |    |    +--- com.googlecode.java-diff-utils:diffutils:1.3.0
	 |    |    \--- com.geirsson:metaconfig-typesafe-config_2.11:0.9.1
	 |    |         +--- org.scala-lang:scala-library:2.11.12
	 |    |         +--- com.geirsson:metaconfig-core_2.11:0.9.1
	 |    |         |    +--- org.scala-lang:scala-library:2.11.12
	 |    |         |    +--- com.lihaoyi:pprint_2.11:0.5.3
	 |    |         |    |    +--- org.scala-lang:scala-library:2.11.11 -> 2.11.12
	 |    |         |    |    +--- com.lihaoyi:fansi_2.11:0.2.5
	 |    |         |    |    |    +--- org.scala-lang:scala-library:2.11.8 -> 2.11.12
	 |    |         |    |    |    \--- com.lihaoyi:sourcecode_2.11:0.1.4 (*)
	 |    |         |    |    \--- com.lihaoyi:sourcecode_2.11:0.1.4 (*)
	 |    |         |    \--- org.typelevel:paiges-core_2.11:0.2.0
	 |    |         |         \--- org.scala-lang:scala-library:2.11.11 -> 2.11.12
	 |    |         \--- com.typesafe:config:1.2.1
	 |    +--- ch.epfl.scala:scalafix-rules_2.11:0.9.1
	 |    |    +--- org.scala-lang:scala-library:2.11.12
	 |    |    \--- ch.epfl.scala:scalafix-core_2.11:0.9.1 (*)
	 |    +--- org.scalameta:metacp_2.11:4.1.0 (*)
	 |    \--- org.scala-lang:scala-reflect:2.11.12 (*)
	 +--- ch.epfl.scala:scalafix-interfaces:0.9.1
	 +--- com.martiansoftware:nailgun-server:0.9.1
	 +--- org.eclipse.jgit:org.eclipse.jgit:4.5.4.201711221230-r
	 |    +--- com.jcraft:jsch:0.1.53
	 |    +--- com.googlecode.javaewah:JavaEWAH:0.7.9
	 |    +--- org.apache.httpcomponents:httpclient:4.3.6
	 |    |    +--- org.apache.httpcomponents:httpcore:4.3.3
	 |    |    +--- commons-logging:commons-logging:1.1.3
	 |    |    \--- commons-codec:commons-codec:1.6
	 |    \--- org.slf4j:slf4j-api:1.7.2 -> 1.7.25
	 +--- ch.qos.logback:logback-classic:1.2.3
	 |    +--- ch.qos.logback:logback-core:1.2.3
	 |    \--- org.slf4j:slf4j-api:1.7.25
	 \--- org.apache.commons:commons-text:1.2
	      \--- org.apache.commons:commons-lang3:3.7
	*/
}
