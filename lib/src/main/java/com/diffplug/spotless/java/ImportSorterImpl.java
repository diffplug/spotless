/*
 * Copyright 2016-2025 DiffPlug
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

import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
	private final Set<String> knownGroupings = new HashSet<>();
	private final Map<String, List<String>> matchingImports = new HashMap<>();
	private final List<String> notMatching = new ArrayList<>();
	private final Set<String> allImportOrderItems = new HashSet<>();
	private final Comparator<String> ordering;

	// An ImportsGroup is a group of imports ; each group is separated by blank lines.
	// A group is composed of subgroups : imports are sorted by subgroup.
	private static class ImportsGroup {

		private final List<String> subGroups;

		public ImportsGroup(String importOrder, Set<String> knownGroupings) {
			this.subGroups = Stream.of(importOrder.split("\\" + SUBGROUP_SEPARATOR, -1))
					.map(this::normalizeStatic)
					.filter(group -> !knownGroupings.contains(group))
					.collect(toList());
			knownGroupings.addAll(this.subGroups);
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

	static List<String> sort(List<String> imports, List<String> importsOrder, boolean wildcardsLast,
			boolean semanticSort, Set<String> treatAsPackage, Set<String> treatAsClass, String lineFormat) {
		ImportSorterImpl importsSorter = new ImportSorterImpl(importsOrder, wildcardsLast, semanticSort, treatAsPackage,
				treatAsClass);
		return importsSorter.sort(imports, lineFormat);
	}

	private List<String> sort(List<String> imports, String lineFormat) {
		filterMatchingImports(imports);
		mergeNotMatchingItems(false);
		mergeNotMatchingItems(true);
		List<String> sortedImported = mergeMatchingItems();

		return getResult(sortedImported, lineFormat);
	}

	private ImportSorterImpl(List<String> importOrder, boolean wildcardsLast, boolean semanticSort,
			Set<String> treatAsPackage, Set<String> treatAsClass) {
		importsGroups = importOrder.stream().filter(Objects::nonNull).map(order -> new ImportsGroup(order, knownGroupings)).collect(toList());
		putStaticItemIfNotExists(importsGroups);
		putCatchAllGroupIfNotExists(importsGroups);

		if (semanticSort) {
			ordering = new SemanticOrderingComparator(wildcardsLast, treatAsPackage, treatAsClass);
		} else {
			ordering = new LexicographicalOrderingComparator(wildcardsLast);
		}

		List<String> subgroups = importsGroups.stream().map(ImportsGroup::getSubGroups).flatMap(Collection::stream).collect(toList());
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
		importsGroups.add(indexOfFirstStatic, new ImportsGroup(STATIC_KEYWORD, this.knownGroupings));
	}

	private void putCatchAllGroupIfNotExists(List<ImportsGroup> importsGroups) {
		boolean catchAllSubGroupExist = importsGroups.stream().anyMatch(group -> group.getSubGroups().contains(CATCH_ALL_SUBGROUP));
		if (!catchAllSubGroupExist) {
			importsGroups.add(new ImportsGroup(CATCH_ALL_SUBGROUP, this.knownGroupings));
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
		if (!template.isEmpty() && ImportSorter.N.equals(template.get(template.size() - 1))) {
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
			if (ImportSorter.N.equals(s)) {
				strings.add(s);
			} else {
				strings.add(lineFormat.formatted(s) + ImportSorter.N);
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

	private static int compareWithWildcare(String string1, String string2, boolean wildcardsLast) {
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
		return string1IsWildcard == wildcardsLast ? 1 : -1;
	}

	private static final class LexicographicalOrderingComparator implements Comparator<String>, Serializable {
		private static final long serialVersionUID = 1;

		private final boolean wildcardsLast;

		private LexicographicalOrderingComparator(boolean wildcardsLast) {
			this.wildcardsLast = wildcardsLast;
		}

		@Override
		public int compare(String string1, String string2) {
			return compareWithWildcare(string1, string2, wildcardsLast);
		}
	}

	private static final class SemanticOrderingComparator implements Comparator<String>, Serializable {
		private static final long serialVersionUID = 1;

		private final boolean wildcardsLast;
		private final Set<String> treatAsPackage;
		private final Set<String> treatAsClass;

		private SemanticOrderingComparator(boolean wildcardsLast, Set<String> treatAsPackage,
				Set<String> treatAsClass) {
			this.wildcardsLast = wildcardsLast;
			this.treatAsPackage = treatAsPackage;
			this.treatAsClass = treatAsClass;
		}

		@Override
		public int compare(String string1, String string2) {
			/*
			 * Ordering uses semantics of the import string by splitting it into package,
			 * class name(s) and static member (for static imports) and then comparing by
			 * each of those three substrings in sequence.
			 *
			 * When comparing static imports, the last segment in the dot-separated string
			 * is considered to be the member (field, method, type) name.
			 *
			 * The first segment starting with an upper case letter is considered to be the
			 * (first) class name. Since this comparator has no actual type information,
			 * this auto-detection will fail for upper case package names and lower case
			 * class names. treatAsPackage and treatAsClass can be used respectively to
			 * provide hints to the auto-detection.
			 */
			if (string1.startsWith(STATIC_KEYWORD)) {
				String[] split = splitFqcnAndMember(string1);
				String fqcn1 = split[0];
				String member1 = split[1];

				split = splitFqcnAndMember(string2);
				String fqcn2 = split[0];
				String member2 = split[1];

				int result = compareFullyQualifiedClassName(fqcn1, fqcn2);
				if (result != 0) {
					return result;
				}

				return compareWithWildcare(member1, member2, wildcardsLast);
			} else {
				return compareFullyQualifiedClassName(string1, string2);
			}
		}

		/**
		 * Compares two fully qualified class names by splitting them into package and
		 * (nested) class names.
		 */
		private int compareFullyQualifiedClassName(String fqcn1, String fqcn2) {
			String[] split = splitPackageAndClasses(fqcn1);
			String p1 = split[0];
			String c1 = split[1];

			split = splitPackageAndClasses(fqcn2);
			String p2 = split[0];
			String c2 = split[1];

			int result = p1.compareTo(p2);
			if (result != 0) {
				return result;
			}

			return compareWithWildcare(c1, c2, wildcardsLast);
		}

		/**
		 * Splits the provided static import string into fully qualified class name and
		 * the imported static member (field, method or type).
		 */
		private String[] splitFqcnAndMember(String importString) {
			String s = importString.substring(STATIC_KEYWORD.length()).trim();

			/*
			 * Static imports always contain a member or wildcard and it's always the last
			 * segment.
			 */
			int dot = s.lastIndexOf(".");
			String fqcn = s.substring(0, dot);
			String member = s.substring(dot + 1);
			return new String[]{fqcn, member};
		}

		/**
		 * Splits the fully qualified class name into package and class name(s).
		 */
		private String[] splitPackageAndClasses(String fqcn) {
			String packageNames = null;
			String classNames = null;

			/*
			 * The first segment that starts with an upper case letter starts the class
			 * name(s), unless it matches treatAsPackage (then it's explicitly declared as
			 * package via configuration). If no segment starts with an upper case letter
			 * then the last segment must be a class name (unless the method input is
			 * garbage).
			 */
			int dot = fqcn.indexOf('.');
			while (dot > -1) {
				int nextDot = fqcn.indexOf('.', dot + 1);
				if (nextDot > -1) {
					if (Character.isUpperCase(fqcn.charAt(dot + 1))) {
						// if upper case, check if should be treated as package nonetheless
						if (!treatAsPackage(fqcn.substring(0, nextDot))) {
							packageNames = fqcn.substring(0, dot);
							classNames = fqcn.substring(dot + 1);
							break;
						}
					} else {
						// if lower case, check if should be treated as class nonetheless
						if (treatAsClass(fqcn.substring(0, nextDot))) {
							packageNames = fqcn.substring(0, dot);
							classNames = fqcn.substring(dot + 1);
							break;
						}
					}
				}

				dot = nextDot;
			}

			if (packageNames == null) {
				int i = fqcn.lastIndexOf(".");
				packageNames = fqcn.substring(0, i);
				classNames = fqcn.substring(i + 1);
			}

			return new String[]{packageNames, classNames};
		}

		/**
		 * Returns whether the provided prefix matches any entry of
		 * {@code treatAsPackage}.
		 */
		private boolean treatAsPackage(String prefix) {
			// This would be the place to introduce wild cards or even regex matching.
			return treatAsPackage != null && treatAsPackage.contains(prefix);
		}

		/**
		 * Returns whether the provided prefix name matches any entry of
		 * {@code treatAsClass}.
		 */
		private boolean treatAsClass(String prefix) {
			// This would be the place to introduce wild cards or even regex matching.
			return treatAsClass != null && treatAsClass.contains(prefix);
		}

	}
}
