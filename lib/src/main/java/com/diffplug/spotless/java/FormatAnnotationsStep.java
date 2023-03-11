/*
 * Copyright 2022-2023 DiffPlug
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;

/**
 * Some formatters put every annotation on its own line
 * -- even type annotations, which should be on the same line as the type they qualify.
 * This class corrects the formatting.
 * This is useful as a postprocessing step after a Java formatter that is not cognizant of type annotations.

 * <p>
 * Note: A type annotation is an annotation that is meta-annotated with {@code @Target({ElementType.TYPE_USE})}.
 */
public final class FormatAnnotationsStep {

	/**
	 * Simple names of type annotations.
	 * A type annotation is an annotation that is meta-annotated with @Target({ElementType.TYPE_USE}).
	 * A type annotation should be formatted on the same line as the type it qualifies.
	 */
	private static final List<String> defaultTypeAnnotations =
			// Use simple names because Spotless has no access to the
			// fully-qualified names or the definitions of the type qualifiers.
			Arrays.asList(
					// Type annotations from the Checker Framework and all
					// the tools it supports: FindBugs, JetBrains (IntelliJ),
					// Eclipse, NetBeans, Spring, JML, Android, etc.
					"A",
					"ACCBottom",
					"Acceleration",
					"ACCTop",
					"AinferBottom",
					"AinferDefaultType",
					"AinferParent",
					"AinferSibling1",
					"AinferSibling2",
					"AinferTop",
					"AinferImplicitAnno",
					"AinferSiblingWithFields",
					"AlwaysSafe",
					"Angle",
					"AnnoWithStringArg",
					"Area",
					"ArrayLen",
					"ArrayLenRange",
					"ArrayWithoutPackage",
					"AwtAlphaCompositingRule",
					"AwtColorSpace",
					"AwtCursorType",
					"AwtFlowLayout",
					"B",
					"BinaryName",
					"BinaryNameInUnnamedPackage",
					"BinaryNameOrPrimitiveType",
					"BinaryNameWithoutPackage",
					"BoolVal",
					"Bottom",
					"BottomQualifier",
					"BottomThis",
					"BottomVal",
					"C",
					"CalledMethods",
					"CalledMethodsBottom",
					"CalledMethodsPredicate",
					"CalledMethodsTop",
					"CanonicalName",
					"CanonicalNameAndBinaryName",
					"CanonicalNameOrEmpty",
					"CanonicalNameOrPrimitiveType",
					"CCBottom",
					"CCTop",
					"cd",
					"ClassBound",
					"ClassGetName",
					"ClassGetSimpleName",
					"ClassVal",
					"ClassValBottom",
					"CompilerMessageKey",
					"CompilerMessageKeyBottom",
					"Constant",
					"Critical",
					"Current",
					"D",
					"DefaultType",
					"degrees",
					"Det",
					"DoesNotMatchRegex",
					"DotSeparatedIdentifiers",
					"DotSeparatedIdentifiersOrPrimitiveType",
					"DoubleVal",
					"E",
					"Encrypted",
					"EnhancedRegex",
					"EnumVal",
					"Even",
					"F",
					"FBCBottom",
					"FEBottom",
					"FEBot",
					"Fenum",
					"FenumBottom",
					"FenumTop",
					"FETop",
					"FieldDescriptor",
					"FieldDescriptorForPrimitive",
					"FieldDescriptorForPrimitiveOrArrayInUnnamedPackage",
					"FieldDescriptorWithoutPackage",
					"FlowExp",
					"Force",
					"Format",
					"FormatBottom",
					"FqBinaryName",
					"Frequency",
					"FullyQualifiedName",
					"g",
					"GTENegativeOne",
					"GuardedBy",
					"GuardedByBottom",
					"GuardedByUnknown",
					"GuardSatisfied",
					"h",
					"H1Bot",
					"H1Invalid",
					"H1Poly",
					"H1S1",
					"H1S2",
					"H1Top",
					"H2Bot",
					"H2Poly",
					"H2S1",
					"H2S2",
					"H2Top",
					"Hz",
					"I18nFormat",
					"I18nFormatBottom",
					"I18nFormatFor",
					"I18nInvalidFormat",
					"I18nUnknownFormat",
					"Identifier",
					"IdentifierOrArray",
					"IdentifierOrPrimitiveType",
					"ImplicitAnno",
					"IndexFor",
					"IndexOrHigh",
					"IndexOrLow",
					"Initialized",
					"InitializedFields",
					"InitializedFieldsBottom",
					"InitializedFieldsPredicate",
					"InternalForm",
					"Interned",
					"InternedDistinct",
					"IntRange",
					"IntVal",
					"InvalidFormat",
					"K",
					"KeyFor",
					"KeyForBottom",
					"KeyForType",
					"kg",
					"kHz",
					"km",
					"km2",
					"km3",
					"kmPERh",
					"kN",
					"LbTop",
					"LB_TOP",
					"LeakedToResult",
					"Length",
					"LengthOf",
					"LessThan",
					"LessThanBottom",
					"LessThanUnknown",
					"LocalizableKey",
					"LocalizableKeyBottom",
					"Localized",
					"LowerBoundBottom",
					"LowerBoundUnknown",
					"LTEqLengthOf",
					"LTLengthOf",
					"LTOMLengthOf",
					"Luminance",
					"m",
					"m2",
					"m3",
					"Mass",
					"MatchesRegex",
					"MaybeAliased",
					"MaybeDerivedFromConstant",
					"MaybePresent",
					"MaybeThis",
					"MethodDescriptor",
					"MethodVal",
					"MethodValBottom",
					"min",
					"MinLen",
					"mm",
					"mm2",
					"mm3",
					"mol",
					"MonotonicNonNull",
					"MonotonicNonNullType",
					"MonotonicOdd",
					"mPERs",
					"mPERs2",
					"MustCall",
					"MustCallAlias",
					"MustCallUnknown",
					"N",
					"NegativeIndexFor",
					"NewObject",
					"NonConstant",
					"NonDet",
					"NonLeaked",
					"NonNegative",
					"NonNull",
					"NonNullType",
					"NonRaw",
					"NotCalledMethods",
					"NotNull",
					"NotQualifier",
					"NTDBottom",
					"NTDMiddle",
					"NTDSide",
					"NTDTop",
					"Nullable",
					"NullableType",
					"Odd",
					"OptionalBottom",
					"OrderNonDet",
					"Parent",
					"PatternA",
					"PatternAB",
					"PatternAC",
					"PatternB",
					"PatternBC",
					"PatternBottomFull",
					"PatternBottomPartial",
					"PatternC",
					"PatternUnknown",
					"Poly",
					"PolyAll",
					"PolyConstant",
					"PolyDet",
					"PolyEncrypted",
					"PolyFenum",
					"PolyIndex",
					"PolyInitializedFields",
					"PolyInterned",
					"PolyKeyFor",
					"PolyLength",
					"PolyLowerBound",
					"PolyMustCall",
					"PolyNull",
					"PolyNullType",
					"PolyPresent",
					"PolyRaw",
					"PolyReflection",
					"PolyRegex",
					"PolySameLen",
					"PolySignature",
					"PolySigned",
					"PolyTainted",
					"PolyTestAccumulation",
					"PolyTypeDeclDefault",
					"PolyUI",
					"PolyUnit",
					"PolyUpperBound",
					"PolyValue",
					"PolyVariableNameDefault",
					"Positive",
					"Present",
					"PrimitiveType",
					"PropertyKey",
					"PropertyKeyBottom",
					"PurityUnqualified",
					"Qualifier",
					"radians",
					"Raw",
					"ReflectBottom",
					"Regex",
					"RegexBottom",
					"RegexNNGroups",
					"ReportUnqualified",
					"s",
					"SameLen",
					"SameLenBottom",
					"SameLenUnknown",
					"SearchIndexBottom",
					"SearchIndexFor",
					"SearchIndexUnknown",
					"Sibling1",
					"Sibling2",
					"SiblingWithFields",
					"SignatureBottom",
					"Signed",
					"SignednessBottom",
					"SignednessGlb",
					"SignedPositive",
					"SignedPositiveFromUnsigned",
					"Speed",
					"StringVal",
					"SubQual",
					"Substance",
					"SubstringIndexBottom",
					"SubstringIndexFor",
					"SubstringIndexUnknown",
					"SuperQual",
					"SwingBoxOrientation",
					"SwingCompassDirection",
					"SwingElementOrientation",
					"SwingHorizontalOrientation",
					"SwingSplitPaneOrientation",
					"SwingTextOrientation",
					"SwingTitleJustification",
					"SwingTitlePosition",
					"SwingVerticalOrientation",
					"t",
					"Tainted",
					"Temperature",
					"TestAccumulation",
					"TestAccumulationBottom",
					"TestAccumulationPredicate",
					"This",
					"Time",
					"Top",
					"TypeDeclDefaultBottom",
					"TypeDeclDefaultMiddle",
					"TypeDeclDefaultTop",
					"UbTop",
					"UB_TOP",
					"UI",
					"UnderInitialization",
					"Unique",
					"UnitsBottom",
					"UnknownClass",
					"UnknownCompilerMessageKey",
					"UnknownFormat",
					"UnknownInitialization",
					"UnknownInterned",
					"UnknownKeyFor",
					"UnknownLocalizableKey",
					"UnknownLocalized",
					"UnknownMethod",
					"UnknownPropertyKey",
					"UnknownRegex",
					"UnknownSignedness",
					"UnknownThis",
					"UnknownUnits",
					"UnknownVal",
					"Unsigned",
					"Untainted",
					"UpperBoundBottom",
					"UpperBoundLiteral",
					"UpperBoundUnknown",
					"ValueTypeAnno",
					"VariableNameDefaultBottom",
					"VariableNameDefaultMiddle",
					"VariableNameDefaultTop",
					"Volume",
					"WholeProgramInferenceBottom"
			// TODO: Add type annotations from other tools here.

			);

