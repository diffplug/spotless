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

import static com.diffplug.spotless.maven.report.SpotlessReport.count;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.maven.doxia.sink.Sink;

import com.diffplug.spotless.maven.report.SpotlessReport.Changes;
import com.diffplug.spotless.maven.report.SpotlessReport.FileResult;
import com.diffplug.spotless.maven.report.SpotlessReport.Format;
import com.diffplug.spotless.maven.report.SpotlessReport.LintRow;

/**
 * Renders a {@link SpotlessReport} through the Doxia {@link Sink} API.
 * <p>
 * It only calls {@code Sink} methods that exist with the same signature in Doxia 1.11 and Doxia 2.0, so the same
 * plugin works with {@code maven-site-plugin} 3.12.x (Doxia 1, Maven 3.9's default) and 3.20 or later (Doxia 2).
 * In particular it avoids {@code verbatim()}, {@code verbatim(boolean)}, {@code tableRows()},
 * {@code tableCell(String)} and {@code tableHeaderCell(String)}, and it closes sinks explicitly instead of with
 * try-with-resources, because the Doxia 1 {@code Sink} is not {@link AutoCloseable}.
 */
final class ReportRenderer {
	/** Opens the sink for another page, given its file name next to the summary page. */
	@FunctionalInterface
	interface PageSinks {
		Sink open(String fileName) throws IOException;
	}

	private static final String PADDED_CELL_URL = "https://github.com/diffplug/spotless/blob/main/PADDEDCELL.md";

	private static final String CLEAN_COLOR = "#2da44e";
	private static final String FORMATTING_COLOR = "#cf222e";
	private static final String LINT_COLOR = "#bf8700";
	private static final String NOT_CONVERGING_COLOR = "#8250df";
	private static final String ERROR_COLOR = "#6e7781";

	private static final String PRE_STYLE = "margin:0 0 1em 0;padding:4px 0;border:1px solid #d0d7de;border-radius:6px;background:#ffffff;"
			+ "color:#1f2328;font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:12px;line-height:1.5;"
			+ "overflow-x:auto;white-space:pre;tab-size:4";
	private static final String LINE_STYLE = "display:block;min-width:max-content;padding:0 10px";

	private static final String TAB_STYLE = "background:rgba(140,149,159,0.3)";

	private final SpotlessReport report;
	private final String mainPage;
	/** Files that need formatting, have lints, do not converge or could not be checked, in report order. */
	private final List<FileResult> problemFiles = new ArrayList<>();
	private final Map<FileResult, Format> formatOf = new IdentityHashMap<>();
	private final Map<FileResult, String> pageOf = new IdentityHashMap<>();

	ReportRenderer(SpotlessReport report, String outputName) {
		this.report = report;
		this.mainPage = outputName + ".html";
		Set<String> usedPages = new HashSet<>();
		usedPages.add(mainPage);
		for (Format format : report.formats()) {
			for (FileResult file : format.files) {
				if (!file.isClean()) {
					problemFiles.add(file);
					formatOf.put(file, format);
					pageOf.put(file, uniquePageName(outputName, file.path, usedPages));
				}
			}
		}
	}

	private static String uniquePageName(String outputName, String path, Set<String> usedPages) {
		String base = outputName + "-" + path.replace('/', '.').replaceAll("[^A-Za-z0-9._-]", "_");
		if (base.length() > 180) {
			// stay well within the file-name limits of common file systems
			base = base.substring(0, 180);
		}
		String name = base + ".html";
		for (int i = 2; !usedPages.add(name); i++) {
			name = base + "-" + i + ".html";
		}
		return name;
	}

