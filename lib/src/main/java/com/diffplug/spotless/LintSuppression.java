/*
 * Copyright 2024-2025 DiffPlug
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
package com.diffplug.spotless;

import java.io.File;
import java.io.Serial;
import java.util.Objects;

import javax.annotation.Nullable;

public class LintSuppression implements java.io.Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private static final String ALL = "*";
	private String path = ALL;
	private String step = ALL;
	private String shortCode = ALL;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		if (path.indexOf('\\') != -1) {
			throw new IllegalArgumentException("Path must use only unix style path separator `/`, this was " + path);
		}
		this.path = Objects.requireNonNull(path);
	}

	public String getStep() {
		return step;
	}

	public void setStep(String step) {
		this.step = Objects.requireNonNull(step);
	}

	public String getShortCode() {
		return shortCode;
	}

	public void setShortCode(String shortCode) {
		this.shortCode = Objects.requireNonNull(shortCode);
	}

	public boolean suppresses(String relativePath, FormatterStep formatterStep, Lint lint) {
		if (path.equals(ALL) || path.equals(relativePath)) {
			if (step.equals(ALL) || formatterStep.getName().equals(this.step)) {
				if (shortCode.equals(ALL) || lint.getShortCode().equals(this.shortCode)) {
					return true;
				}
			}
		}
		return false;
	}

	public void ensureDoesNotSuppressAll() {
		boolean suppressAll = path.equals(ALL) && step.equals(ALL) && shortCode.equals(ALL);
		if (suppressAll) {
			throw new IllegalArgumentException("You must specify a specific `file`, `step`, or `shortCode`.");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		LintSuppression that = (LintSuppression) o;
		return Objects.equals(path, that.path) && Objects.equals(step, that.step) && Objects.equals(shortCode, that.shortCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(path, step, shortCode);
	}

	@Override
	public String toString() {
		return "LintSuppression{" +
				"file='" + path + '\'' +
				", step='" + step + '\'' +
				", code='" + shortCode + '\'' +
				'}';
	}

	/**
	 * Returns the relative path between root and dest, or null if dest is not a
	 * child of root. Guaranteed to only have unix-separators.
	 */
	public static @Nullable String relativizeAsUnix(File root, File dest) {
		String rootPath = root.getAbsolutePath();
		String destPath = dest.getAbsolutePath();
		if (!destPath.startsWith(rootPath)) {
			return null;
		} else {
			String relativized = destPath.substring(rootPath.length());
			String unixified = relativized.replace('\\', '/');
			return unixified.startsWith("/") ? unixified.substring(1) : unixified;
		}
	}
}
