/*
 * Copyright 2024-2025 DiffPlug
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
package com.diffplug.spotless.extra.glue.jdt;

import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.util.CompilationUnitSorter;
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener;

/**
 * This class is derived and adapted code from the Eclipse JDT project (Derivative Works according to EPL 2.0 license).
 */
@SuppressFBWarnings(value = "SE_COMPARATOR_SHOULD_BE_SERIALIZABLE", justification = "this comparator is not meant to be serialized")
class DefaultJavaElementComparator implements Comparator<BodyDeclaration> {

	static final int TYPE_INDEX = 0;
	static final int CONSTRUCTORS_INDEX = 1;
	static final int METHOD_INDEX = 2;
	static final int FIELDS_INDEX = 3;
	static final int INIT_INDEX = 4;
	static final int STATIC_FIELDS_INDEX = 5;
	static final int STATIC_INIT_INDEX = 6;
	static final int STATIC_METHODS_INDEX = 7;
	static final int ENUM_CONSTANTS_INDEX = 8;
	static final int N_CATEGORIES = 9;

	static final int PUBLIC_INDEX = 0;
	static final int PRIVATE_INDEX = 1;
	static final int PROTECTED_INDEX = 2;
	static final int DEFAULT_INDEX = 3;
	static final int N_VISIBILITIES = 4;

	private final boolean doNotSortFields;
	private final int[] memberCategoryOffsets;
	private final boolean sortByVisibility;
	private final int[] visibilityOffsets;

	static DefaultJavaElementComparator of(
			boolean doNotSortFields,
			String memberCategoryPreferences,
			boolean sortByVisibility,
			String visibilityPreferences) {

		int[] memberCategoryOffsets = new int[9];
		boolean success = fillMemberCategoryOffsets(memberCategoryPreferences, memberCategoryOffsets);
		if (!success) {
			String defaultValue = "T,SF,SI,SM,F,I,C,M";
			fillMemberCategoryOffsets(defaultValue, memberCategoryOffsets);
		}

		int[] visibilityOffsets = new int[4];
		boolean success2 = fillVisibilityOffsets(visibilityPreferences, visibilityOffsets);
		if (!success2) {
			String defaultValue = "B,V,R,D";
			fillVisibilityOffsets(defaultValue, visibilityOffsets);
		}

		return new DefaultJavaElementComparator(doNotSortFields, memberCategoryOffsets, sortByVisibility, visibilityOffsets);
	}

	DefaultJavaElementComparator(
			boolean doNotSortFields,
			int[] memberCategoryOffsets,
			boolean sortByVisibility,
			int[] visibilityOffsets) {

		this.doNotSortFields = doNotSortFields;
		this.memberCategoryOffsets = memberCategoryOffsets;
		this.sortByVisibility = sortByVisibility;
		this.visibilityOffsets = visibilityOffsets;
	}

