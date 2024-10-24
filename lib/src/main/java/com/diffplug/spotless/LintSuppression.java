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
	private String file = ALL;
	private String step = ALL;
	private String shortCode = ALL;

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = Objects.requireNonNull(file);
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
		if (file.equals(ALL) || file.equals(relativePath)) {
			if (step.equals(ALL) || formatterStep.getName().equals(this.step)) {
				if (shortCode.equals(ALL) || lint.getRuleId().equals(this.shortCode)) {
					return true;
				}
			}
		}
		return false;
	}

	public void ensureDoesNotSuppressAll() {
		boolean suppressAll = file.equals(ALL) && step.equals(ALL) && shortCode.equals(ALL);
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
		return Objects.equals(file, that.file) && Objects.equals(step, that.step) && Objects.equals(shortCode, that.shortCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(file, step, shortCode);
	}

	@Override
	public String toString() {
		return "LintSuppression{" +
				"file='" + file + '\'' +
				", step='" + step + '\'' +
				", code='" + shortCode + '\'' +
				'}';
	}
}
