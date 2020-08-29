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

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PipeStep {
	final String open, close;
	transient Pattern splitter;
	transient ArrayList<String> groups = new ArrayList<>();
	transient StringBuilder builder = new StringBuilder();

	private PipeStep(String open, String close) {
		this.open = Objects.requireNonNull(open);
		this.close = Objects.requireNonNull(close);
		this.splitter = Pattern.compile(open + "(.*?)" + close, Pattern.DOTALL);
	}

	public String formatIn(String in) {
		groups.clear();
		Matcher matcher = splitter.matcher(in);
		while (matcher.find()) {
			groups.add(matcher.group(1));
		}
		return in;
	}

	public String formatOut(String in) {
		if (groups.isEmpty()) {
			return in;
		}
		builder.setLength(0);
		Matcher matcher = splitter.matcher(in);
		int lastEnd = 0;
		int groupIdx = 0;
		while (matcher.find()) {
			builder.append(in, lastEnd, matcher.start(1));
			builder.append(groups.get(groupIdx));
			lastEnd = matcher.end(1);
		}
		if (groupIdx < groups.size()) {
			throw new Error("An intermediate step removed a '" + open + "' / '" + close + "' pair.");
		}
		builder.append(in, lastEnd, in.length());
		return builder.toString();
	}
}
