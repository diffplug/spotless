/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.spotless.generic;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class PipeStepPair {
	/** The two steps will be named `<name>In` and `<name>Out`. */
	public static Builder named(String name) {
		return new Builder(name);
	}

	public static String defaultToggleName() {
		return "toggle";
	}

	public static String defaultToggleOff() {
		return "spotless:off";
	}

	public static String defaultToggleOn() {
		return "spotless:on";
	}

	public static class Builder {
		String name;
		Pattern regex;
		List<FormatterStep> steps = new ArrayList<>();

		private Builder(String name) {
			this.name = Objects.requireNonNull(name);
		}

		/** Defines the opening and closing markers. */
		public Builder openClose(String open, String close) {
			return regex(Pattern.quote(open) + "([\\s\\S]*?)" + Pattern.quote(close));
		}

		/** Defines the pipe via regex. Must have *exactly one* capturing group. */
		public Builder regex(String regex) {
			return regex(Pattern.compile(regex));
		}

		/** Defines the pipe via regex. Must have *exactly one* capturing group. */
		public Builder regex(Pattern regex) {
			this.regex = Objects.requireNonNull(regex);
			return this;
		}

		/** Sets the steps which will be applied within the extracted blocks. (Optional) */
		public Builder steps(Path rootPath, Collection<? extends FormatterStep> steps) {
			this.steps.clear();
			this.steps.addAll(steps);
			return this;
		}

		public PipeStepPair buildPair() {
			return new PipeStepPair(name, regex, steps);
		}
	}

	final FormatterStep in, out;

	private PipeStepPair(String name, Pattern pattern, List<FormatterStep> steps) {
		StateIn stateIn = new StateIn(pattern, steps);
		StateOut stateOut = new StateOut(stateIn);
		in = FormatterStep.createLazy(name + "In", () -> stateIn, s -> FormatterFunc.Closeable.of(s.buildFormatter(), stateIn::format));
		out = FormatterStep.create(name + "Out", stateOut, state -> state::format);
	}

	public FormatterStep in() {
		return in;
	}

	public FormatterStep out() {
		return out;
	}

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	static class StateIn implements Serializable {
		private static final long serialVersionUID = -844178006407733370L;

		final Pattern regex;
		final List<FormatterStep> steps;

		public StateIn(Pattern regex, List<FormatterStep> steps) {
			this.regex = Objects.requireNonNull(regex);
			this.steps = Objects.requireNonNull(steps);
		}

		Formatter buildFormatter() {
			return Formatter.builder()
					.encoding(StandardCharsets.UTF_8) // can be any UTF, doesn't matter
					.lineEndingsPolicy(LineEnding.UNIX.createPolicy()) // just internal, won't conflict with user
					.steps(steps)
					.rootDir(INSIDE_PIPE.toPath())
					.build();
		}

		final transient ArrayList<String> groups = new ArrayList<>();

		private String format(Formatter formatter, String unix) throws Exception {
			groups.clear();
			Matcher matcher = regex.matcher(unix);
			while (matcher.find()) {
				groups.add(formatter.compute(matcher.group(1), INSIDE_PIPE));
			}
			return unix;
		}
	}

	private static final File INSIDE_PIPE = new File("inside pipe");

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	static class StateOut implements Serializable {
		private static final long serialVersionUID = -1195263184715054229L;

		final StateIn in;

		StateOut(StateIn in) {
			this.in = Objects.requireNonNull(in);
		}

		final transient StringBuilder builder = new StringBuilder();

		private String format(String unix) {
			if (in.groups.isEmpty()) {
				return unix;
			}
			builder.setLength(0);
			Matcher matcher = in.regex.matcher(unix);
			int lastEnd = 0;
			int groupIdx = 0;
			while (matcher.find()) {
				builder.append(unix, lastEnd, matcher.start(1));
				builder.append(in.groups.get(groupIdx));
				lastEnd = matcher.end(1);
				++groupIdx;
			}
			if (groupIdx == in.groups.size()) {
				builder.append(unix, lastEnd, unix.length());
				return builder.toString();
			} else {
				// throw an error with either the full regex, or the nicer open/close pair
				Matcher openClose = Pattern.compile("\\\\Q([\\s\\S]*?)\\\\E" + "\\Q([\\s\\S]*?)\\E" + "\\\\Q([\\s\\S]*?)\\\\E")
						.matcher(in.regex.pattern());
				String pattern;
				if (openClose.matches()) {
					pattern = openClose.group(1) + " " + openClose.group(2);
				} else {
					pattern = in.regex.pattern();
				}
				throw new Error("An intermediate step removed a match of " + pattern);
			}
		}
	}
}