	static final String NAME = "No line break between type annotation and type";

	public static FormatterStep create() {
		return create(Collections.emptyList(), Collections.emptyList());
	}

	public static FormatterStep create(List<String> addedTypeAnnotations, List<String> removedTypeAnnotations) {
		return FormatterStep.create(NAME, new State(addedTypeAnnotations, removedTypeAnnotations), State::toFormatter);
	}

	private FormatAnnotationsStep() {}

	// TODO: Read from a local .type-annotations file.
	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final Set<String> typeAnnotations = new HashSet<>(defaultTypeAnnotations);

		// group 1 is the basename of the annotation.
		private static final String annoNoArgRegex = "@(?:[A-Za-z_][A-Za-z0-9_.]*\\.)?([A-Za-z_][A-Za-z0-9_]*)";
		private static final Pattern annoNoArgPattern = Pattern.compile(annoNoArgRegex);
		// 3 non-empty cases:  ()  (".*")  (.*)
		private static final String annoArgRegex = "(?:\\(\\)|\\(\"[^\"]*\"\\)|\\([^\")][^)]*\\))?";
		// group 1 is the basename of the annotation.
		private static final String annoRegex = annoNoArgRegex + annoArgRegex;
		private static final String trailingAnnoRegex = annoRegex + "$";
		private static final Pattern trailingAnnoPattern = Pattern.compile(trailingAnnoRegex);

