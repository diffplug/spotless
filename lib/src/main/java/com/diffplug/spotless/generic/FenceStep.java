/*
 * Copyright 2020-2025 DiffPlug
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.ConfigurationCacheHackList;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Lint;

public final class FenceStep {
	/** Declares the name of the step. */
	public static FenceStep named(String name) {
		return new FenceStep(name);
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

	String name;
	Pattern regex;

	private FenceStep(String name) {
		this.name = requireNonNull(name);
	}

	/** Defines the opening and closing markers. */
	public FenceStep openClose(String open, String close) {
		return regex(Pattern.quote(open) + "([\\s\\S]*?)" + Pattern.quote(close));
	}

	/** Defines the pipe via regex. Must have *exactly one* capturing group. */
	public FenceStep regex(String regex) {
		return regex(Pattern.compile(regex));
	}

	/** Defines the pipe via regex. Must have *exactly one* capturing group. */
	public FenceStep regex(Pattern regex) {
		this.regex = requireNonNull(regex);
		return this;
	}

	private void assertRegexSet() {
		requireNonNull(regex, "must call regex() or openClose()");
	}

	/** Returns a step which will apply the given steps but preserve the content selected by the regex / openClose pair. */
	public FormatterStep preserveWithin(List<FormatterStep> steps) {
		return createStep(Kind.PRESERVE, steps);
	}

	/**
	 * Returns a step which will apply the given steps only within the blocks selected by the regex / openClose pair.
	 * Linting within the substeps is not supported.
	 */
	public FormatterStep applyWithin(List<FormatterStep> steps) {
		return createStep(Kind.APPLY, steps);
	}

	private FormatterStep createStep(Kind kind, List<FormatterStep> steps) {
		assertRegexSet();
		return FormatterStep.createLazy(name, () -> new RoundtripAndEqualityState(kind, regex, steps, false),
				RoundtripAndEqualityState::toEqualityState,
				RoundtripAndEqualityState::toFormatterFunc);
	}

	private enum Kind {
		APPLY, PRESERVE
	}

	private static final class RoundtripAndEqualityState implements Serializable {
		private static final long serialVersionUID = 272603249547598947L;
		final String regexPattern;
		final int regexFlags;
		final Kind kind;
		final ConfigurationCacheHackList steps;

		/** Roundtrip state. */
		private RoundtripAndEqualityState(Kind kind, Pattern regex, List<FormatterStep> steps, boolean optimizeForEquality) {
			this.kind = kind;
			this.regexPattern = regex.pattern();
			this.regexFlags = regex.flags();
			this.steps = optimizeForEquality ? ConfigurationCacheHackList.forEquality() : ConfigurationCacheHackList.forRoundtrip();
			this.steps.addAll(steps);
		}

		private Pattern regex() {
			return Pattern.compile(regexPattern, regexFlags);
		}

		private List<FormatterStep> steps() {
			return steps.getSteps();
		}

		public RoundtripAndEqualityState toEqualityState() {
			return new RoundtripAndEqualityState(kind, regex(), steps(), true);
		}

		public BaseFormatter toFormatterFunc() {
			return new BaseFormatter(kind, this);
		}
	}

	private static class BaseFormatter implements FormatterFunc.NeedsFile, FormatterFunc.Closeable {
		final Kind kind;
		final Pattern regex;
		final List<FormatterStep> steps;

		final ArrayList<String> groups = new ArrayList<>();
		final StringBuilder builderInternal = new StringBuilder();

		public BaseFormatter(Kind kind, RoundtripAndEqualityState state) {
			this.kind = kind;
			this.regex = state.regex();
			this.steps = state.steps();
		}

		protected ArrayList<String> groupsZeroed() {
			groups.clear();
			return groups;
		}

		private StringBuilder builderZeroed() {
			builderInternal.setLength(0);
			return builderInternal;
		}

		protected Formatter buildFormatter() {
			return Formatter.builder()
					.encoding(UTF_8) // can be any UTF, doesn't matter
					.lineEndingsPolicy(LineEnding.UNIX.createPolicy()) // just internal, won't conflict with user
					.steps(steps)
					.build();
		}

		protected String assembleGroups(String unix) {
			if (groups.isEmpty()) {
				return unix;
			}
			StringBuilder builder = builderZeroed();
			Matcher matcher = regex.matcher(unix);
			int lastEnd = 0;
			int groupIdx = 0;
			while (matcher.find()) {
				builder.append(unix, lastEnd, matcher.start(1));
				builder.append(groups.get(groupIdx));
				lastEnd = matcher.end(1);
				++groupIdx;
			}
			if (groupIdx == groups.size()) {
				builder.append(unix, lastEnd, unix.length());
				return builder.toString();
			} else {
				// these will be needed to generate Lints later on
				int startLine = 1 + (int) builder.toString().codePoints().filter(c -> c == '\n').count();
				int endLine = 1 + (int) unix.codePoints().filter(c -> c == '\n').count();

				// throw an error with either the full regex, or the nicer open/close pair
				Matcher openClose = Pattern.compile("\\\\Q([\\s\\S]*?)\\\\E" + "\\Q([\\s\\S]*?)\\E" + "\\\\Q([\\s\\S]*?)\\\\E")
						.matcher(regex.pattern());
				String pattern;
				if (openClose.matches()) {
					pattern = openClose.group(1) + " " + openClose.group(2);
				} else {
					pattern = regex.pattern();
				}
				throw Lint.atLineRange(startLine, endLine, "fenceRemoved",
						"An intermediate step removed a match of " + pattern).shortcut();
			}
		}

		private Formatter formatter;

		@Override
		public String applyWithFile(String unix, File file) throws Exception {
			if (formatter == null) {
				formatter = buildFormatter();
			}
			List<String> groups = groupsZeroed();
			Matcher matcher = regex.matcher(unix);
			switch (kind) {
			case APPLY:
				while (matcher.find()) {
					// apply the formatter to each group
					groups.add(formatter.compute(matcher.group(1), file));
				}
				// and then assemble the result right away
				return assembleGroups(unix);
			case PRESERVE:
				while (matcher.find()) {
					// store whatever is within the open/close tags
					groups.add(matcher.group(1));
				}
				String formatted = formatter.compute(unix, file);
				return assembleGroups(formatted);
			default:
				throw new Error();
			}
		}

		@Override
		public void close() {
			if (formatter != null) {
				formatter.close();
				formatter = null;
			}
		}
	}
}
