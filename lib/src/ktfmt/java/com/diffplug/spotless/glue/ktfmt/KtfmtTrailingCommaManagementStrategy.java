/*
 * Copyright 2025 DiffPlug
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
package com.diffplug.spotless.glue.ktfmt;

import com.facebook.ktfmt.format.TrailingCommaManagementStrategy;

public enum KtfmtTrailingCommaManagementStrategy {
	NONE, ONLY_ADD, COMPLETE;

	public TrailingCommaManagementStrategy toFormatterTrailingCommaManagementStrategy() {
		return switch (this) {
			case NONE -> TrailingCommaManagementStrategy.NONE;
			case ONLY_ADD -> TrailingCommaManagementStrategy.ONLY_ADD;
			case COMPLETE -> TrailingCommaManagementStrategy.COMPLETE;
		};
	}
}
