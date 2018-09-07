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
package com.diffplug.spotless.extra.nodebased.prettier.options;

import java.util.Optional;

import com.diffplug.spotless.extra.nodebased.wrapper.NodeJSWrapper;
import com.diffplug.spotless.extra.nodebased.wrapper.V8ObjectWrapper;

public class PrettierOptions {

	private final Integer printWidth;

	private final Integer tabWidth;

	private final Boolean useTabs;

	private final Boolean semi;

	private final Boolean singleQuote;

	// "none", "es5" or "all"
	private final String trailingComma;

	private final Boolean bracketSpacing;

	private final Boolean jsxBracketSameLine;

	// "avoid", "always"
	private final String arrowParens;

	private final Integer rangeStart;

	private final Integer rangeEnd;

	private final PrettierParser parser;

	// not supported
	//  String filePath;

	private final Boolean requirePragma;

	private final Boolean insertPragma;

	// "always", "never", "preserve"
	private final String proseWrap;

	private PrettierOptions(Builder builder) {
		printWidth = builder.printWidth;
		tabWidth = builder.tabWidth;
		useTabs = builder.useTabs;
		semi = builder.semi;
		singleQuote = builder.singleQuote;
		trailingComma = builder.trailingComma;
		bracketSpacing = builder.bracketSpacing;
		jsxBracketSameLine = builder.jsxBracketSameLine;
		arrowParens = builder.arrowParens;
		rangeStart = builder.rangeStart;
		rangeEnd = builder.rangeEnd;
		parser = builder.parser;
		requirePragma = builder.requirePragma;
		insertPragma = builder.insertPragma;
		proseWrap = builder.proseWrap;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(PrettierOptions copy) {
		Builder builder = new Builder();
		builder.printWidth = copy.getPrintWidth();
		builder.tabWidth = copy.getTabWidth();
		builder.useTabs = copy.getUseTabs();
		builder.semi = copy.getSemi();
		builder.singleQuote = copy.getSingleQuote();
		builder.trailingComma = copy.getTrailingComma();
		builder.bracketSpacing = copy.getBracketSpacing();
		builder.jsxBracketSameLine = copy.getJsxBracketSameLine();
		builder.arrowParens = copy.getArrowParens();
		builder.rangeStart = copy.getRangeStart();
		builder.rangeEnd = copy.getRangeEnd();
		builder.parser = copy.getParser();
		builder.requirePragma = copy.getRequirePragma();
		builder.insertPragma = copy.getInsertPragma();
		builder.proseWrap = copy.getProseWrap();
		return builder;
	}

	public static PrettierOptions allDefaults() {
		return PrettierOptions.newBuilder().build();
	}

	public Integer getPrintWidth() {
		return printWidth;
	}

	public Integer getTabWidth() {
		return tabWidth;
	}

	public Boolean getUseTabs() {
		return useTabs;
	}

	public Boolean getSemi() {
		return semi;
	}

	public Boolean getSingleQuote() {
		return singleQuote;
	}

	public String getTrailingComma() {
		return trailingComma;
	}

	public Boolean getBracketSpacing() {
		return bracketSpacing;
	}

	public Boolean getJsxBracketSameLine() {
		return jsxBracketSameLine;
	}

	public String getArrowParens() {
		return arrowParens;
	}

	public Integer getRangeStart() {
		return rangeStart;
	}

	public Integer getRangeEnd() {
		return rangeEnd;
	}

	public PrettierParser getParser() {
		return parser;
	}

	public Boolean getRequirePragma() {
		return requirePragma;
	}

	public Boolean getInsertPragma() {
		return insertPragma;
	}

	public String getProseWrap() {
		return proseWrap;
	}

	public static PrettierOptions fromV8Object(V8ObjectWrapper v8PrettierOptions) {
		if (v8PrettierOptions == null) {
			return allDefaults();
		}
		Builder builder = newBuilder();
		v8PrettierOptions.getOptionalInteger("printWidth").ifPresent(builder::withPrintWidth);
		v8PrettierOptions.getOptionalInteger("tabWidth").ifPresent(builder::withTabWidth);
		v8PrettierOptions.getOptionalBoolean("useTabs").ifPresent(builder::withUseTabs);
		v8PrettierOptions.getOptionalBoolean("semi").ifPresent(builder::withSemi);
		v8PrettierOptions.getOptionalBoolean("singleQuote").ifPresent(builder::withSingleQuote);
		v8PrettierOptions.getOptionalString("trailingComma").ifPresent(builder::withTrailingComma);
		v8PrettierOptions.getOptionalBoolean("bracketSpacing").ifPresent(builder::withBracketSpacing);
		v8PrettierOptions.getOptionalBoolean("jsxBracketSameLine").ifPresent(builder::withJsxBracketSameLine);
		v8PrettierOptions.getOptionalString("arrowParens").ifPresent(builder::withArrowParens);
		v8PrettierOptions.getOptionalInteger("rangeStart").ifPresent(builder::withRangeStart);
		v8PrettierOptions.getOptionalInteger("rangeEnd").ifPresent(builder::withRangeEnd);
		v8PrettierOptions.getOptionalString("parser").map(PrettierParser::getByParserName).ifPresent(builder::withParser);
		v8PrettierOptions.getOptionalBoolean("requirePragma").ifPresent(builder::withRequirePragma);
		v8PrettierOptions.getOptionalBoolean("insertPragma").ifPresent(builder::withInsertPragma);
		v8PrettierOptions.getOptionalString("proseWrap").ifPresent(builder::withProseWrap);
		return builder.build();
	}

	public PrettierOptions overrideWith(PrettierOptions overrides) {
		if (overrides == null) {
			return this;
		}
		final Builder builder = newBuilder(this);
		Optional.ofNullable(overrides.getPrintWidth()).ifPresent(builder::withPrintWidth);
		Optional.ofNullable(overrides.getTabWidth()).ifPresent(builder::withTabWidth);
		Optional.ofNullable(overrides.getUseTabs()).ifPresent(builder::withUseTabs);
		Optional.ofNullable(overrides.getSemi()).ifPresent(builder::withSemi);
		Optional.ofNullable(overrides.getSingleQuote()).ifPresent(builder::withSingleQuote);
		Optional.ofNullable(overrides.getTrailingComma()).ifPresent(builder::withTrailingComma);
		Optional.ofNullable(overrides.getBracketSpacing()).ifPresent(builder::withBracketSpacing);
		Optional.ofNullable(overrides.getJsxBracketSameLine()).ifPresent(builder::withJsxBracketSameLine);
		Optional.ofNullable(overrides.getArrowParens()).ifPresent(builder::withArrowParens);
		Optional.ofNullable(overrides.getRangeStart()).ifPresent(builder::withRangeStart);
		Optional.ofNullable(overrides.getRangeEnd()).ifPresent(builder::withRangeEnd);
		Optional.ofNullable(overrides.getParser()).ifPresent(builder::withParser);
		Optional.ofNullable(overrides.getRequirePragma()).ifPresent(builder::withRequirePragma);
		Optional.ofNullable(overrides.getInsertPragma()).ifPresent(builder::withInsertPragma);
		Optional.ofNullable(overrides.getProseWrap()).ifPresent(builder::withProseWrap);
		return builder.build();
	}

	public V8ObjectWrapper toV8Object(NodeJSWrapper nodeJSWrapper) {
		if (nodeJSWrapper == null) {
			throw new IllegalArgumentException("cannot work without nodeJSWrapper");
		}
		final V8ObjectWrapper v8Object = nodeJSWrapper.createNewObject();
		Optional.ofNullable(getPrintWidth()).ifPresent(val -> v8Object.add("printWidth", val));
		Optional.ofNullable(getTabWidth()).ifPresent(val -> v8Object.add("tabWidth", val));
		Optional.ofNullable(getUseTabs()).ifPresent(val -> v8Object.add("useTabs", val));
		Optional.ofNullable(getSemi()).ifPresent(val -> v8Object.add("semi", val));
		Optional.ofNullable(getSingleQuote()).ifPresent(val -> v8Object.add("singleQuote", val));
		Optional.ofNullable(getTrailingComma()).ifPresent(val -> v8Object.add("trailingComma", val));
		Optional.ofNullable(getBracketSpacing()).ifPresent(val -> v8Object.add("bracketSpacing", val));
		Optional.ofNullable(getJsxBracketSameLine()).ifPresent(val -> v8Object.add("jsxBracketSameLine", val));
		Optional.ofNullable(getArrowParens()).ifPresent(val -> v8Object.add("arrowParens", val));
		Optional.ofNullable(getRangeStart()).ifPresent(val -> v8Object.add("rangeStart", val));
		Optional.ofNullable(getRangeEnd()).ifPresent(val -> v8Object.add("rangeEnd", val));
		Optional.ofNullable(getParser()).map(PrettierParser::parserName).ifPresent(val -> v8Object.add("parser", val));
		Optional.ofNullable(getRequirePragma()).ifPresent(val -> v8Object.add("requirePragma", val));
		Optional.ofNullable(getInsertPragma()).ifPresent(val -> v8Object.add("insertPragma", val));
		Optional.ofNullable(getProseWrap()).ifPresent(val -> v8Object.add("proseWrap", val));
		return v8Object;
	}

	public static final class Builder {
		private Integer printWidth;
		private Integer tabWidth;
		private Boolean useTabs;
		private Boolean semi;
		private Boolean singleQuote;
		private String trailingComma;
		private Boolean bracketSpacing;
		private Boolean jsxBracketSameLine;
		private String arrowParens;
		private Integer rangeStart;
		private Integer rangeEnd;
		private PrettierParser parser;
		private Boolean requirePragma;
		private Boolean insertPragma;
		private String proseWrap;

		private Builder() {}

		public Builder withPrintWidth(Integer printWidth) {
			this.printWidth = printWidth;
			return this;
		}

		public Builder withTabWidth(Integer tabWidth) {
			this.tabWidth = tabWidth;
			return this;
		}

		public Builder withUseTabs(Boolean useTabs) {
			this.useTabs = useTabs;
			return this;
		}

		public Builder withSemi(Boolean semi) {
			this.semi = semi;
			return this;
		}

		public Builder withSingleQuote(Boolean singleQuote) {
			this.singleQuote = singleQuote;
			return this;
		}

		public Builder withTrailingComma(String trailingComma) {
			this.trailingComma = trailingComma;
			return this;
		}

		public Builder withBracketSpacing(Boolean bracketSpacing) {
			this.bracketSpacing = bracketSpacing;
			return this;
		}

		public Builder withJsxBracketSameLine(Boolean jsxBracketSameLine) {
			this.jsxBracketSameLine = jsxBracketSameLine;
			return this;
		}

		public Builder withArrowParens(String arrowParens) {
			this.arrowParens = arrowParens;
			return this;
		}

		public Builder withRangeStart(Integer rangeStart) {
			this.rangeStart = rangeStart;
			return this;
		}

		public Builder withRangeEnd(Integer rangeEnd) {
			this.rangeEnd = rangeEnd;
			return this;
		}

		public Builder withParser(PrettierParser parser) {
			this.parser = parser;
			return this;
		}

		public Builder withRequirePragma(Boolean requirePragma) {
			this.requirePragma = requirePragma;
			return this;
		}

		public Builder withInsertPragma(Boolean insertPragma) {
			this.insertPragma = insertPragma;
			return this;
		}

		public Builder withProseWrap(String proseWrap) {
			this.proseWrap = proseWrap;
			return this;
		}

		public PrettierOptions build() {
			return new PrettierOptions(this);
		}
	}
}
