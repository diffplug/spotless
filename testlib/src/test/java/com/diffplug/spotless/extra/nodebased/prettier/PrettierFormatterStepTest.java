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
package com.diffplug.spotless.extra.nodebased.prettier;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;

import com.diffplug.spotless.*;
import com.diffplug.spotless.extra.nodebased.prettier.options.PrettierOptions;
import com.diffplug.spotless.extra.nodebased.prettier.options.PrettierParser;

public class PrettierFormatterStepTest {

	@Test
	public void testPrettierTypescript() throws IOException {
		final FormatterStep formatterStep = PrettierFormatterStep.create(
				TestProvisioner.mavenCentral(),
				new File("/Users/simschla/tmp/demo-main/"),
				new File("/Users/simschla/.nvm/versions/node/v8.11.2/bin/npm"),
				new PrettierConfig(new File("/Users/simschla/tmp/.prettierrc"),
						PrettierOptions.newBuilder()
								.withBracketSpacing(true)
								.build()));

		final Formatter formatter = Formatter.builder()
				.encoding(StandardCharsets.UTF_8)
				.rootDir(new File("/Users/simschla/tmp/demo-basedir").toPath())
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.steps(Arrays.asList(formatterStep))
				.exceptionPolicy(FormatExceptionPolicy.failOnlyOnError())
				.build();

		System.out.println("formatted: " + formatter.applyToAndReturnResultIfDirty(new File("/Users/simschla/tmp/demo-basedir", "example.ts")));
	}

	@Test
	public void testPrettierJson() throws IOException {
		final FormatterStep formatterStep = PrettierFormatterStep.create(
				TestProvisioner.mavenCentral(),
				new File("/Users/simschla/tmp/demo-main/"),
				new File("/Users/simschla/.nvm/versions/node/v8.11.2/bin/npm"),
				new PrettierConfig(new File("/Users/simschla/tmp/.prettierrc"),
						PrettierOptions.newBuilder()
								.withBracketSpacing(true)
								.withParser(PrettierParser.JSON)
								.build()));

		final Formatter formatter = Formatter.builder()
				.encoding(StandardCharsets.UTF_8)
				.rootDir(new File("/Users/simschla/tmp/demo-basedir").toPath())
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.steps(Arrays.asList(formatterStep))
				.exceptionPolicy(FormatExceptionPolicy.failOnlyOnError())
				.build();

		System.out.println("formatted: " + formatter.applyToAndReturnResultIfDirty(new File("/Users/simschla/tmp/demo-basedir", "toformat.json")));
	}

}
