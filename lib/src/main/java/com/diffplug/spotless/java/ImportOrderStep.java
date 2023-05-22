/*
 * Copyright 2016-2023 DiffPlug
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
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public final class ImportOrderStep {
	private static final boolean WILDCARDS_LAST_DEFAULT = false;
	private static final boolean SEMANTIC_SORT_DEFAULT = false;
	private static final Set<String> TREAT_AS_PACKAGE_DEFAULT = Set.of();
	private static final Set<String> TREAT_AS_CLASS_DEFAULT = Set.of();

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
		return createFrom(WILDCARDS_LAST_DEFAULT, SEMANTIC_SORT_DEFAULT, TREAT_AS_PACKAGE_DEFAULT,
				TREAT_AS_CLASS_DEFAULT, importOrder);
	}

	public FormatterStep createFrom(boolean wildcardsLast, boolean semanticSort, Set<String> treatAsPackage,
			Set<String> treatAsClass, String... importOrder) {
		// defensive copying and null checking
		List<String> importOrderList = requireElementsNonNull(Arrays.asList(importOrder));
		return createFrom(wildcardsLast, semanticSort, treatAsPackage, treatAsClass, () -> importOrderList);
	}

	public FormatterStep createFrom(File importsFile) {
		return createFrom(WILDCARDS_LAST_DEFAULT, SEMANTIC_SORT_DEFAULT, TREAT_AS_PACKAGE_DEFAULT,
				TREAT_AS_CLASS_DEFAULT, importsFile);
	}

	public FormatterStep createFrom(boolean wildcardsLast, boolean semanticSort, Set<String> treatAsPackage,
			Set<String> treatAsClass, File importsFile) {
		Objects.requireNonNull(importsFile);
		return createFrom(wildcardsLast, semanticSort, treatAsPackage, treatAsClass, () -> getImportOrder(importsFile));
	}

	private FormatterStep createFrom(boolean wildcardsLast, boolean semanticSort, Set<String> treatAsPackage,
			Set<String> treatAsClass, Supplier<List<String>> importOrder) {
		return FormatterStep.createLazy("importOrder",
				() -> new State(importOrder.get(), lineFormat, wildcardsLast, semanticSort, treatAsPackage,
						treatAsClass),
				State::toFormatter);
	}

	@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE") // workaround https://github.com/spotbugs/spotbugs/issues/756
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
		private final boolean wildcardsLast;
		private final boolean semanticSort;
		private final TreeSet<String> treatAsPackage;
		private final TreeSet<String> treatAsClass;

		State(List<String> importOrder, String lineFormat, boolean wildcardsLast, boolean semanticSort,
				Set<String> treatAsPackage, Set<String> treatAsClass) {
			this.importOrder = importOrder;
			this.lineFormat = lineFormat;
			this.wildcardsLast = wildcardsLast;
			this.semanticSort = semanticSort;
			this.treatAsPackage = treatAsPackage == null ? null : new TreeSet<>(treatAsPackage);
			this.treatAsClass = treatAsClass == null ? null : new TreeSet<>(treatAsClass);
		}

		FormatterFunc toFormatter() {
			return raw -> new ImportSorter(importOrder, wildcardsLast, semanticSort, treatAsPackage, treatAsClass)
					.format(raw, lineFormat);
		}
	}
}
