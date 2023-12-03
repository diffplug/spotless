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
package com.diffplug.spotless.extra.glue.jdt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.manipulation.CodeStyleConfiguration;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.internal.core.BufferManager;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.jdt.internal.core.JavaCorePreferenceInitializer;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.nd.indexer.Indexer;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettingsConstants;

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework;

/**
 * Basic set-up for formatting features based on {@code org.eclipse.jdt:org.eclipse.jdt.core.manipulation}.
 */
class EclipseJdtCoreManipulation {
	// The JDT UI shall be used for creating the settings (JavaUI not imported due to dependencies).
	private final static String JDT_UI_PLUGIN_ID = "org.eclipse.jdt.ui";

	private final static String ROOT_AS_SRC = "";
	private final static String PROJECT_NAME = "spotless";
	private final static String SOURCE_NAME = "source.java";

	protected final ILog logger;

	private final AtomicInteger uniqueProjectId = new AtomicInteger(0);
	private final Map<String, String> defaultOptions;

	protected EclipseJdtCoreManipulation() throws Exception {
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

					config.useSlf4J(EclipseJdtOrganizeImportStepImpl.class.getPackage().getName());
					config.set(InternalPlatform.PROP_OS, ""); //Required for org.eclipse.core.internal.resources.OS initialization
				},
				plugins -> {
					plugins.applyDefault();

					//JDT configuration requires an existing project source folder.
					plugins.add(new org.eclipse.core.internal.filesystem.Activator());
					plugins.add(new JavaCore());
				})) {

			initializeJdtUiPreferenceDefaults();
			/*
			 * Assure that the 'allowed keys' are initialized, otherwise
			 * JProject will not accept any options.
			 */
			new JavaCorePreferenceInitializer().initializeDefaultPreferences();

			/*
			 * Don't run indexer in background (does not disable thread but the job scheduling)
			 */
			Indexer.getInstance().enableAutomaticIndexing(false);
		}

		defaultOptions = new HashMap<>();
		defaultOptions.put(JavaCore.COMPILER_SOURCE, getJavaCoreVersion());
		logger = JavaCore.getPlugin().getLog();
	}

	private static void initializeJdtUiPreferenceDefaults() {
		//Following values correspond org.eclipse.jdt.ui.PreferenceConstants (not used due to SWT dependency)
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

	private static String getJavaCoreVersion() {
		final String javaVersion = System.getProperty("java.version");
		final List<String> orderedSupportedCoreVersions = JavaCore.getAllVersions();
		for (String coreVersion : orderedSupportedCoreVersions) {
			if (javaVersion.startsWith(coreVersion)) {
				return coreVersion;
			}
		}
		return orderedSupportedCoreVersions.get(orderedSupportedCoreVersions.size() - 1);
	}

	/**
	 * Creates a JAVA project and applies the configuration.
	 * @param settings Configuration settings
	 * @return Configured JAVA project
	 * @throws Exception In case the project creation fails
	 */
	protected final IJavaProject createProject(Properties settings) throws Exception {
		String uniqueProjectName = String.format("%s-%d", PROJECT_NAME, uniqueProjectId.incrementAndGet());
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(uniqueProjectName);
		// The project must be open before items (natures, folders, sources, ...) can be created
		if (!project.exists()) {
			project.create(null); //Might still exist in case of restarts and dedicated class loader
		}
		project.open(0, null);

		//If the project nature is not set, the AST is not created for the compilation units
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[]{JavaCore.NATURE_ID});
		project.setDescription(description, null);
		IJavaProject jProject = JavaCore.create(project);

		Map<String, String> allSettings = new HashMap<>(defaultOptions);
		settings.forEach((key, value) -> {
			allSettings.put(key.toString(), value.toString());
		});
		//Configure JDT manipulation processor
		IEclipsePreferences projectPrefs = new ProjectScope(project.getProject()).getNode(JavaManipulation.getPreferenceNodeId());
		allSettings.forEach((key, value) -> {
			projectPrefs.put(key.toString(), value.toString());
		});
		/*
		 * Configure options taken directly from the Java project (without qualifier).
		 * Whether a setting is a Java project option or not, is filtered by the
		 * JavaCorePreferenceInitializer, initialized by the constructor of this class.
		 */
		jProject.setOptions(allSettings);

		// Eclipse source files require an existing source folder for creation
		IPackageFragmentRoot src = jProject.getPackageFragmentRoot(jProject.getProject());
		IPackageFragment pkg = src.createPackageFragment(ROOT_AS_SRC, true, null);
		IFolder folder = project.getFolder(uniqueProjectName);
		if (!folder.exists()) {
			folder.create(0, false, null);
		}

		// Eclipse clean-up requires an existing source file
		pkg.createCompilationUnit(SOURCE_NAME, "", true, null);

		return jProject;
	}

	protected final ICompilationUnit createCompilationUnit(String contents, IJavaProject jProject) throws Exception {
		IPackageFragmentRoot src = jProject.getPackageFragmentRoot(jProject.getProject());
		IPackageFragment pkg = src.getPackageFragment(ROOT_AS_SRC);
		return new RamCompilationUnit((PackageFragment) pkg, contents);
	}

	/** Keep compilation units in RAM */
	private static class RamCompilationUnit extends CompilationUnit {

		/*
		 * Each RAM compilation unit has its own buffer manager to
		 * prevent dropping of CUs when a maximum size is reached.
		 */
		private final RamBufferManager manager;

		RamCompilationUnit(PackageFragment parent, String contents) {
			super(parent, SOURCE_NAME, DefaultWorkingCopyOwner.PRIMARY);
			manager = new RamBufferManager();
			IBuffer buffer = BufferManager.createBuffer(this);
			buffer.setContents(contents.toCharArray());
			manager.add(buffer);
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		protected BufferManager getBufferManager() {
			return manager;
		}

		@Override
		public void save(IProgressMonitor pm, boolean force) throws JavaModelException {
			//RAM CU is never stored on disk
		}

		@Override
		public ICompilationUnit getWorkingCopy(IProgressMonitor monitor) throws JavaModelException {
			return new RamCompilationUnit((PackageFragment) this.getParent(), getBuffer().getContents());
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj; //Working copies are not supported
		}
	}

	/** Work around package privileges */
	private static class RamBufferManager extends BufferManager {
		void add(IBuffer buffer) {
			addBuffer(buffer);
		}
	}
}