	/**
	 * Renders the summary page into {@code mainSink}, then one page per problem file through {@code pages}.
	 * If {@code pages} is {@code null}, the details of each problem file go on the summary page instead.
	 */
	void render(Sink mainSink, PageSinks pages) throws IOException {
		try {
			renderSummaryPage(mainSink, pages != null);
		} finally {
			mainSink.flush();
			mainSink.close();
		}
		if (pages != null) {
			for (FileResult file : problemFiles) {
				Sink sink = pages.open(pageOf.get(file));
				try {
					renderFilePage(sink, file);
				} finally {
					sink.flush();
					sink.close();
				}
			}
		}
	}

	private void renderSummaryPage(Sink sink, boolean detailsOnOwnPages) {
		head(sink, "Spotless");
		sink.body();
		sink.section1();
		title(sink, 1, "Spotless");
		sink.paragraph();
		sink.text("Spotless checks that files match the formatting rules configured for this project. This page covers the files ");
		code(sink, "mvn spotless:check");
		sink.text(" looks at: which ones need formatting, with the changes ");
		code(sink, "mvn spotless:apply");
		sink.text(" would make, and which ones have lint problems. Generating it does not change any file.");
		sink.paragraph_();
		paragraph(sink, verdict());
		if (!report.formats().isEmpty()) {
			renderSummaryTable(sink);
			if (!problemFiles.isEmpty()) {
				renderProblemFiles(sink, detailsOnOwnPages);
				renderLintTable(sink, detailsOnOwnPages);
				if (!detailsOnOwnPages) {
					section(sink, 2);
					title(sink, 2, "Details");
					for (FileResult file : problemFiles) {
						section(sink, 3);
						sink.anchor(anchorOf(file));
						sink.anchor_();
						title(sink, 3, file.path);
						describeFormat(sink, formatOf.get(file));
						renderFileDetails(sink, file, 4);
						section_(sink, 3);
					}
					section_(sink, 2);
				}
			}
		}
		sink.section1_();
		sink.body_();
	}

	private String verdict() {
		if (report.formats().isEmpty()) {
			return "No Spotless formats are configured for this report. It uses the plugin-level <configuration> of spotless-maven-plugin; configuration inside an <execution> is not used.";
		}
		Totals totals = new Totals();
		for (Format format : report.formats()) {
			totals.add(format);
		}
		if (totals.files == 0) {
			return "Spotless found no files to look at. If the formats use ratchetFrom, only files that changed since that commit are included.";
		}
		if (problemFiles.isEmpty()) {
			return totals.files == 1 ? "The only file Spotless looked at is clean." : "All " + totals.files + " files Spotless looked at are clean.";
		}
		List<String> problems = new ArrayList<>();
		if (totals.needFormatting > 0) {
			problems.add(totals.needFormatting + (totals.needFormatting == 1 ? " needs" : " need") + " formatting");
		}
		if (totals.withLints > 0) {
			problems.add(totals.withLints + (totals.withLints == 1 ? " has" : " have") + " lint problems");
		}
		if (totals.notConverging > 0) {
			problems.add(totals.notConverging + (totals.notConverging == 1 ? " does" : " do") + " not converge");
		}
		if (totals.errors > 0) {
			problems.add(totals.errors + " could not be checked");
		}
		StringBuilder verdict = new StringBuilder("Of the ").append(count(totals.files, "file")).append(" Spotless looked at, ");
		for (int i = 0; i < problems.size(); i++) {
			if (i > 0) {
				verdict.append(i == problems.size() - 1 ? " and " : ", ");
			}
			verdict.append(problems.get(i));
		}
		verdict.append('.');
		if (totals.needFormatting > 0) {
			verdict.append(" Run mvn spotless:apply to format them.");
		}
		return verdict.toString();
	}

