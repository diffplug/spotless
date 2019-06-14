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

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CodeStyleConfiguration;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettingsConstants;

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework;

/** Clean-up step which calls out to the Eclipse JDT clean-up / import sorter. */
public class EclipseJdtCleanUpStepImpl {

	// The JDT UI shall be used for creating the settings.
	private final static String JDT_UI_PLUGIN_ID = "org.eclipse.jdt.ui";
	private final IJavaProject jdtConfiguration; //The project stores the JDT clean-up configuration
	private final EclipseJdtHelper jdtHelper;

	public EclipseJdtCleanUpStepImpl(Properties settings) throws Exception {
		if (SpotlessEclipseFramework.setup(
				core -> {
					/*
					 * Indexer needs to exist (but is not used) for JDT clean-up.
					 * The indexer is not created in headless mode by JDT.
					 * 'Active' platform state signals non-headless mode ('Resolved' is default state)..
					 */
					core.add(new org.eclipse.core.internal.registry.osgi.Activator());
					
					core.add(new org.eclipse.core.internal.runtime.PlatformActivator());
					core.add(new org.eclipse.core.internal.preferences.Activator());
					core.add(new org.eclipse.core.internal.runtime.Activator());
				},
				config -> {
					config.hideEnvironment();
					config.disableDebugging();
					config.ignoreUnsupportedPreferences();
					config.useTemporaryLocations();
					config.changeSystemLineSeparator();

					/*
					 * The default 'no content type specific handling' is insufficient.
					 * The Java source type needs to be recognized by file extension.
					 */
					config.add(IContentTypeManager.class, new JavaContentTypeManager());

					config.useSlf4J(EclipseJdtCleanUpStepImpl.class.getPackage().getName());
					config.set(InternalPlatform.PROP_OS, "");
				},
				plugins -> {
					plugins.applyDefault();

					//JDT configuration requires an existing project source folder.
					plugins.add(new org.eclipse.core.internal.filesystem.Activator());
					plugins.add(new JavaCore());
				})) {
			initializeJdtUiDefaultSettings();
		}
		jdtHelper = EclipseJdtHelper.getInstance();
		jdtConfiguration = jdtHelper.createProject(settings);
	}

	private static void initializeJdtUiDefaultSettings() {
		//Following values correspond org.eclipse.jdt.ui.PreferenceConstants
		JavaManipulation.setPreferenceNodeId(JDT_UI_PLUGIN_ID);
		IEclipsePreferences prefs = DefaultScope.INSTANCE.getNode(JDT_UI_PLUGIN_ID);

		prefs.put(CodeStyleConfiguration.ORGIMPORTS_IMPORTORDER, "java;javax;org;com");
		prefs.put(CodeStyleConfiguration.ORGIMPORTS_ONDEMANDTHRESHOLD, "99");
		prefs.put(CodeStyleConfiguration.ORGIMPORTS_STATIC_ONDEMANDTHRESHOLD, "99");

		prefs.put(CodeGenerationSettingsConstants.CODEGEN_KEYWORD_THIS, "false");
		prefs.put(CodeGenerationSettingsConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, "false");
		prefs.put(CodeGenerationSettingsConstants.CODEGEN_ADD_COMMENTS, "true");
		prefs.put(CodeGenerationSettingsConstants.ORGIMPORTS_IGNORELOWERCASE, "true");
	}

	public String organizeImport(String raw) throws Exception {
		ICompilationUnit compilationUnit = jdtHelper.createCompilationUnit(raw, jdtConfiguration);
		CompilationUnit ast = SharedASTProviderCore.getAST(compilationUnit, SharedASTProviderCore.WAIT_YES, null);
		OrganizeImportsOperation formatOperation = new OrganizeImportsOperation(compilationUnit, ast, true, false, true, null);
		try {
			formatOperation.run(null);
			return compilationUnit.getSource();
		} catch (OperationCanceledException | CoreException e) {
			throw new IllegalArgumentException("Invalid java syntax for formatting.", e);
		}
	}

}
