/*
 * Copyright 2016-2024 DiffPlug
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
package com.diffplug.spotless.sql;

import java.io.File;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterProperties;
import com.diffplug.spotless.FormatterStep;

/** SQL formatter step which wraps up DBeaver's SqlTokenizedFormatter implementation. */
public class DBeaverSQLFormatterStep {

	private static final String NAME = "dbeaverSql";

	// prevent direct instantiation
	private DBeaverSQLFormatterStep() {}

	public static FormatterStep create(Iterable<File> files) {
		return FormatterStep.create(NAME, FileSignature.promise(files),
				FileSignature.Promised::get,
				DBeaverSQLFormatterStep::createFormat);
	}

	private static FormatterFunc createFormat(FileSignature settings) {
		FormatterProperties preferences = FormatterProperties.from(settings.files());
		DBeaverSQLFormatter dbeaverSqlFormatter = new DBeaverSQLFormatter(preferences.getProperties());
		return dbeaverSqlFormatter::format;
	}
}
