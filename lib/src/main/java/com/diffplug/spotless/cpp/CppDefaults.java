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

	public static final List<String> FILE_FILTER = Collections.unmodifiableList(
			Arrays.asList("c", "h", "C", "cpp", "cxx", "cc", "c++", "h", "hpp", "hh", "hxx", "inc")
					.stream().map(s -> {
						return "**/*." + s;
					}).collect(Collectors.toList()));

	/**
	 * Default delimiter expression shall cover most valid and common starts of C/C++ declarations and definitions.
	 * Furthermore it shall not conflict with terms used within  license header.
	 * Note that the longest match is selected. Hence "using namespace foo" is preferred over "namespace foo".
	 */
	public static final String DELIMITER_EXPR = Arrays.asList(
			"#define", "#error", "#if", "#ifdef", "#ifndef", "#include", "#pragma", "#undef",
			"namespace", "using namespace", "extern", "int main", "void main", "class", "struct")
			.stream().map(s -> {
				return s.replaceAll("#", "\\\\#").replaceAll(" ", "[\\\\n\\\\s]+") + "[\\(\\s\\n]+";
			}).collect(Collectors.joining("|"));

}
