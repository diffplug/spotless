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
package com.diffplug.spotless.generic;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class FenceStep {
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
		this.name = Objects.requireNonNull(name);
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
		this.regex = Objects.requireNonNull(regex);
		return this;
	}

	private void assertRegexSet() {
		Objects.requireNonNull(regex, "must call regex() or openClose()");
	}

	/** Returns a step which will apply the given steps but preserve the content selected by the regex / openClose pair. */
	public FormatterStep preserveWithin(List<FormatterStep> steps) {
		assertRegexSet();
		return new PreserveWithin(name, regex, steps);
	}

	/**
	 * Returns a step which will apply the given steps only within the blocks selected by the regex / openClose pair.
	 * Linting within the substeps is not supported.
	 */
	public FormatterStep applyWithin(List<FormatterStep> steps) {
		assertRegexSet();
		return new ApplyWithin(name, regex, steps);
	}

	static class ApplyWithin extends BaseStep {
		private static final long serialVersionUID = 17061466531957339L;

		ApplyWithin(String name, Pattern regex, List<FormatterStep> steps) {
			super(name, regex, steps);
		}

		@Override
		public String apply(Formatter formatter, String unix, File file) throws Exception {
			List<String> groups = groupsZeroed();
			Matcher matcher = regex.matcher(unix);
			while (matcher.find()) {
				// apply the formatter to each group
				groups.add(formatter.compute(matcher.group(1), file));
			}
			// and then assemble the result right away
			return assembleGroups(unix);
		}
	}

	static class PreserveWithin extends BaseStep {
		private static final long serialVersionUID = -8676786492305178343L;

		PreserveWithin(String name, Pattern regex, List<FormatterStep> steps) {
			super(name, regex, steps);
		}

		private void storeGroups(String unix) {
			List<String> groups = groupsZeroed();
			Matcher matcher = regex.matcher(unix);
			while (matcher.find()) {
				// store whatever is within the open/close tags
				groups.add(matcher.group(1));
			}
		}

		@Override
		public String apply(Formatter formatter, String unix, File file) throws Exception {
			storeGroups(unix);
			String formatted = formatter.compute(unix, file);
			return assembleGroups(formatted);
		}
	}

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	public static abstract class BaseStep implements Serializable, FormatterStep, FormatterFunc.Closeable.ResourceFuncNeedsFile<Formatter> {
		final String name;
		private static final long serialVersionUID = -2301848328356559915L;
		final Pattern regex;
		final List<FormatterStep> steps;

		transient ArrayList<String> groups = new ArrayList<>();
		transient StringBuilder builderInternal;

		public BaseStep(String name, Pattern regex, List<FormatterStep> steps) {
			this.name = name;
			this.regex = regex;
			this.steps = steps;
		}

		protected ArrayList<String> groupsZeroed() {
			if (groups == null) {
				groups = new ArrayList<>();
			} else {
				groups.clear();
			}
			return groups;
		}

		private StringBuilder builderZeroed() {
			if (builderInternal == null) {
				builderInternal = new StringBuilder();
			} else {
				builderInternal.setLength(0);
			}
			return builderInternal;
		}

		protected Formatter buildFormatter() {
			return Formatter.builder()
					.encoding(StandardCharsets.UTF_8) // can be any UTF, doesn't matter
					.lineEndingsPolicy(LineEnding.UNIX.createPolicy()) // just internal, won't conflict with user
					.steps(steps)
					.rootDir(Path.of("")) // TODO: error messages will be suboptimal for now, but it will get fixed when we ship linting
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
				// int startLine = 1 + (int) builder.toString().codePoints().filter(c -> c == '\n').count();
				// int endLine = 1 + (int) unix.codePoints().filter(c -> c == '\n').count();

				// throw an error with either the full regex, or the nicer open/close pair
				Matcher openClose = Pattern.compile("\\\\Q([\\s\\S]*?)\\\\E" + "\\Q([\\s\\S]*?)\\E" + "\\\\Q([\\s\\S]*?)\\\\E")
						.matcher(regex.pattern());
				String pattern;
				if (openClose.matches()) {
					pattern = openClose.group(1) + " " + openClose.group(2);
				} else {
					pattern = regex.pattern();
				}
				throw new Error("An intermediate step removed a match of " + pattern);
			}
		}

		@Override
		public String getName() {
			return name;
		}

		private transient Formatter formatter;

		@Nullable
		@Override
		public String format(String rawUnix, File file) throws Exception {
			if (formatter == null) {
				formatter = buildFormatter();
			}
			return this.apply(formatter, rawUnix, file);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			BaseStep step = (BaseStep) o;
			return name.equals(step.name) && regex.pattern().equals(step.regex.pattern()) && regex.flags() == step.regex.flags() && steps.equals(step.steps);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, regex.pattern(), regex.flags(), steps);
		}

		public void cleanup() {
			if (formatter != null) {
				formatter.close();
				formatter = null;
			}
		}
	}
}
