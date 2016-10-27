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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.gradle.api.GradleException;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StringPrinter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Incorporates the PaddedCell machinery into FormatTask.apply() and FormatTask.check().
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
 * or some other kind of mischief.  If they are, it throws a special error message, {@link #youShouldTurnOnPaddedCell()}
 * which tells them to turn on paddedCell.
 *
 * ### spotlessCheck with paddedCell on
 *
 * Spotless check behaves as normal, finding a list of problem files, but then passes that list
 * to {@link #check(FormatTask, Formatter, List)}.  If there were no problem files, then `paddedCell`
 * is no longer necessary, so users might as well turn it off, so we give that info as a warning.
 *
 * If
 *
 */
class PaddedCellTaskMisc {
	/** URL to a page which describes the padded cell thing. */
	private static final String URL = "https://github.com/diffplug/spotless/blob/master/PADDEDCELL.md";

	/**
	 * For well-behaved formatters, {@link PaddedCell#check(Formatter, File)} takes about
	 * twice as long to run as a standard call to {@link Formatter#isClean(File)}.
	 *
	 * By limiting the time that we spend checking for misbehaving rules, we make sure that
	 * {@link #anyMisbehave(Formatter, List)} doesn't cause much slowdown for a failed
	 * spotlessCheck (it causes no slowdown at all to a passed spotlessCheck).
	 *
	 * The real point of this check is to handle the case that a user called spotlessApply,
	 * but spotlessCheck is still failing.  In that case, all of the problemFiles are
	 * guaranteed to be misbehaving, so this time limit doesn't hurt correctness.
	 */
	private static final long MAX_MS_DETERMINING_MISBEHAVIOR = 500;

	/** Returns true if the formatter is misbehaving for any of the given files. */
	public static boolean anyMisbehave(Formatter formatter, List<File> problemFiles) {
		long start = System.currentTimeMillis();
		for (File problem : problemFiles) {
			PaddedCell padded = PaddedCell.check(formatter, problem);
			if (padded.misbehaved()) {
				return true;
			}
			if (System.currentTimeMillis() - start > MAX_MS_DETERMINING_MISBEHAVIOR) {
				return false;
			}
		}
		return false;
	}

	static GradleException youShouldTurnOnPaddedCell(FormatTask task) {
		Path diagnosePath = diagnoseDir(task);
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
				"'" + rootPath.relativize(diagnosePath) + "', and spotlessApply",
				"and spotlessCheck will be self-consistent from here on out.",
				"",
				"For details see " + URL));
	}

	private static Path diagnoseDir(FormatTask task) {
		return task.getProject().getBuildDir().toPath().resolve("spotless-diagnose-" + task.getFormatName());
	}

	@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
	static void check(FormatTask task, Formatter formatter, List<File> problemFiles) throws IOException {
		if (problemFiles.isEmpty()) {
			// if the first pass was successful, then paddedCell() mode is unnecessary
			task.getLogger().info(StringPrinter.buildStringFromLines(
					task.getName() + " is in paddedCell() mode, but it doesn't need to be.",
					"If you remove that option, spotless will run ~2x faster.",
					"For details see " + URL));
		}

		// "fake" Formatter which can use the already-computed result of a PaddedCell as
		Step paddedCellStep = new Step();
		Formatter paddedFormatter = new Formatter(
				formatter.lineEndingPolicy, formatter.encoding, formatter.projectDirectory,
				Collections.singletonList(paddedCellStep));

		// empty out the diagnose folder
		Path diagnoseDir = diagnoseDir(task);
		Path rootDir = task.getProject().getRootDir().toPath();
		cleanDir(diagnoseDir);

		Iterator<File> problemIter = problemFiles.iterator();
		while (problemIter.hasNext()) {
			File problemFile = problemIter.next();

			task.getLogger().debug("Running padded cell check on " + problemFile);
			PaddedCell padded = PaddedCell.check(formatter, problemFile);
			if (!padded.misbehaved()) {
				task.getLogger().debug("    well-behaved.");
			} else {
				// the file is misbehaved, so we'll write all its steps to DIAGNOSE_DIR
				Path relative = rootDir.relativize(problemFile.toPath());
				Path diagnoseFile = diagnoseDir.resolve(relative);
				for (int i = 0; i < padded.steps().size(); ++i) {
					Path path = Paths.get(diagnoseFile + "." + padded.type().name().toLowerCase(Locale.US) + i);
					Files.createDirectories(path.getParent());
					String version = padded.steps().get(i);
					Files.write(path, version.getBytes(formatter.encoding));
				}
				// dump the type of the misbehavior to console
				task.getLogger().quiet("    " + relative + " " + padded.userMessage());

				if (!padded.isResolvable()) {
					// if it's not resolvable, then there's
					// no point killing the build over it
					problemIter.remove();
				} else {
					// if the input is resolvable, we'll use that to try again at
					// determining if it's clean
					paddedCellStep.set(problemFile, padded.steps().get(0));
					if (paddedFormatter.isClean(problemFile)) {
						problemIter.remove();
					}
				}
			}
		}
		if (!problemFiles.isEmpty()) {
			throw task.formatViolationsFor(formatter, problemFiles);
		}
	}

	/** Helper for check(). */
	static class Step implements FormatterStep {
		private File file;
		private String formatted;

		void set(File file, String formatted) {
			this.file = file;
			this.formatted = formatted;
		}

		@Override
		public String format(String raw, File file) throws Throwable {
			if (file.equals(this.file)) {
				this.file = null;
				return Objects.requireNonNull(formatted);
			} else {
				throw new IllegalArgumentException("Must call set() before each call to format.");
			}
		}

		@Override
		public String getName() {
			return "Padded cell result";
		}
	}

	static void apply(FormatTask task, Formatter formatter, File file) throws IOException {
		byte[] rawBytes = Files.readAllBytes(file.toPath());
		String raw = new String(rawBytes, formatter.encoding);
		String rawUnix = LineEnding.toUnix(raw);

		// enforce the format
		String formattedUnix = formatter.applySteps(rawUnix, file);
		// convert the line endings if necessary
		String formatted = formatter.applyLineEndings(formattedUnix, file);

		// if F(input) == input, then the formatter is well-behaving and the input is clean
		byte[] formattedBytes = formatted.getBytes(formatter.encoding);
		if (Arrays.equals(rawBytes, formattedBytes)) {
			return;
		}

		// F(input) != input, so we'll do a padded check
		PaddedCell cell = PaddedCell.check(formatter, file, rawUnix);
		if (!cell.isResolvable()) {
			// nothing we can do, but check will warn and dump out the divergence path
			return;
		}

		// get the canonical bytes
		String canonicalUnix = cell.canonical();
		String canonical = formatter.applyLineEndings(canonicalUnix, file);
		byte[] canonicalBytes = canonical.getBytes(formatter.encoding);
		if (!Arrays.equals(rawBytes, canonicalBytes)) {
			// and write them to disk if needed
			Files.write(file.toPath(), canonicalBytes, StandardOpenOption.TRUNCATE_EXISTING);
		}
	}

	/** Does whatever it takes to turn this path into an empty folder. */
	static void cleanDir(Path folder) throws IOException {
		if (Files.exists(folder)) {
			if (Files.isDirectory(folder)) {
				Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(final Path file, final IOException e) {
						return handleException(e);
					}

					private FileVisitResult handleException(final IOException e) {
						Errors.log().accept(e);
						return FileVisitResult.TERMINATE;
					}

					@Override
					public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
						if (e != null) {
							return handleException(e);
						}
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
			} else {
				Files.delete(folder);
			}
		}
		Files.createDirectories(folder);
	}
}
