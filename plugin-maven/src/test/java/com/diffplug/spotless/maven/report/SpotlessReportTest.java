/*
 * Copyright 2026 DiffPlug
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
package com.diffplug.spotless.maven.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Lint;
import com.diffplug.spotless.LintState;
import com.diffplug.spotless.NeverUpToDateStep;
import com.diffplug.spotless.generic.TrimTrailingWhitespaceStep;

class SpotlessReportTest {
	@TempDir
	File dir;

	private SpotlessReport report = new SpotlessReport(1000);

	@Test
	void cleanProject() throws IOException {
		Formatter formatter = formatter(LineEnding.UNIX, TrimTrailingWhitespaceStep.create());
		add(report.addFormat("Java", formatter), formatter, "src/A.java", "class A {}\n");

		String summary = writeStandalone();
		assertThat(summary).contains("The only file Spotless looked at is clean.").contains("<code>java</code>").contains("trimTrailingWhitespace");
		assertThat(new File(dir, "site").list()).containsExactly("spotless.html");
		assertThat(report.summary()).isEqualTo("Spotless report: 1 file, all clean.");
	}

	@Test
	void fileThatNeedsFormattingGetsItsOwnPageWithADiff() throws IOException {
		Formatter formatter = formatter(LineEnding.UNIX, TrimTrailingWhitespaceStep.create());
		SpotlessReport.Format format = report.addFormat("Java", formatter);
		add(format, formatter, "src/Clean.java", "class Clean {}\n");
		add(format, formatter, "src/Dirty.java", "class Dirty {}   \n");

		String summary = writeStandalone();
		assertThat(summary)
				.contains("Of the 2 files Spotless looked at, 1 needs formatting. Run mvn spotless:apply to format them.")
				.contains("<a href=\"spotless-src.Dirty.java.html\"><code>src/Dirty.java</code></a>")
				.contains("+1 -1 lines")
				.doesNotContain("src/Clean.java");
		String page = read("site/spotless-src.Dirty.java.html");
		assertThat(page)
				.contains("<h1>src/Dirty.java</h1>")
				.contains("<a href=\"spotless.html\">Back to the Spotless report</a>")
				.contains("@@ -1 +1 @@")
				// trailing whitespace is highlighted, because it's invisible otherwise
				.contains("-class Dirty {}<span style=\"background:rgba(207,34,46,0.45)\">   </span>")
				.contains("+class Dirty {}</span>");
		assertThat(report.summary()).isEqualTo("Spotless report: 2 files, 1 needs formatting.");
	}

	@Test
	void tabsInChangedLinesAreVisible() throws IOException {
		FormatterStep tabsToSpaces = NeverUpToDateStep.create("tabsToSpaces", input -> input.replace("\t", "    "));
		Formatter formatter = formatter(LineEnding.UNIX, tabsToSpaces);
		add(report.addFormat("Java", formatter), formatter, "src/Tabs.java", "class Tabs {\n\tint x;\n}\n");

		writeStandalone();
		assertThat(read("site/spotless-src.Tabs.java.html"))
				.contains("-<span style=\"background:rgba(140,149,159,0.3)\">\t</span>int x;")
				.contains("+    int x;");
	}

	@Test
	void lintProblemsAreListed() throws IOException {
		FormatterStep noTodo = NeverUpToDateStep.create("noTodo", input -> {
			if (input.contains("TODO")) {
				throw Lint.atLine(2, "todo", "Remove the TODO\nso nobody forgets it").shortcut();
			}
			return input;
		});
		Formatter formatter = formatter(LineEnding.UNIX, noTodo);
		add(report.addFormat("Java", formatter), formatter, "src/Todo.java", "class Todo {\n// TODO\n}\n");

		String summary = writeStandalone();
		assertThat(summary)
				.contains("1 has lint problems")
				.contains("<h2>Lint problems</h2>")
				.contains("<td>no changes</td><td>1</td>")
				.contains("<td><a href=\"spotless-src.Todo.java.html\">src/Todo.java</a></td><td>2</td><td>noTodo</td><td><code>todo</code></td><td>Remove the TODO</td>")
				// the file doesn't need formatting, so its line numbers are those of the file on disk
				.doesNotContain("line numbers refer to the file after");
		String page = read("site/spotless-src.Todo.java.html");
		assertThat(page)
				.contains("<td>2</td><td>noTodo</td><td><code>todo</code></td><td>Remove the TODO<details><summary>Details</summary>")
				.contains("so nobody forgets it")
				.doesNotContain("<h2>Changes</h2>");
	}

	@Test
	void lintLineNumbersOfFilesThatNeedFormattingAreExplained() throws IOException {
		FormatterStep noTodo = NeverUpToDateStep.create("noTodo", input -> {
			if (input.contains("TODO")) {
				throw Lint.atLine(1, "todo", "Remove the TODO").shortcut();
			}
			return input;
		});
		Formatter formatter = formatter(LineEnding.UNIX, TrimTrailingWhitespaceStep.create(), noTodo);
		add(report.addFormat("Java", formatter), formatter, "src/Both.java", "// TODO   \n");

		assertThat(writeStandalone()).contains("In files that need formatting, the line numbers refer to the file after mvn spotless:apply has formatted it.");
		assertThat(read("site/spotless-src.Both.java.html")).contains("The line numbers refer to the file after mvn spotless:apply has formatted it");
	}

	@Test
	void filesThatDoNotConvergeAreExplained() throws IOException {
		FormatterStep grow = NeverUpToDateStep.create("grow", input -> input + "x");
		Formatter formatter = formatter(LineEnding.UNIX, grow);
		add(report.addFormat("Format", formatter), formatter, "grows.txt", "a\n");

		assertThat(writeStandalone()).contains("<th>Do not converge</th>").contains("does not converge");
		assertThat(read("site/spotless-grows.txt.html")).contains("PADDEDCELL.md").contains("spotless:check and spotless:apply leave the file alone");
		assertThat(report.summary()).isEqualTo("Spotless report: 1 file, 1 does not converge.");
	}

	@Test
	void lineEndingChangesAreExplainedInsteadOfDiffed() throws IOException {
		Formatter formatter = formatter(LineEnding.WINDOWS);
		add(report.addFormat("Format", formatter), formatter, "README.txt", "a\nb\n");

		assertThat(writeStandalone()).contains("invisible changes only");
		assertThat(read("site/spotless-README.txt.html"))
				.contains("Only the line endings change: the file uses LF (\\n), but Spotless expects CRLF (\\r\\n).")
				.doesNotContain("<pre");
	}

	@Test
	void fileContentIsEscaped() throws IOException {
		Formatter formatter = formatter(LineEnding.UNIX, TrimTrailingWhitespaceStep.create());
		add(report.addFormat("Javascript", formatter), formatter, "src/x.js", "<script>alert(1)</script>  \n");

		writeStandalone();
		assertThat(read("site/spotless-src.x.js.html"))
				.contains("&lt;script&gt;alert(1)&lt;/script&gt;")
				.doesNotContain("<script>");
	}

	@Test
	void rawHtmlIsPureAscii() throws IOException {
		// the site renderer replaces "$relativePath" in everything it writes, and may use an encoding without every character
		Formatter formatter = formatter(LineEnding.UNIX, TrimTrailingWhitespaceStep.create());
		add(report.addFormat("Format", formatter), formatter, "a.sh", "echo $relativePath caf\u00e9  \n");

		writeStandalone();
		assertThat(read("site/spotless-a.sh.html"))
				.contains("-echo &#36;relativePath caf&#xe9;")
				.doesNotContain("$relativePath");
	}

	@Test
	void markupInFileNamesStaysOutOfThePageTitle() throws IOException {
		Formatter formatter = formatter(LineEnding.UNIX, TrimTrailingWhitespaceStep.create());
		add(report.addFormat("Format", formatter), formatter, "a&b.txt", "x  \n");

		writeStandalone();
		assertThat(read("site/spotless-a_b.txt.html")).contains("<title>Spotless: a_b.txt</title>").contains("<h1>a&amp;b.txt</h1>");
	}

	@Test
	void longDiffsAreCutOff() throws IOException {
		report = new SpotlessReport(5);
		Formatter formatter = formatter(LineEnding.UNIX, TrimTrailingWhitespaceStep.create());
		add(report.addFormat("Format", formatter), formatter, "big.txt", "line 0   \nline 1   \nline 2   \nline 3   \nline 4   \nline 5   \nline 6   \nline 7   \nline 8   \nline 9   \n");

		writeStandalone();
		// a hunk header, 10 removed and 10 added lines, of which 5 are shown
		assertThat(read("site/spotless-big.txt.html")).contains("16 more lines of the diff are not shown.");
	}

	@Test
	void oneHiddenLineIsSingular() throws IOException {
		report = new SpotlessReport(2);
		Formatter formatter = formatter(LineEnding.UNIX, TrimTrailingWhitespaceStep.create());
		add(report.addFormat("Format", formatter), formatter, "one.txt", "x  \n");

		writeStandalone();
		// a hunk header, a removed and an added line, of which 2 are shown
		assertThat(read("site/spotless-one.txt.html")).contains("1 more line of the diff is not shown.");
	}

	@Test
	void zeroMaxDiffLinesShowsWholeDiffs() throws IOException {
		report = new SpotlessReport(0);
		Formatter formatter = formatter(LineEnding.UNIX, TrimTrailingWhitespaceStep.create());
		add(report.addFormat("Format", formatter), formatter, "one.txt", "x  \n");

		writeStandalone();
		assertThat(read("site/spotless-one.txt.html")).contains("+x</span>").doesNotContain("not shown");
	}

	@Test
	void filesThatCouldNotBeCheckedAreReported() throws IOException {
		Formatter formatter = formatter(LineEnding.UNIX, TrimTrailingWhitespaceStep.create());
		report.addFormat("Java", formatter).addError("src/Broken.java", new IllegalArgumentException("Encoding error! Spotless uses UTF-8"));

		assertThat(writeStandalone()).contains("1 could not be checked").contains("<th>Could not be checked</th>");
		assertThat(read("site/spotless-src.Broken.java.html")).contains("Spotless could not check this file:").contains("Encoding error! Spotless uses UTF-8");
	}

	@Test
	void withoutASinkFactoryTheDetailsGoOnTheSummaryPage() throws IOException {
		Formatter formatter = formatter(LineEnding.UNIX, TrimTrailingWhitespaceStep.create());
		add(report.addFormat("Java", formatter), formatter, "src/Dirty.java", "class Dirty {}   \n");

		StringWriter html = new StringWriter();
		report.render(HtmlSink.create(html), null, dir, "spotless");
		assertThat(html.toString())
				.contains("<a href=\"#file-spotless-src.Dirty.java\"><code>src/Dirty.java</code></a>")
				.contains("<a id=\"file-spotless-src.Dirty.java\"></a><h3>src/Dirty.java</h3>")
				.contains("+class Dirty {}</span>");
	}

	@Test
	void formatsAndPagesGetUniqueNames() throws IOException {
		Formatter formatter = formatter(LineEnding.UNIX, TrimTrailingWhitespaceStep.create());
		SpotlessReport.Format first = report.addFormat("Format", formatter);
		SpotlessReport.Format second = report.addFormat("Format", formatter);
		add(first, formatter, "a/b.txt", "x \n");
		add(second, formatter, "a.b.txt", "y \n");

		String summary = writeStandalone();
		assertThat(summary).contains("<code>format</code>").contains("<code>format #2</code>").contains("<strong>Total</strong>");
		String[] pages = new File(dir, "site").list();
		Arrays.sort(pages);
		assertThat(pages).containsExactly("spotless-a.b.txt-2.html", "spotless-a.b.txt.html", "spotless.html");
	}

	@Test
	void toggleFenceIsExplained() throws IOException {
		Formatter formatter = formatter(LineEnding.UNIX, NeverUpToDateStep.create("toggle", input -> input));
		add(report.addFormat("Java", formatter), formatter, "src/A.java", "class A {}\n");

		assertThat(writeStandalone()).contains("toggle (spotless:off and spotless:on around the configured steps)");
	}

	private Formatter formatter(LineEnding lineEnding, FormatterStep... steps) {
		return Formatter.builder()
				.lineEndingsPolicy(lineEnding.createPolicy())
				.encoding(StandardCharsets.UTF_8)
				.steps(List.of(steps))
				.build();
	}

	private void add(SpotlessReport.Format format, Formatter formatter, String path, String content) throws IOException {
		File file = new File(dir, path);
		Files.createDirectories(file.getParentFile().toPath());
		Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
		format.addFile(path, LintState.of(formatter, file), Files.readAllBytes(file.toPath()), formatter);
	}

	private String writeStandalone() throws IOException {
		File page = report.writeStandalone(new File(dir, "site"), "spotless");
		return new String(Files.readAllBytes(page.toPath()), StandardCharsets.UTF_8);
	}

	private String read(String path) throws IOException {
		return new String(Files.readAllBytes(new File(dir, path).toPath()), StandardCharsets.UTF_8);
	}
}
