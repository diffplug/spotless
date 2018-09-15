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
package com.diffplug.spotless.cpp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Common utilities for C/C++ */
public class CppDefaults {
	//Prevent instantiation
	private CppDefaults() {};

	/** Filtering based on Eclipse-CDT <code>org.eclipse.core.contenttype.contentTypes</code> */
	/**
	 * Filter based on Eclipse-CDT <code>org.eclipse.core.contenttype.contentTypes</code>
	 * extension <code>cSource</code>, <code>cHeader</code>, <code>cxxSource</code> and <code>cxxHeader</code>.
	 */
	public static final List<String> FILE_FILTER = Collections.unmodifiableList(
			Arrays.asList("c", "h", "C", "cpp", "cxx", "cc", "c++", "h", "hpp", "hh", "hxx", "inc")
					.stream().map(s -> {
						return "**/*." + s;
					}).collect(Collectors.toList()));

	/**
	 * Default delimiter expression shall cover most valid and common starts of C/C++ declarations and definitions.
	 * Furthermore it shall not conflict with terms commonly used within license headers.
	 * Note that the longest match is selected. Hence "using namespace foo" is preferred over "namespace foo".
	 */
	public static final String DELIMITER_EXPR = Arrays.asList(
			"#define", "#error", "#if", "#ifdef", "#ifndef", "#include", "#pragma", "#undef",
			"asm", "class", "namespace", "struct", "typedef", "using namespace",
			"const", "extern", "mutable", "signed", "unsigned", "volatile",
			"auto", "char", "double", "float", "int", "long", "void",
			"char16_t", "char32_t", "char64_t",
			"int8_t", "int16_t", "int32_t", "int64_t",
			"__int8_t", "__int16_t", "__int32_t", "__int64_t",
			"uint8_t", "uint16_t", "uint32_t", "uint64_t")
			.stream().map(s -> {
				return "(?<![0-9a-zA-Z]+)" + s.replaceAll("#", "\\\\#").replaceAll(" ", "[\\\\n\\\\s]+") + "[\\*\\s\\n]+";
			}).collect(Collectors.joining("|"));

}