	private void renderSummaryTable(Sink sink) {
		Totals totals = new Totals();
		for (Format format : report.formats()) {
			totals.add(format);
		}
		boolean showNotConverging = totals.notConverging > 0;
		boolean showErrors = totals.errors > 0;

		section(sink, 2);
		title(sink, 2, "Summary");
		List<String> headers = new ArrayList<>(List.of("Format", "Steps", "Files", "Clean", "Need formatting", "Have lint problems"));
		if (showNotConverging) {
			headers.add("Do not converge");
		}
		if (showErrors) {
			headers.add("Could not be checked");
		}
		headers.add("Overview");
		int[] justification = new int[headers.size()];
		for (int i = 0; i < justification.length; i++) {
			justification[i] = i >= 2 && i < headers.size() - 1 ? Sink.JUSTIFY_RIGHT : Sink.JUSTIFY_LEFT;
		}
		sink.table();
		sink.tableRows(justification, false);
		headerRow(sink, headers);
		for (Format format : report.formats()) {
			Totals row = new Totals();
			row.add(format);
			sink.tableRow();
			sink.tableCell();
			code(sink, format.name);
			sink.tableCell_();
			cell(sink, format.steps.isEmpty() ? "none" : String.join(", ", format.steps));
			totalsCells(sink, row, showNotConverging, showErrors);
			sink.tableRow_();
		}
		if (report.formats().size() > 1) {
			sink.tableRow();
			sink.tableCell();
			sink.bold();
			sink.text("Total");
			sink.bold_();
			sink.tableCell_();
			cell(sink, "");
			totalsCells(sink, totals, showNotConverging, showErrors);
			sink.tableRow_();
		}
		sink.tableRows_();
		sink.table_();
		section_(sink, 2);
	}

	private static void totalsCells(Sink sink, Totals totals, boolean showNotConverging, boolean showErrors) {
		cell(sink, Integer.toString(totals.files));
		cell(sink, Integer.toString(totals.clean));
		cell(sink, Integer.toString(totals.needFormatting));
		cell(sink, Integer.toString(totals.withLints));
		if (showNotConverging) {
			cell(sink, Integer.toString(totals.notConverging));
		}
		if (showErrors) {
			cell(sink, Integer.toString(totals.errors));
		}
		sink.tableCell();
		sink.rawText(bar(totals));
		sink.tableCell_();
	}

	/** A bar showing how the files split into clean, need formatting, lints only, not converging and not checked. */
	private static String bar(Totals totals) {
		String title = totals.clean + " clean, " + totals.needFormatting + " need formatting, " + totals.lintsOnly + " with lint problems only, "
				+ totals.notConverging + " not converging, " + totals.errors + " not checked";
		StringBuilder bar = new StringBuilder("<div title=\"").append(Html.escape(title))
				.append("\" style=\"display:flex;width:160px;height:10px;border-radius:5px;overflow:hidden;background:#eaeef2\">");
		if (totals.files > 0) {
			segment(bar, totals.clean, totals.files, CLEAN_COLOR);
			segment(bar, totals.needFormatting, totals.files, FORMATTING_COLOR);
			segment(bar, totals.lintsOnly, totals.files, LINT_COLOR);
			segment(bar, totals.notConverging, totals.files, NOT_CONVERGING_COLOR);
			segment(bar, totals.errors, totals.files, ERROR_COLOR);
		}
		return bar.append("</div>").toString();
	}

	private static void segment(StringBuilder bar, int part, int whole, String color) {
		if (part > 0) {
			double percent = 100.0 * part / whole;
			bar.append("<div style=\"width:").append(String.format(Locale.ROOT, "%.2f", percent)).append("%;background:").append(color).append("\"></div>");
		}
	}

	private void renderProblemFiles(Sink sink, boolean detailsOnOwnPages) {
		section(sink, 2);
		title(sink, 2, "Files with problems");
		sink.table();
		sink.tableRows(new int[]{Sink.JUSTIFY_LEFT, Sink.JUSTIFY_LEFT, Sink.JUSTIFY_LEFT, Sink.JUSTIFY_RIGHT}, false);
		headerRow(sink, List.of("File", "Format", "Formatting", "Lint problems"));
		for (FileResult file : problemFiles) {
			sink.tableRow();
			sink.tableCell();
			sink.link(linkTo(file, detailsOnOwnPages));
			code(sink, file.path);
			sink.link_();
			sink.tableCell_();
			cell(sink, formatOf.get(file).name);
			cell(sink, formattingStatus(file));
			cell(sink, file.lints.isEmpty() ? "" : Integer.toString(file.lints.size()));
			sink.tableRow_();
		}
		sink.tableRows_();
		sink.table_();
		section_(sink, 2);
	}

