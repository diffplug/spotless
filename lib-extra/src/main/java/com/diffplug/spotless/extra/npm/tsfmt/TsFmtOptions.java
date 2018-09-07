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
package com.diffplug.spotless.extra.npm.tsfmt;

import java.io.Serializable;
import java.util.Optional;

import com.diffplug.spotless.extra.npm.wrapper.NodeJSWrapper;
import com.diffplug.spotless.extra.npm.wrapper.V8ObjectWrapper;

public class TsFmtOptions implements Serializable {

	private final Boolean verbose;

	private final String basedir;

	private final Boolean tsconfig;

	private final String tsconfigFile;

	private final Boolean tslint;

	private final String tslintFile;

	private final Boolean editorconfig;

	private final Boolean vscode;

	private final String vscodeFile;

	private final Boolean tsfmt;

	private final String tsfmtFile;

	private TsFmtOptions(Builder builder) {
		verbose = builder.verbose;
		basedir = builder.basedir;
		tsconfig = builder.tsconfig;
		tsconfigFile = builder.tsconfigFile;
		tslint = builder.tslint;
		tslintFile = builder.tslintFile;
		editorconfig = builder.editorconfig;
		vscode = builder.vscode;
		vscodeFile = builder.vscodeFile;
		tsfmt = builder.tsfmt;
		tsfmtFile = builder.tsfmtFile;
	}

	public Boolean getVerbose() {
		return verbose;
	}

	public String getBasedir() {
		return basedir;
	}

	public Boolean getTsconfig() {
		return tsconfig;
	}

	public String getTsconfigFile() {
		return tsconfigFile;
	}

	public Boolean getTslint() {
		return tslint;
	}

	public String getTslintFile() {
		return tslintFile;
	}

	public Boolean getEditorconfig() {
		return editorconfig;
	}

	public Boolean getVscode() {
		return vscode;
	}

	public String getVscodeFile() {
		return vscodeFile;
	}

	public Boolean getTsfmt() {
		return tsfmt;
	}

	public String getTsfmtFile() {
		return tsfmtFile;
	}

	public V8ObjectWrapper toV8Object(NodeJSWrapper nodeJSWrapper) {
		if (nodeJSWrapper == null) {
			throw new IllegalArgumentException("cannot work without nodeJSWrapper");
		}
		final V8ObjectWrapper v8Object = nodeJSWrapper.createNewObject()
				.add("replace", false)
				.add("verify", false);
		Optional.ofNullable(getVerbose()).ifPresent(val -> v8Object.add("verbose", val));
		Optional.ofNullable(getBasedir()).ifPresent(val -> v8Object.add("basedir", val));
		Optional.ofNullable(getTsconfig()).ifPresent(val -> v8Object.add("tsconfig", val));
		Optional.ofNullable(getTsconfigFile()).ifPresent(val -> v8Object.add("tsconfigFile", val));
		Optional.ofNullable(getTslint()).ifPresent(val -> v8Object.add("tslint", val));
		Optional.ofNullable(getTslintFile()).ifPresent(val -> v8Object.add("tslintFile", val));
		Optional.ofNullable(getEditorconfig()).ifPresent(val -> v8Object.add("editorconfig", val));
		Optional.ofNullable(getVscode()).ifPresent(val -> v8Object.add("vscode", val));
		Optional.ofNullable(getVscodeFile()).ifPresent(val -> v8Object.add("vscodeFile", val));
		Optional.ofNullable(getTsfmt()).ifPresent(val -> v8Object.add("tsfmt", val));
		Optional.ofNullable(getTsfmtFile()).ifPresent(val -> v8Object.add("tsfmtFile", val));
		return v8Object;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(TsFmtOptions copy) {
		Builder builder = new Builder();
		builder.verbose = copy.getVerbose();
		builder.basedir = copy.getBasedir();
		builder.tsconfig = copy.getTsconfig();
		builder.tsconfigFile = copy.getTsconfigFile();
		builder.tslint = copy.getTslint();
		builder.tslintFile = copy.getTslintFile();
		builder.editorconfig = copy.getEditorconfig();
		builder.vscode = copy.getVscode();
		builder.vscodeFile = copy.getVscodeFile();
		builder.tsfmt = copy.getTsfmt();
		builder.tsfmtFile = copy.getTsfmtFile();
		return builder;
	}

	public static final class Builder {
		private Boolean verbose;
		private String basedir;
		private Boolean tsconfig = false;
		private String tsconfigFile = null;
		private Boolean tslint = false;
		private String tslintFile = null;
		private Boolean editorconfig = false;
		private Boolean vscode = false;
		private String vscodeFile = null;
		private Boolean tsfmt = false;
		private String tsfmtFile = null;

		private Builder() {}

		public Builder withVerbose(Boolean verbose) {
			this.verbose = verbose;
			return this;
		}

		public Builder withBasedir(String basedir) {
			this.basedir = basedir;
			return this;
		}

		public Builder withTsconfig(Boolean tsconfig) {
			this.tsconfig = tsconfig;
			return this;
		}

		public Builder withTsconfigFile(String tsconfigFile) {
			this.tsconfigFile = tsconfigFile;
			return withTsconfig(tsconfigFile != null);
		}

		public Builder withTslint(Boolean tslint) {
			this.tslint = tslint;
			return this;
		}

		public Builder withTslintFile(String tslintFile) {
			this.tslintFile = tslintFile;
			return withTslint(tslintFile != null);
		}

		public Builder withEditorconfig(Boolean editorconfig) {
			this.editorconfig = editorconfig;
			return this;
		}

		public Builder withVscode(Boolean vscode) {
			this.vscode = vscode;
			return this;
		}

		public Builder withVscodeFile(String vscodeFile) {
			this.vscodeFile = vscodeFile;
			return withVscode(vscodeFile != null);
		}

		public Builder withTsfmt(Boolean tsfmt) {
			this.tsfmt = tsfmt;
			return this;
		}

		public Builder withTsfmtFile(String tsfmtFile) {
			this.tsfmtFile = tsfmtFile;
			return withTsfmt(tsfmtFile != null);
		}

		public TsFmtOptions build() {
			return new TsFmtOptions(this);
		}
	}

	/*
			dryRun?: Boolean;
	verbose?: Boolean; x
	baseDir?: string;
	replace: Boolean;
	verify: Boolean;
	tsconfig: Boolean;
	tsconfigFile: string | null;
	tslint: Boolean;
	tslintFile: string | null;
	editorconfig: Boolean;
	vscode: Boolean;
	vscodeFile: string | null;
	tsfmt: Boolean;
	tsfmtFile: string | null;
	 */

}
