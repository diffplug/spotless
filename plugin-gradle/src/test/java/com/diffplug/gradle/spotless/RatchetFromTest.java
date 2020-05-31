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
package com.diffplug.gradle.spotless;

import org.eclipse.jgit.api.Git;
import org.junit.Test;

public class RatchetFromTest extends GradleIntegrationTest {
	@Test
	public void singleProjectExhaustive() throws Exception {
		Git git = Git.init().setDirectory(rootFolder()).call();
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"  ratchetFrom 'baseline'",
				"  format 'misc', {",
				"    target '*.md'",
				"    custom 'lowercase', { str -> str.toLowerCase() }",
				"    bumpThisNumberIfACustomStepChanges(1)",
				"  }",
				"}");
		setFile("test.md").toContent("HELLO");
		git.add().addFilepattern("test.md").call();
		git.commit().setMessage("Initial state").call();
		// tag this initial state as the baseline for spotless to ratchet from
		git.tag().setName("baseline").call();

		// so at this point we have test.md, and it would normally be dirty,
		// but because it is unchanged, spotless says it is clean
		assertClean();

		// but if we change it so that it is not clean, spotless will now say it is dirty
		setFile("test.md").toContent("HELLO WORLD");
		assertDirty();
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.md").hasContent("hello world");

		// but if we make it unchanged again, it goes back to being clean
		setFile("test.md").toContent("HELLO");
		assertClean();

		// and if we make the index dirty
		setFile("test.md").toContent("HELLO WORLD");
		git.add().addFilepattern("test.md").call();
		{
			// and the content dirty in the same way, then it's dirty
			assertDirty();
			// if we make the content something else dirty, then it's dirty
			setFile("test.md").toContent("HELLO MOM");
			assertDirty();
			// if we make the content unchanged, even though index it and index are dirty, then it's clean
			setFile("test.md").toContent("HELLO");
			assertClean();
			// if we delete the file, but it's still in the index, then it's clean
			setFile("test.md").deleted();
			assertClean();
		}
		// if we remove the file from the index
		git.rm().addFilepattern("test.md").setCached(true).call();
		{
			// and it's gone in real life too, then it's clean
			assertClean();
			// if the content is there and unchanged, then it's clean
			setFile("test.md").toContent("HELLO");
			assertClean();
			// if the content is dirty, then it's dirty
			setFile("test.md").toContent("HELLO WORLD");
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
		gradleRunner().withArguments("spotlessCheck").build();
	}

	private void assertDirty() throws Exception {
		gradleRunner().withArguments("spotlessCheck").buildAndFail();
	}
}
