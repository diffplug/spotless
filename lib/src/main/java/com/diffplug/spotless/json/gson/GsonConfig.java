package com.diffplug.spotless.json.gson;

import java.io.Serializable;

public class GsonConfig implements Serializable {
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
