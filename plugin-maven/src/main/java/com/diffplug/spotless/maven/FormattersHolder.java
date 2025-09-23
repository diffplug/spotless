/*
 * Copyright 2021-2024 DiffPlug
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
package com.diffplug.spotless.maven;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import com.diffplug.spotless.Formatter;

class FormattersHolder implements AutoCloseable {
	final Map<FormatterFactory, Formatter> openFormatters;
	final Map<FormatterFactory, Supplier<Iterable<File>>> factoryToFiles;

	FormattersHolder(Map<FormatterFactory, Formatter> openFormatters, Map<FormatterFactory, Supplier<Iterable<File>>> factoryToFiles) {
		this.openFormatters = openFormatters;
		this.factoryToFiles = factoryToFiles;
	}

	public String nameFor(FormatterFactory factory) {
		return factory.getClass().getSimpleName();
	}

	static FormattersHolder create(Map<FormatterFactory, Supplier<Iterable<File>>> formatterFactoryToFiles, FormatterConfig config) {
		Map<FormatterFactory, Formatter> openFormatters = new LinkedHashMap<>();
		try {
			for (Entry<FormatterFactory, Supplier<Iterable<File>>> entry : formatterFactoryToFiles.entrySet()) {
				FormatterFactory formatterFactory = entry.getKey();
				Supplier<Iterable<File>> files = entry.getValue();
				Formatter formatter = formatterFactory.newFormatter(files, config);
				openFormatters.put(formatterFactory, formatter);
			}
		} catch (RuntimeException openError) {
			try {
				close(openFormatters.values());
			} catch (Exception closeError) {
				openError.addSuppressed(closeError);
			}
			throw openError;
		}

		return new FormattersHolder(openFormatters, formatterFactoryToFiles);
	}

	@Override
	public void close() {
		try {
			close(openFormatters.values());
		} catch (Exception e) {
			throw new RuntimeException("Unable to close formatters", e);
		}
	}

	private static void close(Collection<Formatter> formatters) throws Exception {
		Exception error = null;
		for (Formatter formatter : formatters) {
			try {
				formatter.close();
			} catch (Exception e) {
				if (error == null) {
					error = e;
				} else {
					error.addSuppressed(e);
				}
			}
		}
		if (error != null) {
			throw error;
		}
	}
}
