/*
 * Copyright 2020-2024 DiffPlug
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

import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefDatabase;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.ClearGitConfig;

@ClearGitConfig
class GitRatchetMavenTest extends MavenIntegrationHarness {
	private static final String TEST_PATH = "src/markdown/test.md";

	private Git initRepo() throws IllegalStateException, GitAPIException, IOException {
		Git git = Git.init().setDirectory(rootFolder()).call();
		RefDatabase refDB = git.getRepository().getRefDatabase();
		refDB.newUpdate(Constants.R_HEADS + "main", false).setNewObjectId(ObjectId.zeroId());
		refDB.newUpdate(Constants.HEAD, false).link(Constants.R_HEADS + "main");
		refDB.newUpdate(Constants.R_HEADS + Constants.MASTER, false).delete();
		return git;
	}

	private static final String RATCHET_FROM_POM = StringPrinter.buildStringFromLines(
			"<formats>",
			"  <format>",
			"    <ratchetFrom>baseline</ratchetFrom>",
			"    <includes>",
			"      <include>src/markdown/*.md</include>",
			"    </includes>",
			"    <replace>",
			"      <name>Lowercase hello</name>",
			"      <search>HELLO</search>",
			"      <replacement>hello</replacement>",
			"    </replace>",
			"    <replace>",
			"      <name>Lowercase world</name>",
			"      <search>WORLD</search>",
			"      <replacement>world</replacement>",
			"    </replace>",
			"    <replace>",
			"      <name>Lowercase world</name>",
			"      <search>MOM</search>",
			"      <replacement>mom</replacement>",
			"    </replace>",
			"  </format>",
			"</formats>");

	@Test
	void singleProjectExhaustive() throws Exception {
		try (Git git = initRepo()) {
			writePom(RATCHET_FROM_POM);
			checkBehavior(git);
		}
	}

	@Test
	void singleProjectExhaustiveGlobal() throws Exception {
		try (Git git = initRepo()) {
			writePom(RATCHET_FROM_POM
					.replace("<ratchetFrom>baseline</ratchetFrom>", "")
					.replace("<formats>", "<ratchetFrom>baseline</ratchetFrom>\n<formats>"));
			checkBehavior(git);
		}
	}

	private void checkBehavior(Git git) throws Exception {
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
		mavenRunner().withArguments("spotless:apply").runNoError();
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

	private void assertClean() throws Exception {
		mavenRunner().withArguments("spotless:check").runNoError();
	}

	private void assertDirty() throws Exception {
		mavenRunner().withArguments("spotless:check").runHasError();
	}
}
