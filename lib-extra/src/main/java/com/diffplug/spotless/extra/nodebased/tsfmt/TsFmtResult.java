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
package com.diffplug.spotless.extra.nodebased.tsfmt;

public class TsFmtResult {

	private final String message;
	private final Boolean error;
	private final String formatted;

	public TsFmtResult(String message, Boolean error, String formatted) {
		this.message = message;
		this.error = error;
		this.formatted = formatted;
	}

	public String getMessage() {
		return message;
	}

	public Boolean isError() {
		return error;
	}

	public String getFormatted() {
		return formatted;
	}
}