		// Heuristic: matches if the line might be within a //, /*, or Javadoc comment.
		private static final Pattern withinCommentPattern = Pattern.compile("//|/\\*(?!.*/*/)|^[ \t]*\\*[ \t]");
		// Don't move an annotation to the start of a comment line.
		private static final Pattern startsWithCommentPattern = Pattern.compile("^[ \t]*(//|/\\*$|/\\*|void\\b)");

		/**
		 * @param addedTypeAnnotations simple names to add to Spotless's default list
		 * @param removedTypeAnnotations simple names to remove from Spotless's default list
		 */
		State(List<String> addedTypeAnnotations, List<String> removedTypeAnnotations) {
			typeAnnotations.addAll(addedTypeAnnotations);
			removedTypeAnnotations.forEach(typeAnnotations::remove);
		}

		FormatterFunc toFormatter() {
			return this::fixupTypeAnnotations;
		}

		/**
		 * Removes line break between type annotations and the following type.
		 *
		 * @param the text of a Java file
		 * @return corrected text of the Java file
		 */
		String fixupTypeAnnotations(String unixStr) {
			// Each element of `lines` ends with a newline.
			String[] lines = unixStr.split("((?<=\n))");
			for (int i = 0; i < lines.length - 1; i++) {
				String line = lines[i];
				if (endsWithTypeAnnotation(line)) {
					String nextLine = lines[i + 1];
					if (startsWithCommentPattern.matcher(nextLine).find()) {
						continue;
					}
					lines[i] = "";
					lines[i + 1] = line.replaceAll("\\s+$", "") + " " + nextLine.replaceAll("^\\s+", "");
				}
			}
			return String.join("", lines);
		}

		/**
		 * Returns true if the line ends with a type annotation.
		 * FormatAnnotationsStep fixes such formatting.
		 */
		boolean endsWithTypeAnnotation(String unixLine) {
			// Remove trailing newline.
			String line = unixLine.replaceAll("\\s+$", "");
			Matcher m = trailingAnnoPattern.matcher(line);
			if (!m.find()) {
				return false;
			}
			String preceding = line.substring(0, m.start());
			String basename = m.group(1);

			if (withinCommentPattern.matcher(preceding).find()) {
				return false;
			}

			return typeAnnotations.contains(basename);
		}
	}
}
