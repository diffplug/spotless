/*
 * Copyright 2016-2024 DiffPlug
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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.gradle.api.provider.Provider;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeEach;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.tree.TreeDef;
import com.diffplug.common.tree.TreeStream;
import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.ResourceHarness;

public class GradleIntegrationHarness extends ResourceHarness {
	public enum GradleVersionSupport {
		JRE_11("5.0"), MINIMUM(SpotlessPlugin.VER_GRADLE_min),
		// technically, this API exists in 6.5, but the flags for it change in 6.6, so we build to that
		CONFIGURATION_CACHE("6.6"),
		// https://docs.gradle.org/7.5/userguide/configuration_cache.html#config_cache:stable
		STABLE_CONFIGURATION_CACHE("7.5");

		final String version;

		GradleVersionSupport(String version) {
			String minVersionForRunningJRE;
			switch (Jvm.version()) {
			case 22:
				// TODO: https://docs.gradle.org/current/userguide/compatibility.html
			case 21:
				minVersionForRunningJRE = "8.5";
				break;
			case 20:
				minVersionForRunningJRE = "8.3";
				break;
			case 19:
				minVersionForRunningJRE = "7.6";
				break;
			case 18:
				minVersionForRunningJRE = "7.5";
				break;
			case 17:
				minVersionForRunningJRE = "7.3";
				break;
			case 16:
				minVersionForRunningJRE = "7.0";
				break;
			case 15:
				minVersionForRunningJRE = "6.7";
				break;
			case 14:
				minVersionForRunningJRE = "6.3";
				break;
			default:
				minVersionForRunningJRE = null;
				break;
			}
			if (minVersionForRunningJRE != null && GradleVersion.version(minVersionForRunningJRE).compareTo(GradleVersion.version(version)) > 0) {
				this.version = minVersionForRunningJRE;
			} else {
				this.version = version;
			}
		}
	}

	public static <T> Provider<T> providerOf(T value) {
		return org.gradle.api.internal.provider.Providers.of(value);
	}

	/**
	 * Each test gets its own temp folder, and we create a gradle
	 * build there and run it.
	 * <p>
	 * Because those test folders don't have a .gitattributes file,
	 * git (on windows) will default to \r\n. So now if you read a
	 * test file from the spotless test resources, and compare it
	 * to a build result, the line endings won't match.
	 * <p>
	 * By sticking this .gitattributes file into the test directory,
	 * we ensure that the default Spotless line endings policy of
	 * GIT_ATTRIBUTES will use \n, so that tests match the test
	 * resources on win and linux.
	 */
	@BeforeEach
	void gitAttributes() throws IOException {
		setFile(".gitattributes").toContent("* text eol=lf");
	}

	protected GradleRunner gradleRunner() throws IOException {
		return GradleRunner.create()
				.withGradleVersion(GradleVersionSupport.MINIMUM.version)
				.withProjectDir(rootFolder())
				.withTestKitDir(getTestKitDir())
				.withPluginClasspath();
	}

	/** Dumps the complete file contents of the folder to the console. */
	protected String getContents() {
		return getContents(subPath -> !subPath.startsWith(".gradle"));
	}

	protected String getContents(Predicate<String> subpathsToInclude) {
		return StringPrinter.buildString(printer -> Errors.rethrow().run(() -> iterateFiles(subpathsToInclude, (subpath, file) -> {
			printer.println("### " + subpath + " ###");
			try {
				printer.println(read(subpath));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		})));
	}

	/** Dumps the filtered file listing of the folder to the console. */
	protected String listFiles(Predicate<String> subpathsToInclude) {
		return StringPrinter.buildString(printer -> iterateFiles(subpathsToInclude, (subPath, file) -> {
			printer.println(subPath + " [" + getFileAttributes(file) + "]");
		}));
	}

	/** Dumps the file listing of the folder to the console. */
	protected String listFiles() {
		return listFiles(subPath -> !subPath.startsWith(".gradle"));
	}

	protected void iterateFiles(Predicate<String> subpathsToInclude, BiConsumer<String, File> consumer) {
		TreeDef<File> treeDef = TreeDef.forFile(Errors.rethrow());
		List<File> files = TreeStream.depthFirst(treeDef, rootFolder())
				.filter(File::isFile)
				.collect(Collectors.toList());

		ListIterator<File> iterator = files.listIterator(files.size());
		int rootLength = rootFolder().getAbsolutePath().length() + 1;
		while (iterator.hasPrevious()) {
			File file = iterator.previous();
			String subPath = file.getAbsolutePath().substring(rootLength);
			if (subpathsToInclude.test(subPath)) {
				consumer.accept(subPath, file);
			}
		}
	}

	protected String getFileAttributes(File file) {
		return (file.canRead() ? "r" : "-") + (file.canWrite() ? "w" : "-") + (file.canExecute() ? "x" : "-");
	}

	protected void checkRunsThenUpToDate() throws IOException {
		checkIsUpToDate(false);
		checkIsUpToDate(true);
	}

	protected void applyIsUpToDate(boolean upToDate) throws IOException {
		taskIsUpToDate("spotlessApply", upToDate);
	}

	protected void checkIsUpToDate(boolean upToDate) throws IOException {
		taskIsUpToDate("spotlessCheck", upToDate);
	}

	private static final int FILESYSTEM_RESOLUTION_MS = FileSignature.machineIsWin() ? 150 : 2000;

	void pauseForFilesystem() {
		Errors.rethrow().run(() -> Thread.sleep(FILESYSTEM_RESOLUTION_MS));
	}

	private void taskIsUpToDate(String task, boolean upToDate) throws IOException {
		pauseForFilesystem();
		BuildResult buildResult = gradleRunner().withArguments(task).build();

		List<String> expected = outcomes(buildResult, upToDate ? TaskOutcome.UP_TO_DATE : TaskOutcome.SUCCESS);
		List<String> notExpected = outcomes(buildResult, upToDate ? TaskOutcome.SUCCESS : TaskOutcome.UP_TO_DATE);
		boolean everythingAsExpected = !expected.isEmpty() && notExpected.isEmpty() && buildResult.getTasks().size() - 1 == expected.size();
		if (!everythingAsExpected) {
			fail("Expected all tasks to be " + (upToDate ? TaskOutcome.UP_TO_DATE : TaskOutcome.SUCCESS) + ", but instead was\n" + buildResultToString(buildResult));
		}
	}

	protected static List<String> outcomes(BuildResult build, TaskOutcome outcome) {
		return build.taskPaths(outcome).stream()
				.filter(s -> !s.equals(":spotlessInternalRegisterDependencies"))
				.collect(Collectors.toList());
	}

	protected static List<BuildTask> outcomes(BuildResult build) {
		return build.getTasks().stream()
				.filter(t -> !t.getPath().equals(":spotlessInternalRegisterDependencies"))
				.collect(Collectors.toList());
	}

	static String buildResultToString(BuildResult result) {
		return StringPrinter.buildString(printer -> {
			for (BuildTask task : result.getTasks()) {
				printer.println(task.getPath() + " " + task.getOutcome());
			}
		});
	}

	private static File getTestKitDir() {
		String gradleUserHome = System.getenv("GRADLE_USER_HOME");
		if (gradleUserHome == null || gradleUserHome.isEmpty()) {
			gradleUserHome = new File(System.getProperty("user.home"), ".gradle").getAbsolutePath();
		}
		return new File(gradleUserHome, "testkit");
	}
}
