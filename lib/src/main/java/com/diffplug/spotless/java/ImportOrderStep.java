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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;

public final class ImportOrderStep {
	private final String lineFormat;

	public static ImportOrderStep forGroovy() {
		return new ImportOrderStep("import %s");
	}

	public static ImportOrderStep forJava() {
		return new ImportOrderStep("import %s;");
	}

	private ImportOrderStep(String lineFormat) {
		this.lineFormat = lineFormat;
	}

	public FormatterStep createFrom(String... importOrder) {
		// defensive copying and null checking
		List<String> importOrderList = requireElementsNonNull(Arrays.asList(importOrder));
		return createFrom(() -> importOrderList);
	}

	public FormatterStep createFrom(File importsFile) {
		Objects.requireNonNull(importsFile);
		return createFrom(() -> getImportOrder(importsFile));
	}

	private FormatterStep createFrom(Supplier<List<String>> importOrder) {
		return FormatterStep.createLazy("importOrder",
				() -> new State(importOrder.get(), lineFormat),
				State::toFormatter);
	}

	/** Method interface has been changed to
	 * {@link ImportOrderStep#createFromOrder(String...)}.*/
	@Deprecated
	public static FormatterStep createFromOrder(List<String> importOrder) {
		// defensive copying and null checking
		List<String> importOrderCopy = requireElementsNonNull(new ArrayList<>(importOrder));
		return forJava().createFrom(() -> importOrderCopy);
	}

	/** Static method has been replaced by instance method
	 * {@link ImportOrderStep#createFrom(String...)}.*/
	@Deprecated
	public static FormatterStep createFromOrder(String... importOrder) {
		return forJava().createFrom(importOrder);
	}

	/** Static method has been replaced by instance method
	 * {@link ImportOrderStep#createFrom(File)}.*/
	@Deprecated
	public static FormatterStep createFromFile(File importsFile) {
		return forJava().createFrom(importsFile);
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
		private final String lineFormat;

		State(List<String> importOrder, String lineFormat) {
			this.importOrder = importOrder;
			this.lineFormat = lineFormat;
		}

		FormatterFunc toFormatter() {
			return raw -> new ImportSorter(importOrder).format(raw, lineFormat);
		}
	}
}
