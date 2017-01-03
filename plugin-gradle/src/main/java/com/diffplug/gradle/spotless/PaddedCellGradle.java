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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.gradle.api.GradleException;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCellBulk;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Incorporates the PaddedCell machinery into SpotlessTask.apply() and SpotlessTask.check().
 *
 * Here's the general workflow:
 *
 * ### Identify that paddedCell is needed
 *
 * Initially, paddedCell is off.  That's the default, and there's no need for users to know about it.
 *
 * If they encounter a scenario where `spotlessCheck` fails after calling `spotlessApply`, then they would
 * justifiably be frustrated.  Luckily, every time `spotlessCheck` fails, it passes the failed files to
 * {@link #anyMisbehave(Formatter, List)}, which checks to see if any of the rules are causing a cycle
 * or some other kind of mischief.  If they are, it throws a special error message,
 * {@link #youShouldTurnOnPaddedCell(SpotlessTask)} which tells them to turn on paddedCell.
 *
 * ### spotlessCheck with paddedCell on
 *
 * Spotless check behaves as normal, finding a list of problem files, but then passes that list
 * to {@link #check(SpotlessTask, Formatter, List)}.  If there were no problem files, then `paddedCell`
 * is no longer necessary, so users might as well turn it off, so we give that info as a warning.
 */
// TODO: Cleanup this javadoc
class PaddedCellGradle {
	/** URL to a page which describes the padded cell thing. */
	private static final String URL = "https://github.com/diffplug/spotless/blob/master/PADDEDCELL.md";

	static GradleException youShouldTurnOnPaddedCell(SpotlessTask task) {
		Path rootPath = task.getProject().getRootDir().toPath();
		return new GradleException(StringPrinter.buildStringFromLines(
				"You have a misbehaving rule which can't make up its mind.",
				"This means that spotlessCheck will fail even after spotlessApply has run.",
				"",
				"This is a bug in a formatting rule, not Spotless itself, but Spotless can",
				"work around this bug and generate helpful bug reports for the broken rule",
				"if you add 'paddedCell()' to your build.gradle as such: ",
				"",
				"    spotless {",
				"        format 'someFormat', {",
				"            ...",
				"            paddedCell()",
				"        }",
				"    }",
				"",
				"The next time you run spotlessCheck, it will put helpful bug reports into",
				"'" + rootPath.relativize(diagnoseDir(task).toPath()) + "', and spotlessApply",
				"and spotlessCheck will be self-consistent from here on out.",
				"",
				"For details see " + URL));
	}

	private static File diagnoseDir(SpotlessTask task) {
		return new File(task.getProject().getBuildDir(), "spotless-diagnose-" + task.formatName());
	}

	@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
	static void check(SpotlessTask task, Formatter formatter, List<File> problemFiles) throws IOException {
		if (problemFiles.isEmpty()) {
			// if the first pass was successful, then paddedCell() mode is unnecessary
			task.getLogger().info(StringPrinter.buildStringFromLines(
					task.getName() + " is in paddedCell() mode, but it doesn't need to be.",
					"If you remove that option, spotless will run ~2x faster.",
					"For details see " + URL));
		}

		File diagnoseDir = diagnoseDir(task);
		File rootDir = task.getProject().getRootDir();
		List<File> stillFailing = PaddedCellBulk.check(rootDir, diagnoseDir, formatter, problemFiles);
		if (!stillFailing.isEmpty()) {
			throw task.formatViolationsFor(formatter, problemFiles);
		}
	}
}