	/** Lints are computed on the formatted text, so their line numbers differ from the file on disk only when the diff is not empty. */
	private static boolean linesMove(FileResult file) {
		return file.changes != null && file.changes.diff != null;
	}

	private static String formattingStatus(FileResult file) {
		if (file.error != null) {
			return "could not be checked";
		} else if (file.didNotConverge) {
			return "does not converge";
		} else if (file.changes == null) {
			return "no changes";
		} else if (file.changes.diff == null) {
			return "invisible changes only";
		} else {
			return "+" + file.changes.diff.added() + " -" + file.changes.diff.removed() + " lines";
		}
	}

	private void renderLintTable(Sink sink, boolean detailsOnOwnPages) {
		boolean anyLints = false;
		boolean anyLintsInFilesThatChange = false;
		for (FileResult file : problemFiles) {
			anyLints |= !file.lints.isEmpty();
			anyLintsInFilesThatChange |= !file.lints.isEmpty() && linesMove(file);
		}
		if (!anyLints) {
			return;
		}
		section(sink, 2);
		title(sink, 2, "Lint problems");
		if (anyLintsInFilesThatChange) {
			paragraph(sink, "In files that need formatting, the line numbers refer to the file after mvn spotless:apply has formatted it.");
		}
		sink.table();
		sink.tableRows(new int[]{Sink.JUSTIFY_LEFT, Sink.JUSTIFY_RIGHT, Sink.JUSTIFY_LEFT, Sink.JUSTIFY_LEFT, Sink.JUSTIFY_LEFT}, false);
		headerRow(sink, List.of("File", "Line", "Step", "Code", "Message"));
		for (FileResult file : problemFiles) {
			for (LintRow lint : file.lints) {
				sink.tableRow();
				sink.tableCell();
				sink.link(linkTo(file, detailsOnOwnPages));
				sink.text(file.path);
				sink.link_();
				sink.tableCell_();
				cell(sink, lint.lines());
				cell(sink, lint.step);
				sink.tableCell();
				code(sink, lint.code);
				sink.tableCell_();
				cell(sink, lint.firstLineOfDetail());
				sink.tableRow_();
			}
		}
		sink.tableRows_();
		sink.table_();
		section_(sink, 2);
	}

	private void renderFilePage(Sink sink, FileResult file) {
		// Doxia 1 copies the title into the page unescaped, so keep markup out of it; the heading below shows the exact path
		head(sink, "Spotless: " + file.path.replaceAll("[<>&\"]", "_"));
		sink.body();
		sink.section1();
		title(sink, 1, file.path);
		describeFormat(sink, formatOf.get(file));
		sink.paragraph();
		sink.link(mainPage);
		sink.text("Back to the Spotless report");
		sink.link_();
		sink.paragraph_();
		renderFileDetails(sink, file, 2);
		sink.section1_();
		sink.body_();
	}

	private static void describeFormat(Sink sink, Format format) {
		sink.paragraph();
		sink.text("Format ");
		code(sink, format.name);
		sink.text(format.steps.isEmpty() ? ", no steps, encoding " : ", steps " + String.join(", ", format.steps) + ", encoding ");
		code(sink, format.encoding.name());
		sink.text(".");
		sink.paragraph_();
	}

