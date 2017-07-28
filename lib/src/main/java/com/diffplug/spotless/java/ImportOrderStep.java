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
package com.diffplug.spotless.java;

import static com.diffplug.spotless.java.LibJavaPreconditions.requireElementsNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;

public final class ImportOrderStep {
	// prevent direct instantiation
	private ImportOrderStep() {}

	/** Method interface has been changed to
	 * {@link ImportOrderStep#importOrder(String...)}.*/
	@Deprecated
	public static FormatterStep createFromOrder(List<String> importOrder) {
		// defensive copying and null checking
		importOrder = requireElementsNonNull(new ArrayList<>(importOrder));
		return createFromOrderImpl(importOrder);
	}

	public static FormatterStep createFromOrder(String... importOrder) {
		// defensive copying and null checking
		List<String> importOrderList = requireElementsNonNull(Arrays.asList(importOrder));
		return createFromOrderImpl(importOrderList);
	}

	public static FormatterStep createFromFile(File importsFile) {
		Objects.requireNonNull(importsFile);
		return createFromOrderImpl(getImportOrder(importsFile));
	}

	private static FormatterStep createFromOrderImpl(List<String> importOrder) {
		return FormatterStep.createLazy("importOrder",
				() -> new State(importOrder),
				State::toFormatter);
	}

	private static List<String> getImportOrder(File importsFile) {
		try (Stream<String> lines = Files.lines(importsFile.toPath())) {
			return lines.filter(line -> !line.startsWith("#"))
					// parse 0=input
					.map(ImportOrderStep::splitIntoIndexAndName)
					.sorted(Map.Entry.comparingByKey())
					.map(Map.Entry::getValue)
					.collect(Collectors.toCollection(ArrayList::new));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Map.Entry<Integer, String> splitIntoIndexAndName(String line) {
		String[] pieces = line.split("=");
		Integer index = Integer.valueOf(pieces[0]);
		String name = pieces.length == 2 ? pieces[1] : "";
		return new SimpleImmutableEntry<>(index, name);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final List<String> importOrder;

		State(List<String> importOrder) {
			this.importOrder = importOrder;
		}

		FormatterFunc toFormatter() {
			return raw -> new ImportSorter(importOrder).format(raw);
		}
	}
}
