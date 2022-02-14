/*
 * Copyright 2016-2022 DiffPlug
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
package com.diffplug.gradle.spotless;

import javax.inject.Inject;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.json.JsonSimpleStep;
import com.diffplug.spotless.json.gson.GsonStep;

public class JsonExtension extends FormatExtension {
	private static final int DEFAULT_INDENTATION = 4;
	private static final String DEFAULT_GSON_VERSION = "2.8.9";
	static final String NAME = "json";

	@Inject
	public JsonExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			throw noDefaultTargetException();
		}
		super.setupTask(task);
	}

	public SimpleConfig simple() {
		return new SimpleConfig(DEFAULT_INDENTATION);
	}

	public GsonConfig gson() {
		return new GsonConfig();
	}

	public class SimpleConfig {
		private int indent;

		public SimpleConfig(int indent) {
			this.indent = indent;
			addStep(createStep());
		}

		public void indentWithSpaces(int indent) {
			this.indent = indent;
			replaceStep(createStep());
		}

		private FormatterStep createStep() {
			return JsonSimpleStep.create(indent, provisioner());
		}
	}

	public class GsonConfig {
		private int indentSpaces;
		private boolean sortByKeys;
		private boolean escapeHtml;
		private String version;

		public GsonConfig() {
			this.indentSpaces = DEFAULT_INDENTATION;
			this.sortByKeys = false;
			this.escapeHtml = false;
			this.version = DEFAULT_GSON_VERSION;
			addStep(createStep());
		}

		public GsonConfig indentWithSpaces(int indentSpaces) {
			this.indentSpaces = indentSpaces;
			replaceStep(createStep());
			return this;
		}

		public GsonConfig sortByKeys() {
			this.sortByKeys = true;
			replaceStep(createStep());
			return this;
		}

		public GsonConfig escapeHtml() {
			this.escapeHtml = true;
			replaceStep(createStep());
			return this;
		}

		public GsonConfig version(String version) {
			this.version = version;
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return GsonStep.create(indentSpaces, sortByKeys, escapeHtml, version, provisioner());
		}
	}

}