	private void renderFileDetails(Sink sink, FileResult file, int level) {
		if (file.error != null) {
			section(sink, level);
			title(sink, level, "Could not be checked");
			paragraph(sink, "Spotless could not check this file:");
			sink.rawText(pre(file.error));
			section_(sink, level);
		}
		if (file.didNotConverge) {
			section(sink, level);
			title(sink, level, "Formatting does not converge");
			sink.paragraph();
			sink.text("Applying the formatting steps to this file again and again keeps changing it, so spotless:check and spotless:apply leave the file alone. See ");
			sink.link(PADDED_CELL_URL);
			sink.text("how Spotless handles formatters that misbehave");
			sink.link_();
			sink.text(".");
			sink.paragraph_();
			section_(sink, level);
		}
		Changes changes = file.changes;
		if (changes != null) {
			section(sink, level);
			title(sink, level, "Changes");
			if (changes.note != null) {
				paragraph(sink, changes.note);
			}
			if (changes.diff != null) {
				sink.paragraph();
				sink.text("Running ");
				code(sink, "mvn spotless:apply");
				sink.text(" makes these changes:");
				sink.paragraph_();
				sink.rawText(diffHtml(changes.diff));
				int hidden = changes.diff.totalLines() - changes.diff.lines().size();
				if (hidden > 0) {
					paragraph(sink, count(hidden, "more line") + " of the diff " + (hidden == 1 ? "is" : "are") + " not shown. Raise the report's maxDiffLines parameter to see more.");
				}
			}
			section_(sink, level);
		}
		if (!file.lints.isEmpty()) {
			section(sink, level);
			title(sink, level, "Lint problems");
			if (linesMove(file)) {
				paragraph(sink, "The line numbers refer to the file after mvn spotless:apply has formatted it, the + side of the diff above.");
			}
			sink.table();
			sink.tableRows(new int[]{Sink.JUSTIFY_RIGHT, Sink.JUSTIFY_LEFT, Sink.JUSTIFY_LEFT, Sink.JUSTIFY_LEFT}, false);
			headerRow(sink, List.of("Line", "Step", "Code", "Message"));
			for (LintRow lint : file.lints) {
				sink.tableRow();
				cell(sink, lint.lines());
				cell(sink, lint.step);
				sink.tableCell();
				code(sink, lint.code);
				sink.tableCell_();
				sink.tableCell();
				sink.text(lint.firstLineOfDetail());
				if (!lint.firstLineOfDetail().equals(lint.detail)) {
					// multi-line details, such as stack traces, stay folded until clicked
					sink.rawText("<details><summary>Details</summary>" + pre(lint.detail) + "</details>");
				}
				sink.tableCell_();
				sink.tableRow_();
			}
			sink.tableRows_();
			sink.table_();
			section_(sink, level);
		}
	}

	private static String diffHtml(UnifiedDiff diff) {
		StringBuilder html = new StringBuilder("<pre style=\"").append(PRE_STYLE).append("\">");
		for (UnifiedDiff.Line line : diff.lines()) {
			html.append("<span style=\"").append(LINE_STYLE).append(styleFor(line.kind)).append("\">");
			if (line.kind == UnifiedDiff.Kind.ADDED || line.kind == UnifiedDiff.Kind.REMOVED) {
				appendWithVisibleWhitespace(html, line.text, line.kind == UnifiedDiff.Kind.ADDED ? "rgba(46,160,67,0.45)" : "rgba(207,34,46,0.45)");
			} else {
				html.append(Html.escape(line.text));
			}
			html.append("</span>");
		}
		return html.append("</pre>").toString();
	}

	private static String styleFor(UnifiedDiff.Kind kind) {
		switch (kind) {
		case HUNK:
			return ";background:#ddf4ff;color:#57606a";
		case ADDED:
			return ";background:#e6ffec";
		case REMOVED:
			return ";background:#ffebe9";
		case NOTE:
			return ";color:#57606a;font-style:italic";
		default:
			return "";
		}
	}

