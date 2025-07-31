/*
 * Copyright 2024 DiffPlug
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

import java.util.Objects;

public class LintSuppression implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	private static final String ALL = "*";
	private String path = ALL;
	private String step = ALL;
	private String shortCode = ALL;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
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
		if (ALL.equals(path) || path.equals(relativePath)) {
			if (ALL.equals(step) || formatterStep.getName().equals(this.step)) {
				if (ALL.equals(shortCode) || lint.getShortCode().equals(this.shortCode)) {
					return true;
				}
			}
		}
		return false;
	}

	public void ensureDoesNotSuppressAll() {
		boolean suppressAll = ALL.equals(path) && ALL.equals(step) && ALL.equals(shortCode);
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
}
