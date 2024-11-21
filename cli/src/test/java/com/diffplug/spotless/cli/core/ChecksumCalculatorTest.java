/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.cli.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.cli.SpotlessAction;
import com.diffplug.spotless.cli.SpotlessActionContextProvider;
import com.diffplug.spotless.cli.steps.SpotlessCLIFormatterStep;
import com.diffplug.spotless.cli.steps.SpotlessFormatterStep;

import picocli.CommandLine;

class ChecksumCalculatorTest {

	private ChecksumCalculator checksumCalculator = new ChecksumCalculator();

	@Test
	void itCalculatesAChecksumForStep() {
		Step step = step(randomPath(), randomString(), argGroup(null, randomByteArray()), List.of(randomPath(), randomPath()));

		String checksum = checksumCalculator.calculateChecksum(step);

		assertThat(checksum).isNotNull();
	}

	@Test
	void itCalculatesDifferentChecksumsForSteps() {
		Step step1 = step(randomPath(), randomString(), argGroup(randomString(), null), List.of(randomPath(), randomPath()));
		Step step2 = step(randomPath(), randomString(), argGroup(null, randomByteArray()), List.of(randomPath(), randomPath()));

		String checksum1 = checksumCalculator.calculateChecksum(step1);
		String checksum2 = checksumCalculator.calculateChecksum(step2);

		assertThat(checksum1).isNotEqualTo(checksum2);
	}

	@Test
	void itRecalculatesSameChecksumsForStep() {
		Step step = step(randomPath(), randomString(), argGroup(null, randomByteArray()), List.of(randomPath(), randomPath()));

		String checksum1 = checksumCalculator.calculateChecksum(step);
		String checksum2 = checksumCalculator.calculateChecksum(step);

		assertThat(checksum1).isEqualTo(checksum2);
	}

	@Test
	void itCalculatesAChecksumForCommandLineStream() {
		Step step = step(randomPath(), randomString(), argGroup(null, randomByteArray()), List.of(randomPath(), randomPath()));
		Action action = action(randomPath());
		SpotlessCommandLineStream commandLineStream = commandLine(action, step);

		String checksum = checksumCalculator.calculateChecksum(commandLineStream);

		assertThat(checksum).isNotNull();
	}

	@Test
	void itCalculatesDifferentChecksumForDifferentCommandLineStreamDueToAction() {
		Step step = step(randomPath(), randomString(), argGroup(null, randomByteArray()), List.of(randomPath(), randomPath()));
		Action action1 = action(randomPath());
		Action action2 = action(randomPath());
		SpotlessCommandLineStream commandLineStream1 = commandLine(action1, step);
		SpotlessCommandLineStream commandLineStream2 = commandLine(action2, step);

		String checksum1 = checksumCalculator.calculateChecksum(commandLineStream1);
		String checksum2 = checksumCalculator.calculateChecksum(commandLineStream2);

		assertThat(checksum1).isNotEqualTo(checksum2);
	}

	@Test
	void itCalculatesDifferentChecksumForDifferentCommandLineStreamDueToSteps() {
		Step step1 = step(randomPath(), randomString(), argGroup(null, randomByteArray()), List.of(randomPath(), randomPath()));
		Step step2 = step(randomPath(), randomString(), argGroup(null, randomByteArray()), List.of(randomPath(), randomPath()));
		Action action = action(randomPath());
		SpotlessCommandLineStream commandLineStream1 = commandLine(action, step1);
		SpotlessCommandLineStream commandLineStream2 = commandLine(action, step2);

		String checksum1 = checksumCalculator.calculateChecksum(commandLineStream1);
		String checksum2 = checksumCalculator.calculateChecksum(commandLineStream2);

		assertThat(checksum1).isNotEqualTo(checksum2);
	}

	@Test
	void itCalculatesDifferentChecksumForDifferentCommandLineStreamDueToStepOrder() {
		Step step1 = step(randomPath(), randomString(), argGroup(null, randomByteArray()), List.of(randomPath(), randomPath()));
		Step step2 = step(randomPath(), randomString(), argGroup(null, randomByteArray()), List.of(randomPath(), randomPath()));
		Action action = action(randomPath());
		SpotlessCommandLineStream commandLineStream1 = commandLine(action, step1, step2);
		SpotlessCommandLineStream commandLineStream2 = commandLine(action, step2, step1);

		String checksum1 = checksumCalculator.calculateChecksum(commandLineStream1);
		String checksum2 = checksumCalculator.calculateChecksum(commandLineStream2);

		assertThat(checksum1).isNotEqualTo(checksum2);
	}

	@Test
	void itDoesSomething2() {
		System.out.println("Hello");
	}

	private static Step step(Path test1, String test2, StepArgGroup argGroup, List<Path> parameters) {
		Step step = new Step();
		step.test1 = test1;
		step.test2 = test2;
		step.argGroup = argGroup;
		step.parameters = parameters;
		return step;
	}

	private static StepArgGroup argGroup(String test3, byte[] test4) {
		StepArgGroup argGroup = new StepArgGroup();
		argGroup.test3 = test3;
		argGroup.test4 = test4;
		return argGroup;
	}

	private static Path randomPath() {
		return Path.of(randomString());
	}

	private static byte[] randomByteArray() {
		return randomString().getBytes(StandardCharsets.UTF_8);
	}

	private static String randomString() {
		return Long.toHexString(ThreadLocalRandom.current().nextLong());
	}

	static class Step extends SpotlessFormatterStep {

		@CommandLine.Option(names = "--test1")
		Path test1;

		@CommandLine.Option(names = "--test2")
		String test2;

		@CommandLine.ArgGroup(exclusive = true)
		StepArgGroup argGroup;

		@CommandLine.Parameters
		List<Path> parameters;

		@NotNull
		@Override
		public List<FormatterStep> prepareFormatterSteps(SpotlessActionContext context) {
			return List.of();
		}
	}

	static class StepArgGroup {
		@CommandLine.Option(names = "--test3")
		String test3;

		@CommandLine.Option(names = "--test4")
		byte[] test4;
	}

	private static Action action(Path baseDir) {
		Action action = new Action();
		action.baseDir = baseDir;
		return action;
	}

	@CommandLine.Command(name = "action")
	static class Action implements SpotlessAction {
		@CommandLine.Option(names = {"--basedir"})
		Path baseDir;

		@Override
		public Integer executeSpotlessAction(@NotNull List<FormatterStep> formatterSteps) {
			return 0;
		}
	}

	private static SpotlessCommandLineStream commandLine(SpotlessAction action, SpotlessFormatterStep... steps) {
		return new FixedCommandLineStream(Arrays.asList(steps), List.of(action));
	}

	static class FixedCommandLineStream implements SpotlessCommandLineStream {
		private final List<SpotlessCLIFormatterStep> formatterSteps;
		private final List<SpotlessAction> actions;

		FixedCommandLineStream(List<SpotlessCLIFormatterStep> formatterSteps, List<SpotlessAction> actions) {
			this.formatterSteps = formatterSteps;
			this.actions = actions;
		}

		@Override
		public Stream<SpotlessCLIFormatterStep> formatterSteps() {
			return formatterSteps.stream();
		}

		@Override
		public Stream<SpotlessAction> actions() {
			return actions.stream();
		}

		@Override
		public Stream<SpotlessActionContextProvider> contextProviders() {
			return Stream.empty();
		}
	}

}
