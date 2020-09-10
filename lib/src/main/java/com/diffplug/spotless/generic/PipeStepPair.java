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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterStep;

public class PipeStepPair {
	/** The two steps will be named `<name>In` and `<name>Out`. */
	public static Builder named(String name) {
		return new Builder(name);
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

		private Builder(String name) {
			this.name = Objects.requireNonNull(name);
		}

		/** Defines the opening and closing markers. */
		public Builder openClose(String open, String close) {
			return regex(Pattern.quote(open) + "([.\\n]*?)" + Pattern.quote(close));
		}

		/** Defines the pipe via regex. Must have *exactly one* capturing group. */
		public Builder regex(String regex) {
			return regex(Pattern.compile(regex));
		}

		/** Defines the pipe via regex. Must have *exactly one* capturing group. */
		public Builder regex(Pattern regex) {
			this.regex = regex;
			return this;
		}

		public PipeStepPair buildPair() {
			return new PipeStepPair(name, regex);
		}
	}

	final FormatterStep in, out;

	private PipeStepPair(String name, Pattern pattern) {
		StateIn stateIn = new StateIn(pattern);
		StateOut stateOut = new StateOut(stateIn);
		in = FormatterStep.create(name + "In", stateIn, state -> state::format);
		out = FormatterStep.create(name + "Out", stateOut, state -> state::format);
	}

	public FormatterStep in() {
		return in;
	}

	public FormatterStep out() {
		return out;
	}

	static class StateIn implements Serializable {
		private static final long serialVersionUID = -844178006407733370L;

		final Pattern regex;

		public StateIn(Pattern regex) {
			this.regex = regex;
		}

		final transient ArrayList<String> groups = new ArrayList<>();

		private String format(String unix) {
			groups.clear();
			Matcher matcher = regex.matcher(unix);
			while (matcher.find()) {
				groups.add(matcher.group(1));
			}
			return unix;
		}
	}

	static class StateOut implements Serializable {
		private static final long serialVersionUID = -1195263184715054229L;

		final StateIn in;

		StateOut(StateIn in) {
			this.in = in;
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
			}
			if (groupIdx < in.groups.size()) {
				throw new Error("An intermediate step removed a match of '" + in.regex + "' pair.");
			}
			builder.append(unix, lastEnd, unix.length());
			return builder.toString();
		}
	}
}
