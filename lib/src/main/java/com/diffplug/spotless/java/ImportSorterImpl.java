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

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/*not thread safe*/
// Based on ImportSorterImpl from https://github.com/krasa/EclipseCodeFormatter,
// which itself is licensed under the Apache 2.0 license.
final class ImportSorterImpl {

	private static final String CATCH_ALL_SUBGROUP = "";
	private static final String STATIC_KEYWORD = "static ";
	private static final String STATIC_SYMBOL = "\\#";
	private static final String SUBGROUP_SEPARATOR = "|";

	private final List<ImportsGroup> importsGroups;
	private final Map<String, List<String>> matchingImports = new HashMap<>();
	private final List<String> notMatching = new ArrayList<>();
	private final Set<String> allImportOrderItems = new HashSet<>();
	private final Comparator<String> ordering;

	// An ImportsGroup is a group of imports ; each group is separated by blank lines.
	// A group is composed of subgroups : imports are sorted by subgroup.
	private static class ImportsGroup {

		private final List<String> subGroups;

		public ImportsGroup(String importOrder) {
			this.subGroups = Stream.of(importOrder.split("\\" + SUBGROUP_SEPARATOR, -1))
					.map(this::normalizeStatic)
					.collect(Collectors.toList());
		}

		private String normalizeStatic(String subgroup) {
			if (subgroup.startsWith(STATIC_SYMBOL)) {
				return subgroup.replace(STATIC_SYMBOL, STATIC_KEYWORD);
			}
			return subgroup;
		}

		public List<String> getSubGroups() {
			return subGroups;
		}
	}

	static List<String> sort(List<String> imports, List<String> importsOrder, boolean wildcardsLast, String lineFormat) {
		ImportSorterImpl importsSorter = new ImportSorterImpl(importsOrder, wildcardsLast);
		return importsSorter.sort(imports, lineFormat);
	}

	private List<String> sort(List<String> imports, String lineFormat) {
		filterMatchingImports(imports);
		mergeNotMatchingItems(false);
		mergeNotMatchingItems(true);
		List<String> sortedImported = mergeMatchingItems();

		return getResult(sortedImported, lineFormat);
	}

	private ImportSorterImpl(List<String> importOrder, boolean wildcardsLast) {
		importsGroups = importOrder.stream().filter(Objects::nonNull).map(ImportsGroup::new).collect(Collectors.toList());
		putStaticItemIfNotExists(importsGroups);
		putCatchAllGroupIfNotExists(importsGroups);

		ordering = new OrderingComparator(wildcardsLast);

		List<String> subgroups = importsGroups.stream().map(ImportsGroup::getSubGroups).flatMap(Collection::stream).collect(Collectors.toList());
		this.allImportOrderItems.addAll(subgroups);
	}

	private void putStaticItemIfNotExists(List<ImportsGroup> importsGroups) {
		boolean catchAllSubGroupExist = importsGroups.stream().anyMatch(group -> group.getSubGroups().contains(STATIC_KEYWORD));
		if (catchAllSubGroupExist) {
			return;
		}

		int indexOfFirstStatic = 0;
		for (int i = 0; i < importsGroups.size(); i++) {
			boolean subgroupMatch = importsGroups.get(i).getSubGroups().stream().anyMatch(subgroup -> subgroup.startsWith(STATIC_KEYWORD));
			if (subgroupMatch) {
				indexOfFirstStatic = i;
			}
		}
		importsGroups.add(indexOfFirstStatic, new ImportsGroup(STATIC_KEYWORD));
	}

	private void putCatchAllGroupIfNotExists(List<ImportsGroup> importsGroups) {
		boolean catchAllSubGroupExist = importsGroups.stream().anyMatch(group -> group.getSubGroups().contains(CATCH_ALL_SUBGROUP));
		if (!catchAllSubGroupExist) {
			importsGroups.add(new ImportsGroup(CATCH_ALL_SUBGROUP));
		}
	}

	/**
	 * returns not matching items and initializes internal state
	 */
	private void filterMatchingImports(List<String> imports) {
		for (String anImport : imports) {
			String orderItem = getBestMatchingImportOrderItem(anImport);
			if (orderItem != null) {
				matchingImports.computeIfAbsent(orderItem, key -> new ArrayList<>());
				matchingImports.get(orderItem).add(anImport);
			} else {
				notMatching.add(anImport);
			}
		}
		notMatching.addAll(allImportOrderItems);
	}

