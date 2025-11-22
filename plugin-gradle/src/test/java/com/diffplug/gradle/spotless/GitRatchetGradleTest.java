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
package com.diffplug.gradle.spotless;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.diffplug.spotless.ClearGitConfig;

@ClearGitConfig
class GitRatchetGradleTest extends GradleIntegrationHarness {
	private static final String TEST_PATH = "src/markdown/test.md";

	private Git initRepo() throws IllegalStateException, GitAPIException, IOException {
		Git git = Git.init().setDirectory(rootFolder()).call();
		RefDatabase refDB = git.getRepository().getRefDatabase();
		refDB.newUpdate(Constants.R_HEADS + "main", false).setNewObjectId(ObjectId.zeroId());
		refDB.newUpdate(Constants.HEAD, false).link(Constants.R_HEADS + "main");
		refDB.newUpdate(Constants.R_HEADS + Constants.MASTER, false).delete();
		return git;
	}

	@Override
	public GradleRunner gradleRunner() throws IOException {
		return super.gradleRunner().withGradleVersion(GradleVersionSupport.CUSTOM_STEPS.version);
	}

	@ParameterizedTest
	//@ValueSource(ints = {0, 1}) // TODO: this is a flaky configuration cache issue that started with Gradle 8.5
	@ValueSource(ints = 0)
	void singleProjectExhaustive(int useConfigCache) throws Exception {
		try (Git git = initRepo()) {
			if (useConfigCache == 1) {
				setFile("gradle.properties").toContent("org.gradle.unsafe.configuration-cache=true");
			}
			setFile("build.gradle").toLines(
					"plugins {",
					"  id 'com.diffplug.spotless'",
					"}",
					"spotless {",
					"  ratchetFrom 'baseline'",
					"  format 'misc', {",
					"    target 'src/markdown/*.md'",
					"    custom 'lowercase', { str -> str.toLowerCase() }",
					"    bumpThisNumberIfACustomStepChanges(1)",
					"  }",
					"}");
			setFile(TEST_PATH).toContent("HELLO");
			git.add().addFilepattern(TEST_PATH).call();
			git.commit().setMessage("Initial state").call();
			// tag this initial state as the baseline for spotless to ratchet from
			git.tag().setName("baseline").call();

			// so at this point we have test.md, and it would normally be dirty,
			// but because it is unchanged, spotless says it is clean
			assertClean();

			// but if we change it so that it is not clean, spotless will now say it is dirty
			setFile(TEST_PATH).toContent("HELLO WORLD");
			assertDirty();
			gradleRunner().withArguments("spotlessApply").build();
			assertFile(TEST_PATH).hasContent("hello world");

			// but if we make it unchanged again, it goes back to being clean
			setFile(TEST_PATH).toContent("HELLO");
			assertClean();

			// and if we make the index dirty
			setFile(TEST_PATH).toContent("HELLO WORLD");
			git.add().addFilepattern(TEST_PATH).call();
			{
				// and the content dirty in the same way, then it's dirty
				assertDirty();
				// if we make the content something else dirty, then it's dirty
				setFile(TEST_PATH).toContent("HELLO MOM");
				assertDirty();
				// if we make the content unchanged, even though index it and index are dirty, then it's clean
				setFile(TEST_PATH).toContent("HELLO");
				assertClean();
				// if we delete the file, but it's still in the index, then it's clean
				setFile(TEST_PATH).deleted();
				assertClean();
			}
			// if we remove the file from the index
			git.rm().addFilepattern(TEST_PATH).setCached(true).call();
			{
				// and it's gone in real life too, then it's clean
				assertClean();
				// if the content is there and unchanged, then it's clean
				setFile(TEST_PATH).toContent("HELLO");
				assertClean();
				// if the content is dirty, then it's dirty
				setFile(TEST_PATH).toContent("HELLO WORLD");
				assertDirty();
			}

			// new files always get checked
			setFile("new.md").toContent("HELLO");
			{
				assertDirty();
				// even if they are added
				git.add().addFilepattern("new.md").call();
				assertDirty();
			}
		}
	}

