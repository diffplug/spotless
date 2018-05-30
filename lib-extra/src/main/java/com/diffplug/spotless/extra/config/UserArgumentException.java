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
package com.diffplug.spotless.extra.config;

import javax.annotation.Nullable;

/**
 * Exceptions caused due to invalid user arguments.
 */
class UserArgumentException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private static final int SINGLE_LINE_MAX_VALUE_LENGTH = 30;

	/**
	 * Create argument exception.
	 * @param value Optional value (or its string representation). Value is {@code null} in case user set {@code null} or missed to set a required value.
	 * @param message Error message
	 */
	UserArgumentException(@Nullable Object value, String message) {
		super(createDescription(value, message));
	}

	/**
	 * Create argument exception.
	 * @param value Optional value (or its string representation). Value is {@code null} in case user set {@code null} or missed to set a required value.
	 * @param message Error message
	 * @param cause The original cause of this exception
	 */
	UserArgumentException(@Nullable Object value, String message, Throwable cause) {
		super(createDescription(value, message), cause);
	}

	private static String createDescription(@Nullable Object value, String message) {
		StringBuilder description = new StringBuilder();
		if (null == value) {
			description.append("Value not set or set to 'null': ");
		} else {
			String valueString = value.toString();
			if (valueString.length() < SINGLE_LINE_MAX_VALUE_LENGTH) {
				description.append(String.format("'%s': ", valueString));
			} else {
				/*
				 * In case of bigger amounts of user data, separate the data
				 * from the exception message by line breaks.
				 */
				description.append(String.format("%n'%s':%n ", valueString));
			}
		}
		description.append(message);
		return description.toString();
	}
}