	private @Nullable String getBestMatchingImportOrderItem(String anImport) {
		String matchingImport = null;
		for (String orderItem : allImportOrderItems) {
			if (anImport.startsWith(orderItem)) {
				if (matchingImport == null) {
					matchingImport = orderItem;
				} else {
					matchingImport = betterMatching(matchingImport, orderItem, anImport);
				}
			}
		}
		return matchingImport;
	}

	/**
	 * not matching means it does not match any order item, so it will be appended before or after order items
	 */
	private void mergeNotMatchingItems(boolean staticItems) {
		for (String notMatchingItem : notMatching) {
			if (!matchesStatic(staticItems, notMatchingItem)) {
				continue;
			}
			boolean isOrderItem = isOrderItem(notMatchingItem, staticItems);
			if (!isOrderItem) {
				matchingImports.computeIfAbsent(CATCH_ALL_SUBGROUP, key -> new ArrayList<>());
				matchingImports.get(CATCH_ALL_SUBGROUP).add(notMatchingItem);
			}
		}
	}

	private boolean isOrderItem(String notMatchingItem, boolean staticItems) {
		boolean contains = allImportOrderItems.contains(notMatchingItem);
		return contains && matchesStatic(staticItems, notMatchingItem);
	}

	private static boolean matchesStatic(boolean staticItems, String notMatchingItem) {
		boolean isStatic = notMatchingItem.startsWith(STATIC_KEYWORD);
		return (isStatic && staticItems) || (!isStatic && !staticItems);
	}

	private List<String> mergeMatchingItems() {
		List<String> template = new ArrayList<>();
		for (ImportsGroup group : importsGroups) {
			boolean groupIsNotEmpty = false;
			for (String subgroup : group.getSubGroups()) {
				List<String> strings = matchingImports.get(subgroup);
				if (strings == null || strings.isEmpty()) {
					continue;
				}
				groupIsNotEmpty = true;
				List<String> matchingItems = new ArrayList<>(strings);
				sort(matchingItems);
				template.addAll(matchingItems);
			}
			if (groupIsNotEmpty) {
				template.add(ImportSorter.N);
			}
		}
		// if there is \n on the end, remove it
		if (!template.isEmpty() && template.get(template.size() - 1).equals(ImportSorter.N)) {
			template.remove(template.size() - 1);
		}
		return template;
	}

	private void sort(List<String> items) {
		items.sort(ordering);
	}

	private List<String> getResult(List<String> sortedImported, String lineFormat) {
		List<String> strings = new ArrayList<>();

		for (String s : sortedImported) {
			if (s.equals(ImportSorter.N)) {
				strings.add(s);
			} else {
				strings.add(String.format(lineFormat, s) + ImportSorter.N);
			}
		}
		return strings;
	}

	private static @Nullable String betterMatching(String order1, String order2, String anImport) {
		if (order1.equals(order2)) {
			throw new IllegalArgumentException("orders are same");
		}
		for (int i = 0; i < anImport.length() - 1; i++) {
			if (order1.length() - 1 == i && order2.length() - 1 != i) {
				return order2;
			}
			if (order2.length() - 1 == i && order1.length() - 1 != i) {
				return order1;
			}
			char orderChar1 = order1.length() != 0 ? order1.charAt(i) : ' ';
			char orderChar2 = order2.length() != 0 ? order2.charAt(i) : ' ';
			char importChar = anImport.charAt(i);

			if (importChar == orderChar1 && importChar != orderChar2) {
				return order1;
			} else if (importChar != orderChar1 && importChar == orderChar2) {
				return order2;
			}

		}
		return null;
	}

	private static class OrderingComparator implements Comparator<String>, Serializable {
		private static final long serialVersionUID = 1;

		private final boolean wildcardsLast;

		private OrderingComparator(boolean wildcardsLast) {
			this.wildcardsLast = wildcardsLast;
		}

		@Override
		public int compare(String string1, String string2) {
			int string1WildcardIndex = string1.indexOf('*');
			int string2WildcardIndex = string2.indexOf('*');
			boolean string1IsWildcard = string1WildcardIndex >= 0;
			boolean string2IsWildcard = string2WildcardIndex >= 0;
			if (string1IsWildcard == string2IsWildcard) {
				return string1.compareTo(string2);
			}
			int prefixLength = string1IsWildcard ? string1WildcardIndex : string2WildcardIndex;
			boolean samePrefix = string1.regionMatches(0, string2, 0, prefixLength);
			if (!samePrefix) {
				return string1.compareTo(string2);
			}
			return (string1IsWildcard == wildcardsLast) ? 1 : -1;
		}
	}
}
