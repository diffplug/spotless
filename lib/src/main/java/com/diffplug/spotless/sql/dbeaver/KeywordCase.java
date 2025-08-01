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
package com.diffplug.spotless.sql.dbeaver;

import java.util.Locale;

/**
 * @author Baptiste Mesta.
 */
enum KeywordCase {
	UPPER {
		@Override
		public String transform(String value) {
			return value.toUpperCase(Locale.ENGLISH);
		}
	},
	LOWER {
		@Override
		public String transform(String value) {
			return value.toLowerCase(Locale.ENGLISH);
		}
	},
	ORIGINAL {
		@Override
		public String transform(String value) {
			return value;
		}
	};

	public abstract String transform(String value);
}
