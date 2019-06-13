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

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.internal.resources.OS;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.BufferManager;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.PackageFragment;

/**
 * Helper methods to create Java compilation unit.
 * <p>
 * The helper provides a pseudo extension of the OS (OS specific JARs are not provided with Spotless).
 * The OS initialization is required for compilation unit validation
 * (see {@code org.eclipse.core.internal.resources.LocationValidator} for details).
 * </p>
 */
class EclipseJdtFactory extends OS {

	private final static String ROOT_AS_SRC = "";
	private final static String PROJECT_NAME = "spotless";
	private final static String SOURCE_NAME = "source.java";
	private final static AtomicInteger UNIQUE_PROJECT_ID = new AtomicInteger(0);

	private final static Map<String, String> DEFAULT_OPTIONS;

	static {
		Map<String, String> defaultOptions = new HashMap<>();
		defaultOptions.put(JavaCore.COMPILER_SOURCE, getJavaCoreVersion());
		DEFAULT_OPTIONS = Collections.unmodifiableMap(defaultOptions);
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
	public final static IJavaProject createProject(Properties settings) throws Exception {
		String uniqueProjectName = String.format("%s-%d", PROJECT_NAME, UNIQUE_PROJECT_ID.incrementAndGet());
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(uniqueProjectName);
		// The project must be open before items (natures, folders, sources, ...) can be created
		project.create(null);
		project.open(0, null);
		//If the project nature is not set, things like AST are not created for the Java projects
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[]{JavaCore.NATURE_ID});
		project.setDescription(description, null);
		IJavaProject jProject = JavaCore.create(project);

		Map<String, String> settingsMap = new HashMap<>(DEFAULT_OPTIONS);
		settings.forEach((key, value) -> {
			settingsMap.put(key.toString(), value.toString());
		});
		jProject.setOptions(settingsMap);

		// Eclipse source files require an existing source folder for creation
		IPackageFragmentRoot src = jProject.getPackageFragmentRoot(jProject.getProject());
		IPackageFragment pkg = src.createPackageFragment(ROOT_AS_SRC, true, null);
		IFolder folder = project.getFolder(uniqueProjectName);
		folder.create(0, false, null);

		// Eclipse clean-up requires an existing source file
		pkg.createCompilationUnit(SOURCE_NAME, "", true, null);

		//
		disableSecondaryTypes(project);

		return jProject;
	}

	private static void disableSecondaryTypes(IProject project) {
		JavaModelManager.PerProjectInfo info = JavaModelManager.getJavaModelManager().getPerProjectInfo(project, true);
		info.secondaryTypes = new Hashtable<String, Map<String, IType>>();
	}

	public static ICompilationUnit createJavaSource(String contents, IJavaProject jProject) throws Exception {
		IPackageFragmentRoot src = jProject.getPackageFragmentRoot(jProject.getProject());
		IPackageFragment pkg = src.getPackageFragment(ROOT_AS_SRC);
		return new RamCompilationUnit((PackageFragment) pkg, contents);
	}

	/** Spotless keeps compilation units in RAM as long as they are worked on. */
	private static class RamCompilationUnit extends CompilationUnit {

		//Each RMA compilation unit has its own buffer manager. A drop is therefore prevented.
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
			//RAM CU is never saved to disk
		}

		@Override
		public ICompilationUnit getWorkingCopy(IProgressMonitor monitor) throws JavaModelException {
			throw new UnsupportedOperationException("Spotless RAM compilation unit cannot be copied.");
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj; //Working copies are not supported
		}
	}

	/** Work-around required package privileges when adding buffer for manager singleton */
	private static class RamBufferManager extends BufferManager {
		void add(IBuffer buffer) {
			addBuffer(buffer);
		}
	}
}