	private void assertClean() throws Exception {
		gradleRunner().withArguments("spotlessCheck").build();
	}

	private void assertDirty() throws Exception {
		gradleRunner().withArguments("spotlessCheck").buildAndFail();
	}

	private BuildResultAssertion assertPass(String... tasks) throws Exception {
		return new BuildResultAssertion(gradleRunner().withArguments(tasks).build());
	}

	private BuildResultAssertion assertFail(String... tasks) throws Exception {
		return new BuildResultAssertion(gradleRunner().withArguments(tasks).buildAndFail());
	}

	private static final String BASELINE_ROOT = "fdc3ca3c850cee44d95d32c64cda30afbb29323c";
	private static final String BASELINE_CLEAN = "65fdd75c1ae00c0646f6487d68c44ddca51f0841";
	private static final String BASELINE_DIRTY = "4cfc3358ccbf186738b82a60276b1e5306bc3870";

	@ParameterizedTest
	//@ValueSource(ints = {0, 1}) // TODO: this is a flaky configuration cache issue that started with Gradle 8.5
	@ValueSource(ints = 0)
	void multiProject(int useConfigCache) throws Exception {
		try (Git git = initRepo()) {
			if (useConfigCache == 1) {
				setFile("gradle.properties").toContent("org.gradle.unsafe.configuration-cache=true");
			}
			setFile("settings.gradle").toLines(
					"plugins {",
					"  id 'com.diffplug.spotless' apply false",
					"}",
					"include 'clean'",
					"include 'dirty'",
					"include 'added'");
			setFile("spotless.gradle").toLines(
					"apply plugin: 'com.diffplug.spotless'",
					"spotless {",
					"  ratchetFrom 'main'",
					"  format 'misc', {",
					"    target 'src/markdown/*.md'",
					"    custom 'lowercase', { str -> str.toLowerCase() }",
					"    bumpThisNumberIfACustomStepChanges(1)",
					"  }",
					"}");
			setFile(".gitignore").toContent("build/\n.gradle\n*.properties\n");
			setFile("build.gradle").toContent("apply from: rootProject.file('spotless.gradle') // root");
			setFile(TEST_PATH).toContent("HELLO");
			setFile("clean/build.gradle").toContent("apply from: rootProject.file('spotless.gradle') // clean");
			setFile("clean/" + TEST_PATH).toContent("HELLO");
			setFile("dirty/build.gradle").toContent("apply from: rootProject.file('spotless.gradle') // dirty");
			setFile("dirty/" + TEST_PATH).toContent("HELLO");
			setFile("added/build.gradle").toContent("apply from: rootProject.file('spotless.gradle') // added");
			RevCommit baseline = addAndCommit(git);
			if (useConfigCache == 1) {
				setFile("gradle.properties").toContent("org.gradle.unsafe.configuration-cache=true");
			}

			ObjectId cleanFolder = TreeWalk.forPath(git.getRepository(), "clean", baseline.getTree()).getObjectId(0);
			ObjectId dirtyFolder = TreeWalk.forPath(git.getRepository(), "dirty", baseline.getTree()).getObjectId(0);

			assertThat(baseline.getTree().toObjectId()).isEqualTo(ObjectId.fromString(BASELINE_ROOT));
			assertThat(cleanFolder).isEqualTo(ObjectId.fromString(BASELINE_CLEAN));
			assertThat(dirtyFolder).isEqualTo(ObjectId.fromString(BASELINE_DIRTY));

			assertPass("spotlessCheck")
					.outcome(":spotlessCheck", TaskOutcome.SUCCESS)
					.outcome(":clean:spotlessCheck", TaskOutcome.SUCCESS)
					.outcome(":dirty:spotlessCheck", TaskOutcome.SUCCESS);

			setFile("added/" + TEST_PATH).toContent("HELLO");
			assertPass("spotlessMisc")
					.outcome(":spotlessMisc", TaskOutcome.UP_TO_DATE)
					.outcome(":clean:spotlessMisc", TaskOutcome.UP_TO_DATE)
					.outcome(":dirty:spotlessMisc", TaskOutcome.UP_TO_DATE)
					.outcome(":added:spotlessMisc", TaskOutcome.SUCCESS);
			assertFail(":added:spotlessCheck");
			assertPass(":added:spotlessApply");

			// now dirty is "git dirty" and "format dirty"
			setFile("dirty/" + TEST_PATH).toContent("HELLO WORLD");
			assertFail(":dirty:spotlessCheck")
					.outcome(":dirty:spotlessMisc", TaskOutcome.SUCCESS);
			assertPass("spotlessApply")
					.outcome(":dirty:spotlessMisc", TaskOutcome.UP_TO_DATE);
			// now it is "git dirty" but "format clean"
			assertPass("spotlessCheck");
			// and every single task is up-to-date
			assertPass("spotlessCheck")
					.outcome(":spotlessMisc", TaskOutcome.UP_TO_DATE)
					.outcome(":clean:spotlessMisc", TaskOutcome.UP_TO_DATE)
					.outcome(":dirty:spotlessMisc", TaskOutcome.UP_TO_DATE)
					.outcome(":added:spotlessMisc", TaskOutcome.UP_TO_DATE);

			RevCommit next = addAndCommit(git);
			assertThat(next.getTree().toObjectId()).isNotEqualTo(baseline.getTree().toObjectId());
			// if we commit to main (the baseline), then tasks will be out of date only because the baseline changed
			// TO REPEAAT:
			// - everything was up-to-date
			// - we pressed "commit", which didn't change the files, just the baseline
			// - and that causes spotless to be out-of-date

			ObjectId nextCleanFolder = TreeWalk.forPath(git.getRepository(), "clean", next.getTree()).getObjectId(0);
			ObjectId nextDirtyFolder = TreeWalk.forPath(git.getRepository(), "dirty", next.getTree()).getObjectId(0);
			assertThat(nextCleanFolder).isEqualTo(cleanFolder);    // the baseline for 'clean' didn't change
			assertThat(nextDirtyFolder).isNotEqualTo(dirtyFolder); // only the baseline for dirty

			// check will still pass, but the tasks are all out of date
			assertPass("spotlessCheck")
					.outcome(":spotlessMisc", TaskOutcome.SUCCESS)
					.outcome(":clean:spotlessMisc", TaskOutcome.UP_TO_DATE)	// with up-to-dateness based on subtree, this is UP-TO-DATE
					.outcome(":dirty:spotlessMisc", TaskOutcome.SUCCESS)
					.outcome(":added:spotlessMisc", TaskOutcome.SUCCESS);
		}
	}

	public static class BuildResultAssertion {
		BuildResult result;

		BuildResultAssertion(BuildResult result) {
			this.result = requireNonNull(result);
		}

		public BuildResultAssertion outcome(String taskPath, TaskOutcome expected) {
			TaskOutcome actual = result.getTasks().stream()
					.filter(task -> task.getPath().equals(taskPath))
					.findAny().get().getOutcome();
			assertThat(actual).isEqualTo(expected);
			return this;
		}
	}

	private RevCommit addAndCommit(Git git) throws NoFilepatternException, GitAPIException {
		PersonIdent emptyPerson = new PersonIdent("jane doe", "jane@doe.com", new Date(0), TimeZone.getTimeZone("UTC"));
		git.add().addFilepattern(".").call();
		return git.commit().setMessage("baseline")
				.setCommitter(emptyPerson)
				.setAuthor(emptyPerson)
				.call();
	}
}
