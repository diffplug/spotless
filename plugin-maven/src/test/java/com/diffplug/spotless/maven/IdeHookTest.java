/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless.maven;

import java.io.File;
import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ProcessRunner;

class IdeHookTest extends MavenIntegrationHarness {
	private String output;
	private String error;
	private File dirty;
	private File clean;

	private File outofbounds;

	@BeforeEach
	void before() throws IOException {
		writePomWithFormatSteps("""
				<includes>
				                                <include>DIRTY.md</include>
				                                <include>CLEAN.md</include>
				                            </includes>
				                            <replace>
				                                <name>Greetings to Mars</name>
				                                <search>World</search>
				                                <replacement>Mars</replacement>
				                            </replace>""");

		dirty = setFile("DIRTY.md").toContent("World");
		clean = setFile("CLEAN.md").toContent("Mars");
		outofbounds = setFile("OUTOFBOUNDS.md").toContent("Mars");
	}

	private void runWith(String... arguments) throws IOException, InterruptedException {
		ProcessRunner.Result result = mavenRunner()
				.withArguments(arguments)
				.runNoError();

		this.output = result.stdOutUtf8();
		this.error = result.stdErrUtf8();
	}

	@Test
	void dirty() throws IOException, InterruptedException {
		runWith("spotless:apply", "--quiet", "-DspotlessIdeHook=\"" + dirty.getAbsolutePath() + "\"", "-DspotlessIdeHookUseStdOut=true");
		Assertions.assertThat(output).isEqualTo("Mars");
		Assertions.assertThat(error).startsWith("IS DIRTY");
	}

	@Test
	void clean() throws IOException, InterruptedException {
		runWith("spotless:apply", "--quiet", "-DspotlessIdeHook=" + clean.getAbsolutePath(), "-DspotlessIdeHookUseStdOut=true");
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).startsWith("IS CLEAN");
	}

	@Test
	void outofbounds() throws IOException, InterruptedException {
		runWith("spotless:apply", "--quiet", "-DspotlessIdeHook=" + outofbounds.getAbsolutePath(), "-DspotlessIdeHookUseStdOut=true");
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).isEmpty();
	}

	@Test
	void notAbsolute() throws IOException, InterruptedException {
		runWith("spotless:apply", "--quiet", "-DspotlessIdeHook=\"pom.xml\"", "-DspotlessIdeHookUseStdOut=true");
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).contains("Argument passed to spotlessIdeHook must be an absolute path");
	}
}