	/**
	 * Whitespace changes are invisible in a plain diff, so in changed lines each tab gets a gray background and
	 * trailing whitespace a darker version of the line's color. A change from tabs to spaces then shows up, like the
	 * middle-dot and {@code \t} markers in the message of {@code spotless:check}.
	 */
	private static void appendWithVisibleWhitespace(StringBuilder html, String text, String trailingColor) {
		int end = text.length();
		while (end > 1 && (text.charAt(end - 1) == ' ' || text.charAt(end - 1) == '\t')) {
			end--;
		}
		String[] betweenTabs = text.substring(0, end).split("\t", -1);
		for (int i = 0; i < betweenTabs.length; i++) {
			if (i > 0) {
				html.append("<span style=\"").append(TAB_STYLE).append("\">\t</span>");
			}
			html.append(Html.escape(betweenTabs[i]));
		}
		if (end < text.length()) {
			html.append("<span style=\"background:").append(trailingColor).append("\">").append(Html.escape(text.substring(end))).append("</span>");
		}
	}

	private static String pre(String text) {
		return "<pre style=\"" + PRE_STYLE + ";padding:8px 10px\">" + Html.escape(text) + "</pre>";
	}

	private String linkTo(FileResult file, boolean detailsOnOwnPages) {
		return detailsOnOwnPages ? pageOf.get(file) : "#" + anchorOf(file);
	}

	private String anchorOf(FileResult file) {
		String page = pageOf.get(file);
		return "file-" + page.substring(0, page.length() - ".html".length());
	}

	private static void head(Sink sink, String title) {
		sink.head();
		sink.title();
		sink.text(title);
		sink.title_();
		sink.head_();
	}

	private static void section(Sink sink, int level) {
		switch (level) {
		case 1:
			sink.section1();
			break;
		case 2:
			sink.section2();
			break;
		case 3:
			sink.section3();
			break;
		default:
			sink.section4();
			break;
		}
	}

	private static void section_(Sink sink, int level) {
		switch (level) {
		case 1:
			sink.section1_();
			break;
		case 2:
			sink.section2_();
			break;
		case 3:
			sink.section3_();
			break;
		default:
			sink.section4_();
			break;
		}
	}

	private static void title(Sink sink, int level, String text) {
		switch (level) {
		case 1:
			sink.sectionTitle1();
			sink.text(text);
			sink.sectionTitle1_();
			break;
		case 2:
			sink.sectionTitle2();
			sink.text(text);
			sink.sectionTitle2_();
			break;
		case 3:
			sink.sectionTitle3();
			sink.text(text);
			sink.sectionTitle3_();
			break;
		default:
			sink.sectionTitle4();
			sink.text(text);
			sink.sectionTitle4_();
			break;
		}
	}

	private static void paragraph(Sink sink, String text) {
		sink.paragraph();
		sink.text(text);
		sink.paragraph_();
	}

	private static void code(Sink sink, String text) {
		sink.monospaced();
		sink.text(text);
		sink.monospaced_();
	}

	private static void cell(Sink sink, String text) {
		sink.tableCell();
		sink.text(text);
		sink.tableCell_();
	}

	private static void headerRow(Sink sink, List<String> headers) {
		sink.tableRow();
		for (String header : headers) {
			sink.tableHeaderCell();
			sink.text(header);
			sink.tableHeaderCell_();
		}
		sink.tableRow_();
	}

	/** File counts for one format, or for all of them. Every file is counted in exactly one of the last five buckets. */
	private static final class Totals {
		int files;
		int withLints;
		int clean;
		int needFormatting;
		int lintsOnly;
		int notConverging;
		int errors;

		void add(Format format) {
			for (FileResult file : format.files) {
				files++;
				withLints += file.lints.isEmpty() ? 0 : 1;
				if (file.error != null) {
					errors++;
				} else if (file.didNotConverge) {
					notConverging++;
				} else if (file.changes != null) {
					needFormatting++;
				} else if (!file.lints.isEmpty()) {
					lintsOnly++;
				} else {
					clean++;
				}
			}
		}
	}
}
