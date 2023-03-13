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
package com.diffplug.spotless.extra.eclipse.java;

import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

/** Clean-up step which calls out to the Eclipse JDT clean-up / import sorter. */
public class EclipseJdtOrganizeImportStepImpl extends EclipseJdtCoreManipulation {
	private final IJavaProject jdtConfiguration; //The project stores the JDT clean-up configuration

	public EclipseJdtOrganizeImportStepImpl(Properties settings) throws Exception {
		jdtConfiguration = createProject(settings);
	}

	public String format(String raw) throws Exception {
		ICompilationUnit compilationUnit = createCompilationUnit(raw, jdtConfiguration);
		CompilationUnit ast = SharedASTProviderCore.getAST(compilationUnit, SharedASTProviderCore.WAIT_YES, null);
		OrganizeImportsOperation formatOperation = new OrganizeImportsOperation(compilationUnit, ast, false, false, true, null);
		try {
			formatOperation.run(null);
			return compilationUnit.getSource();
		} catch (OperationCanceledException | CoreException e) {
			throw new IllegalArgumentException("Invalid java syntax for formatting.", e);
		}
	}
}