	@SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT", justification = "we only accept valid tokens in the order string, otherwise we fall back to default value")
	static boolean fillVisibilityOffsets(String preferencesString, int[] offsets) {
		StringTokenizer tokenizer = new StringTokenizer(preferencesString, ",");
		int i = 0;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			switch (token) {
			case "B":
				offsets[PUBLIC_INDEX] = i++;
				break;
			case "D":
				offsets[DEFAULT_INDEX] = i++;
				break;
			case "R":
				offsets[PROTECTED_INDEX] = i++;
				break;
			case "V":
				offsets[PRIVATE_INDEX] = i++;
			}
		}
		return i == N_VISIBILITIES;
	}

	@SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT", justification = "we only accept valid tokens in the order string, otherwise we fall back to default value")
	static boolean fillMemberCategoryOffsets(String orderString, int[] offsets) {
		StringTokenizer tokenizer = new StringTokenizer(orderString, ",");
		int i = 0;
		offsets[8] = i++;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			switch (token) {
			case "C":
				offsets[CONSTRUCTORS_INDEX] = i++;
				break;
			case "F":
				offsets[FIELDS_INDEX] = i++;
				break;
			case "I":
				offsets[INIT_INDEX] = i++;
				break;
			case "M":
				offsets[METHOD_INDEX] = i++;
				break;
			case "T":
				offsets[TYPE_INDEX] = i++;
				break;
			case "SF":
				offsets[STATIC_FIELDS_INDEX] = i++;
				break;
			case "SI":
				offsets[STATIC_INIT_INDEX] = i++;
				break;
			case "SM":
				offsets[STATIC_METHODS_INDEX] = i++;
			}
		}
		return i == N_CATEGORIES;
	}

	private int category(BodyDeclaration bodyDeclaration) {
		switch (bodyDeclaration.getNodeType()) {
		case ASTNode.METHOD_DECLARATION: {
			MethodDeclaration method = (MethodDeclaration) bodyDeclaration;
			if (method.isConstructor()) {
				return CONSTRUCTORS_INDEX;
			}
			int flags = method.getModifiers();
			if (Modifier.isStatic(flags))
				return STATIC_METHODS_INDEX;
			else
				return METHOD_INDEX;
		}
		case ASTNode.FIELD_DECLARATION: {
			if (JdtFlags.isStatic(bodyDeclaration))
				return STATIC_FIELDS_INDEX;
			else
				return FIELDS_INDEX;
		}
		case ASTNode.INITIALIZER: {
			int flags = bodyDeclaration.getModifiers();
			if (Modifier.isStatic(flags))
				return STATIC_INIT_INDEX;
			else
				return INIT_INDEX;
		}
		case ASTNode.TYPE_DECLARATION:
		case ASTNode.ENUM_DECLARATION:
		case ASTNode.ANNOTATION_TYPE_DECLARATION:
			return TYPE_INDEX;
		case ASTNode.ENUM_CONSTANT_DECLARATION:
			return ENUM_CONSTANTS_INDEX;
		case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
			return METHOD_INDEX; // reusing the method index

		}
		return 0; // should never happen
	}

	private int getCategoryIndex(int category) {
		return memberCategoryOffsets[category];
	}

	private int getVisibilityIndex(int modifierFlags) {
		int kind = 3;
		if (Flags.isPublic(modifierFlags)) {
			kind = 0;
		} else if (Flags.isProtected(modifierFlags)) {
			kind = 2;
		} else if (Flags.isPrivate(modifierFlags)) {
			kind = 1;
		}
		return this.visibilityOffsets[kind];
	}

	/**
	 * This comparator follows the contract defined in CompilationUnitSorter.sort.
	 *
	 * @see Comparator#compare(java.lang.Object, java.lang.Object)
	 * @see CompilationUnitSorter#sort(int, org.eclipse.jdt.core.ICompilationUnit, int[], java.util.Comparator, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	@SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST", justification = "when switching to a more recent Java version, we can avoid those unconfirmed casts")
	public int compare(BodyDeclaration bodyDeclaration1, BodyDeclaration bodyDeclaration2) {
		boolean preserved1 = doNotSortFields && isSortPreserved(bodyDeclaration1);
		boolean preserved2 = doNotSortFields && isSortPreserved(bodyDeclaration2);

		// Bug 407759: need to use a common category for all isSortPreserved members that are to be sorted in the same group:
		int cat1 = category(bodyDeclaration1);
		if (preserved1) {
			cat1 = sortPreservedCategory(cat1);
		}
		int cat2 = category(bodyDeclaration2);
		if (preserved2) {
			cat2 = sortPreservedCategory(cat2);
		}

		if (cat1 != cat2) {
			return getCategoryIndex(cat1) - getCategoryIndex(cat2);
		}

		// cat1 == cat2 implies preserved1 == preserved2

		if (preserved1) {
			return preserveRelativeOrder(bodyDeclaration1, bodyDeclaration2);
		}

		if (sortByVisibility) {
			int flags1 = JdtFlags.getVisibilityCode(bodyDeclaration1);
			int flags2 = JdtFlags.getVisibilityCode(bodyDeclaration2);
			int vis = getVisibilityIndex(flags1) - getVisibilityIndex(flags2);
			if (vis != 0) {
				return vis;
			}
		}

		switch (bodyDeclaration1.getNodeType()) {
		case ASTNode.METHOD_DECLARATION: {
			MethodDeclaration method1 = (MethodDeclaration) bodyDeclaration1;
			MethodDeclaration method2 = (MethodDeclaration) bodyDeclaration2;

			if (sortByVisibility) {
				int vis = getVisibilityIndex(method1.getModifiers()) - getVisibilityIndex(method2.getModifiers());
				if (vis != 0) {
					return vis;
				}
			}

			String name1 = method1.getName().getIdentifier();
			String name2 = method2.getName().getIdentifier();

			// method declarations (constructors) are sorted by name
			int cmp = name1.compareTo(name2);
			if (cmp != 0) {
				return cmp;
			}

			// if names equal, sort by parameter types
			List<SingleVariableDeclaration> parameters1 = method1.parameters();
			List<SingleVariableDeclaration> parameters2 = method2.parameters();
			int length1 = parameters1.size();
			int length2 = parameters2.size();

			int len = Math.min(length1, length2);
			for (int i = 0; i < len; i++) {
				SingleVariableDeclaration param1 = parameters1.get(i);
				SingleVariableDeclaration param2 = parameters2.get(i);
				cmp = buildSignature(param1.getType()).compareTo(buildSignature(param2.getType()));
				if (cmp != 0) {
					return cmp;
				}
			}
			if (length1 != length2) {
				return length1 - length2;
			}
			return preserveRelativeOrder(bodyDeclaration1, bodyDeclaration2);
		}
		case ASTNode.FIELD_DECLARATION: {
			FieldDeclaration field1 = (FieldDeclaration) bodyDeclaration1;
			FieldDeclaration field2 = (FieldDeclaration) bodyDeclaration2;

			String name1 = ((VariableDeclarationFragment) field1.fragments().get(0)).getName().getIdentifier();
			String name2 = ((VariableDeclarationFragment) field2.fragments().get(0)).getName().getIdentifier();

			// field declarations are sorted by name
			return compareNames(bodyDeclaration1, bodyDeclaration2, name1, name2);
		}
		case ASTNode.INITIALIZER: {
			// preserve relative order
			return preserveRelativeOrder(bodyDeclaration1, bodyDeclaration2);
		}
		case ASTNode.TYPE_DECLARATION:
		case ASTNode.ENUM_DECLARATION:
		case ASTNode.ANNOTATION_TYPE_DECLARATION: {
			AbstractTypeDeclaration type1 = (AbstractTypeDeclaration) bodyDeclaration1;
			AbstractTypeDeclaration type2 = (AbstractTypeDeclaration) bodyDeclaration2;

			String name1 = type1.getName().getIdentifier();
			String name2 = type2.getName().getIdentifier();

			// typedeclarations are sorted by name
			return compareNames(bodyDeclaration1, bodyDeclaration2, name1, name2);
		}
		case ASTNode.ENUM_CONSTANT_DECLARATION: {
			EnumConstantDeclaration decl1 = (EnumConstantDeclaration) bodyDeclaration1;
			EnumConstantDeclaration decl2 = (EnumConstantDeclaration) bodyDeclaration2;

			String name1 = decl1.getName().getIdentifier();
			String name2 = decl2.getName().getIdentifier();

			// enum constants declarations are sorted by name
			return compareNames(bodyDeclaration1, bodyDeclaration2, name1, name2);
		}
		case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION: {
			AnnotationTypeMemberDeclaration decl1 = (AnnotationTypeMemberDeclaration) bodyDeclaration1;
			AnnotationTypeMemberDeclaration decl2 = (AnnotationTypeMemberDeclaration) bodyDeclaration2;

			String name1 = decl1.getName().getIdentifier();
			String name2 = decl2.getName().getIdentifier();

			// enum constants declarations are sorted by name
			return compareNames(bodyDeclaration1, bodyDeclaration2, name1, name2);
		}
		}
		return 0;
	}

	private static int sortPreservedCategory(int category) {
		switch (category) {
		case STATIC_FIELDS_INDEX:
		case STATIC_INIT_INDEX:
			return STATIC_FIELDS_INDEX;
		case FIELDS_INDEX:
		case INIT_INDEX:
			return FIELDS_INDEX;
		default:
			return category;
		}
	}

	private boolean isSortPreserved(BodyDeclaration bodyDeclaration) {
		switch (bodyDeclaration.getNodeType()) {
		case ASTNode.FIELD_DECLARATION:
		case ASTNode.ENUM_CONSTANT_DECLARATION:
		case ASTNode.INITIALIZER:
			return true;
		default:
			return false;
		}
	}

	private int preserveRelativeOrder(BodyDeclaration bodyDeclaration1, BodyDeclaration bodyDeclaration2) {
		int value1 = (Integer) bodyDeclaration1.getProperty(CompilationUnitSorter.RELATIVE_ORDER);
		int value2 = (Integer) bodyDeclaration2.getProperty(CompilationUnitSorter.RELATIVE_ORDER);
		return value1 - value2;
	}

	private int compareNames(BodyDeclaration bodyDeclaration1, BodyDeclaration bodyDeclaration2, String name1, String name2) {
		int cmp = name1.compareTo(name2);
		if (cmp != 0) {
			return cmp;
		}
		return preserveRelativeOrder(bodyDeclaration1, bodyDeclaration2);
	}

	private String buildSignature(Type type) {
		NaiveASTFlattener flattener = new NaiveASTFlattener();
		type.accept(flattener);
		return flattener.getResult();
	}
}
