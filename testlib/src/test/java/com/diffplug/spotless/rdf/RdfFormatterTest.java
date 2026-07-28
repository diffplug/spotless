/*
 * Copyright 2024-2026 DiffPlug
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
package com.diffplug.spotless.rdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.provider.Arguments;
import org.opentest4j.AssertionFailedError;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

@EnabledOnJre(JRE.JAVA_25)
public class RdfFormatterTest extends ResourceHarness {

	private static FormatterStep forTurtleFormatterVersion(String version) throws ClassNotFoundException {
		return forTurtleFormatterVersionAndStyle(version, defaultStyle());
	}

	private static FormatterStep forTurtleFormatterVersionAndStyle(String version, Map<String, String> style) throws ClassNotFoundException {
		return RdfFormatterStep.create(
				RdfFormatterConfig.builder().turtleFormatterVersion(version).build(),
				style,
				TestProvisioner.mavenCentral());
	}

	public RdfFormatterTest() {}

	@Test
	void testCoolRdfFormatter_2_0_0_DefaultStyle() throws IOException, ClassNotFoundException {
		String inputDir = "/rdf/ttl/input/";
		String expectedOutputDir = "/rdf/ttl/expected/v2.0.0-default/";
		testBeforeAfterFolders(inputDir, expectedOutputDir, StepHarness.forStep(forTurtleFormatterVersion("2.0.0")));
	}

	@Test
	void testCoolRdfFormatter_2_0_0_style01() throws IOException, ClassNotFoundException {
		String inputDir = "/rdf/ttl/input/";
		String expectedOutputDir = "/rdf/ttl/expected/v2.0.0-style01/";
		testBeforeAfterFolders(inputDir, expectedOutputDir, StepHarness.forStep(forTurtleFormatterVersionAndStyle("2.0.0", style01())));
	}

	@RepeatedTest(3)
	void blankNodeOrderingIsStableInCoolRdfFormatter_2_0_1() throws IOException, ClassNotFoundException {
		String inputDir = "/rdf/ttl/v2.0.1-stable-blank-node-order/input/";
		String expectedOutputDir = "/rdf/ttl/v2.0.1-stable-blank-node-order/expected/";
		Map<String, String> style = Map.of("preserveBlankNodeLabelsAndOrdering", "false");
		testBeforeAfterFolders(inputDir, expectedOutputDir, StepHarness.forStep(forTurtleFormatterVersionAndStyle("2.0.1", style)));
	}

	@Test
	void blankNodeOrderingIsNotStableInCoolRdfFormatter_2_0_0() throws Exception {
		FormatterStep step = forTurtleFormatterVersion("2.0.0");
		File file = new File("blank-node-order.ttl");

		String alphaThenBeta = """
				@prefix ex: <http://example.com/> .

				ex:root
					ex:child [
						ex:id "alpha" ;
						ex:value "1"
					], [
						ex:id "beta" ;
						ex:value "2"
					] .
				""";
		String betaThenAlpha = """
				@prefix ex: <http://example.com/> .

				ex:root
					ex:child [
						ex:id "beta" ;
						ex:value "2"
					], [
						ex:id "alpha" ;
						ex:value "1"
					] .
				""";

		assertThat(step.format(alphaThenBeta, file)).isNotEqualTo(step.format(betaThenAlpha, file));
	}

	private static @NotNull Map<String, String> defaultStyle() {
		return Map.of();
	}

	private static @NotNull Map<String, String> style01() {
		return Map.of(
				"alignPrefixes", "RIGHT",
				"alignPredicates", "true",
				"alignObjects", "true",
				"insertFinalNewline", "false");
	}

	private void testBeforeAfterFolders(String beforeDir, String afterDir, StepHarness stepHarness) throws IOException {
		List<Arguments> args = getBeforeAfterTestResources(beforeDir, afterDir);
		for (Arguments arg : args) {
			String before = (String) arg.get()[0];
			String after = (String) arg.get()[1];
			try {
				stepHarness.testResource(before, after);
			} catch (AssertionFailedError e) {
				throw new AssertionFailedError(String.format("Test failed for input %s, expected output %s\n" + e.getMessage(), before, after), e.getCause());
			}
		}
	}

	private List<Arguments> getBeforeAfterTestResources(String beforeDir, String afterDir)
			throws IOException {
		List<Path> inputs = listTestResources(beforeDir)
				.stream()
				.map(s -> Path.of(beforeDir, s))
				.collect(Collectors.toList());
		List<Path> outputs = listTestResources(afterDir)
				.stream()
				.map(s -> Path.of(afterDir, s))
				.collect(Collectors.toList());
		List<Path> missingOutputs = inputs
				.stream()
				.filter(in -> outputs
						.stream().noneMatch(out -> out.getFileName().equals(in.getFileName())))
				.collect(Collectors.toList());
		if (!missingOutputs.isEmpty()) {
			throw new IllegalStateException("'after' directory %s is missing files corresponding to these 'before' files: %s".formatted(beforeDir, missingOutputs));
		}
		List<Path> missingInputs = outputs
				.stream()
				.filter(o -> inputs
						.stream().noneMatch(in -> in.getFileName().equals(o.getFileName())))
				.collect(Collectors.toList());
		if (!missingInputs.isEmpty()) {
			throw new IllegalStateException("'before' directory %s is missing files corresponding to these 'after' files: %s".formatted(afterDir, missingInputs));
		}
		List<Arguments> arguments = new ArrayList<>();
		for (Path input : inputs) {
			Optional<Path> output = outputs.stream().filter(o -> o.getFileName().equals(input.getFileName())).findFirst();
			if (output.isEmpty()) {
				throw new IllegalStateException("'after' directory %s is missing file %s corresponding to 'before' file %s".formatted(afterDir, input.getFileName(), input));
			}
			arguments.add(Arguments.of(unixRelative(input), unixRelative(output.orElseThrow())));
		}
		return arguments;
	}

	private static @NotNull String unixRelative(Path input) {
		String path = input.toString().replaceAll("\\\\", "/");
		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		return path;
	}
}
