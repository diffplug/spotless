/*
 * Copyright 2016-2020 DiffPlug
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
package com.diffplug.spotless.extra.groovy;

import com.diffplug.spotless.JreVersion;

/** Older versions only support Java version 11 or lower */
public class GrEclipseFormatterStepOldTest extends GrEclipseFormatterStepTest {
	@Override
	protected String[] getSupportedVersions() {
		return new String[]{"2.3.0", "4.6.3", "4.8.0", "4.8.1", "4.10.0", "4.12.0", "4.13.0", "4.14.0", "4.15.0", "4.16.0"};
	}

	@Override
	protected void makeAssumptions() {
		// JRE 11 warns like this:
		//		WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
		//		WARNING: All illegal access operations will be denied in a future release
		// And after that it fails like this:
		//		Caused by: java.lang.NoClassDefFoundError: Could not initialize class org.codehaus.groovy.vmplugin.v7.Java7
		//		at org.codehaus.groovy.vmplugin.VMPluginFactory.<clinit>(VMPluginFactory.java:39)
		//		at org.codehaus.groovy.ast.ClassHelper.makeCached(ClassHelper.java:133)
		//		at org.codehaus.groovy.ast.ClassHelper.<clinit>(ClassHelper.java:67)
		//		at org.codehaus.groovy.classgen.Verifier.<clinit>(Verifier.java:113)
		//		at org.codehaus.groovy.control.CompilationUnit.<init>(CompilationUnit.java:158)
		//		at org.codehaus.jdt.groovy.internal.compiler.ast.GroovyParser.makeCompilationUnit(GroovyParser.java:467)
		//		at org.codehaus.jdt.groovy.internal.compiler.ast.GroovyParser.<init>(GroovyParser.java:247)
		//		at org.codehaus.jdt.groovy.internal.compiler.ast.GroovyParser.<init>(GroovyParser.java:216)
		//		at org.codehaus.groovy.eclipse.core.compiler.GroovySnippetParser.dietParse(GroovySnippetParser.java:105)
		//		at org.codehaus.groovy.eclipse.core.compiler.GroovySnippetParser.parse(GroovySnippetParser.java:69)
		//		at org.codehaus.groovy.eclipse.refactoring.core.utils.ASTTools.getASTNodeFromSource(ASTTools.java:204)
		//		at org.codehaus.groovy.eclipse.refactoring.formatter.DefaultGroovyFormatter.initCodebase(DefaultGroovyFormatter.java:109)
		//		at org.codehaus.groovy.eclipse.refactoring.formatter.DefaultGroovyFormatter.format(DefaultGroovyFormatter.java:121)
		//		at com.diffplug.spotless.extra.eclipse.groovy.GrEclipseFormatterStepImpl.format(GrEclipseFormatterStepImpl.java:81)
		JreVersion.assume11OrLess();
	}

}
