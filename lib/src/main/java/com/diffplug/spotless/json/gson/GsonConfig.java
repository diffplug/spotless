/*
 * Copyright 2023-2025 DiffPlug
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
package com.diffplug.spotless.json.gson;

import java.io.Serial;
import java.io.Serializable;

public class GsonConfig implements Serializable {
	@Serial
	private static final long serialVersionUID = 6039715618937332633L;

	private boolean sortByKeys;
	private boolean escapeHtml;
	private int indentSpaces;
	private String version;

	public GsonConfig(boolean sortByKeys, boolean escapeHtml, int indentSpaces, String version) {
		this.sortByKeys = sortByKeys;
		this.escapeHtml = escapeHtml;
		this.indentSpaces = indentSpaces;
		this.version = version;
	}

	public boolean isSortByKeys() {
		return sortByKeys;
	}

	public void setSortByKeys(boolean sortByKeys) {
		this.sortByKeys = sortByKeys;
	}

	public boolean isEscapeHtml() {
		return escapeHtml;
	}

	public void setEscapeHtml(boolean escapeHtml) {
		this.escapeHtml = escapeHtml;
	}

	public int getIndentSpaces() {
		return indentSpaces;
	}

	public void setIndentSpaces(int indentSpaces) {
		this.indentSpaces = indentSpaces;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
