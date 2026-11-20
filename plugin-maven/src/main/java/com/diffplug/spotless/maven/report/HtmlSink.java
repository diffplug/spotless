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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.apache.maven.doxia.sink.Sink;

/**
 * A minimal {@link Sink} that writes a self-contained HTML page, for {@code mvn spotless:report} outside of
 * {@code mvn site}. It handles the events {@link ReportRenderer} emits and ignores the rest, so the plugin
 * does not need Doxia's renderers.
 */
final class HtmlSink implements InvocationHandler {
	private static final String STYLE = "body{font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Helvetica,Arial,sans-serif;"
			+ "max-width:1100px;margin:2em auto;padding:0 1em;color:#1f2328;line-height:1.5}"
			+ "h1{font-size:1.8em;border-bottom:1px solid #d0d7de;padding-bottom:.3em}h2{font-size:1.4em;margin-top:1.5em}"
			+ "table{border-collapse:collapse;margin:.5em 0 1em}"
			+ "th,td{border:1px solid #d0d7de;padding:4px 10px;text-align:left;vertical-align:top}th{background:#f6f8fa}"
			+ "code{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:90%}"
			+ "a{color:#0969da;text-decoration:none}a:hover{text-decoration:underline}";

	private static final Map<String, String> TAGS = Map.ofEntries(
			Map.entry("head_", "</head>\n"),
			Map.entry("title", "<title>"),
			Map.entry("title_", "</title>\n"),
			Map.entry("body", "<body>\n"),
			Map.entry("body_", "</body>\n</html>\n"),
			Map.entry("section1", "<section>\n"),
			Map.entry("section1_", "</section>\n"),
			Map.entry("section2", "<section>\n"),
			Map.entry("section2_", "</section>\n"),
			Map.entry("section3", "<section>\n"),
			Map.entry("section3_", "</section>\n"),
			Map.entry("section4", "<section>\n"),
			Map.entry("section4_", "</section>\n"),
			Map.entry("sectionTitle1", "<h1>"),
			Map.entry("sectionTitle1_", "</h1>\n"),
			Map.entry("sectionTitle2", "<h2>"),
			Map.entry("sectionTitle2_", "</h2>\n"),
			Map.entry("sectionTitle3", "<h3>"),
			Map.entry("sectionTitle3_", "</h3>\n"),
			Map.entry("sectionTitle4", "<h4>"),
			Map.entry("sectionTitle4_", "</h4>\n"),
			Map.entry("paragraph", "<p>"),
			Map.entry("paragraph_", "</p>\n"),
			Map.entry("table", "<table>\n"),
			Map.entry("table_", "</table>\n"),
			Map.entry("tableRows", ""),
			Map.entry("tableRows_", ""),
			Map.entry("tableRow", "<tr>"),
			Map.entry("tableRow_", "</tr>\n"),
			Map.entry("tableHeaderCell", "<th>"),
			Map.entry("tableHeaderCell_", "</th>"),
			Map.entry("tableCell", "<td>"),
			Map.entry("tableCell_", "</td>"),
			Map.entry("list", "<ul>\n"),
			Map.entry("list_", "</ul>\n"),
			Map.entry("listItem", "<li>"),
			Map.entry("listItem_", "</li>\n"),
			Map.entry("link_", "</a>"),
			Map.entry("anchor_", "</a>"),
			Map.entry("bold", "<strong>"),
			Map.entry("bold_", "</strong>"),
			Map.entry("italic", "<em>"),
			Map.entry("italic_", "</em>"),
			Map.entry("monospaced", "<code>"),
			Map.entry("monospaced_", "</code>"),
			Map.entry("lineBreak", "<br>\n"),
			Map.entry("horizontalRule", "<hr>\n"));

	private final Writer out;

	private HtmlSink(Writer out) {
		this.out = out;
	}

	/** Returns a sink that writes HTML to {@code out}, and closes it when the sink is closed. */
	static Sink create(Writer out) {
		return (Sink) Proxy.newProxyInstance(Sink.class.getClassLoader(), new Class<?>[]{Sink.class}, new HtmlSink(out));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) {
		String name = method.getName();
		try {
			String tag = TAGS.get(name);
			if (tag != null) {
				out.write(tag);
				return null;
			}
			switch (name) {
			case "head":
				out.write("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n"
						+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n<style>" + STYLE + "</style>\n");
				return null;
			case "text":
				out.write(Html.escape(String.valueOf(args[0])));
				return null;
			case "rawText":
				out.write(String.valueOf(args[0]));
				return null;
			case "link":
				out.write("<a href=\"" + Html.escape(String.valueOf(args[0])) + "\">");
				return null;
			case "anchor":
				out.write("<a id=\"" + Html.escape(String.valueOf(args[0])) + "\">");
				return null;
			case "flush":
				out.flush();
				return null;
			case "close":
				out.close();
				return null;
			case "hashCode":
				return System.identityHashCode(proxy);
			case "equals":
				return proxy == args[0];
			case "toString":
				return "HtmlSink";
			default:
				// an event the report never emits, such as comment()
				return null;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
