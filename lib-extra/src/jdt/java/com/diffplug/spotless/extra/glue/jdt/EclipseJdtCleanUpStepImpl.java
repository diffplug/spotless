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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

/** Clean-up step which calls out to the Eclipse JDT clean-up / import sorter. */
public class EclipseJdtCleanUpStepImpl extends EclipseJdtCoreManipulation {

	/**
	 * In case of Eclipse JDT clean-up problems (warnings + errors)
	 * the clean-up step is skipped if not problems shall not be ignored.
	 * <p>
	 * Value is either 'true' or 'false' ('false' per default)
	 * </p>
	 */
	public static final String IGNORE_CLEAN_UP_PROBLEMS = "ignoreCleanUpProblems";

	private final boolean ignoreCleanUpProblems;
	private final IJavaProject jdtConfiguration;
	private final CleanUpFactory cleanUpFactory;

	public EclipseJdtCleanUpStepImpl(Properties settings) throws Exception {
		jdtConfiguration = createProject(settings);
		cleanUpFactory = new CleanUpFactory(settings);
		ignoreCleanUpProblems = Boolean.parseBoolean(settings.getProperty(IGNORE_CLEAN_UP_PROBLEMS, "false"));
	}

	/** Formats Java raw text. The file-location is used in log messages. */
	public String format(String raw, String fileLocation) throws Exception {
		ICompilationUnit compilationUnit = createCompilationUnit(raw, jdtConfiguration);
		SpotlessRefactoring refactoring = new SpotlessRefactoring(compilationUnit, ignoreCleanUpProblems);
		RefactoringStatus report = refactoring.apply(cleanUpFactory.create());
		Arrays.stream(report.getEntries()).map(entry -> new SpotlessStatus(entry, fileLocation)).forEach(status -> logger.log(status));
		return compilationUnit.getBuffer().getContents();
	}

	/**
	 * Spotless version of {@code org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring}.
	 * <p/>
	 * Spotless does not request (graphical) user feedback neither does it provide undo-information.
	 * Since Spotless re-factoring / formatting is applied without any further explanation of the changes (preview, warnings, ...),
	 * it skips per default steps reporting problems (non-fatal errors or warnings) to ensure that the result is as expected by the user.
	 * Spotless applies the JDT re-factoring without providing a project scope (dependencies, ...).
	 * Hence steps can cause (fatal) errors which would pass within an Eclipse project.
	 * Unlike the Eclipse re-factoring process, Spotless does not abort in case a step
	 * fails, but just reports and skips the step.
	 */
	private static class SpotlessRefactoring {

		private final ICompilationUnit source;
		private final ICompilationUnit[] sources;
		private final boolean ignoreProblems;
		private final IProgressMonitor doNotMonitor;
		private CompilationUnit lazyAst;
		private boolean astIsFresh;

		SpotlessRefactoring(ICompilationUnit sourceToRefactor, boolean ignoreCleanUpProblems) {
			source = sourceToRefactor;
			sources = new ICompilationUnit[]{sourceToRefactor};
			ignoreProblems = ignoreCleanUpProblems;
			doNotMonitor = new NullProgressMonitor();
			lazyAst = null;
			astIsFresh = false;
		}

		RefactoringStatus apply(List<ICleanUp> steps) throws CoreException {
			RefactoringStatus overallStatus = new RefactoringStatus();
			for (ICleanUp step : steps) {
				apply(step, overallStatus);
			}
			return overallStatus;
		}

		private void apply(ICleanUp step, RefactoringStatus overallStatus) throws CoreException {
			RefactoringStatus preCheckStatus = step.checkPreConditions(source.getJavaProject(), sources, doNotMonitor);
			overallStatus.merge(preCheckStatus);
			if (isStepOk(preCheckStatus)) {
				CleanUpContext context = createContext(step.getRequirements());
				ICleanUpFix fix = step.createFix(context);
				RefactoringStatus postCheckStatus = apply(step, Optional.ofNullable(fix));
				overallStatus.merge(postCheckStatus);
			}
		}

		private RefactoringStatus apply(ICleanUp step, Optional<ICleanUpFix> fix) throws CoreException {
			RefactoringStatus postCheckStatus = new RefactoringStatus();
			if (fix.isPresent()) {
				CompilationUnitChange change = fix.get().createChange(doNotMonitor);
				TextEdit edit = change.getEdit();
				if (null != edit) {
					UndoEdit undo = source.applyTextEdit(edit, doNotMonitor);
					postCheckStatus = step.checkPostConditions(doNotMonitor);
					if (isStepOk(postCheckStatus)) {
						astIsFresh = false;
					} else {
						postCheckStatus.addInfo("Undo step " + step.getClass().getSimpleName());
						if (null != undo) {
							source.applyTextEdit(undo, doNotMonitor);
						}
					}
				}
			}
			return postCheckStatus;
		}

		private boolean isStepOk(RefactoringStatus stepStatus) {
			if (ignoreProblems) {
				return stepStatus.getSeverity() < RefactoringStatus.FATAL;
			}
			return stepStatus.getSeverity() < RefactoringStatus.WARNING;
		}

		private CleanUpContext createContext(CleanUpRequirements requirements) {
			if ((requirements.requiresAST() && null == lazyAst) ||
					(requirements.requiresFreshAST() && false == astIsFresh)) {
				lazyAst = SharedASTProviderCore.getAST(source, SharedASTProviderCore.WAIT_YES, null);
				astIsFresh = true;
			}
			return new CleanUpContext(source, lazyAst);
		}

	};

	private static class SpotlessStatus implements IStatus {
		private final IStatus cleanUpStatus;
		private final String fileLocationAsPluginId;

		SpotlessStatus(RefactoringStatusEntry entry, String fileLocation) {
			cleanUpStatus = entry.toStatus();
			fileLocationAsPluginId = fileLocation;
		}

		@Override
		public IStatus[] getChildren() {
			return cleanUpStatus.getChildren();
		}

		@Override
		public int getCode() {
			return cleanUpStatus.getCode();
		}

		@Override
		public Throwable getException() {
			return cleanUpStatus.getException();
		}

		@Override
		public String getMessage() {
			return cleanUpStatus.getMessage();
		}

		@Override
		public String getPlugin() {
			/*
			 * The plugin ID of the JDT Clean-Up is always a common string.
			 * Hence it does not add any valuable information for the Spotless user.
			 * It is replaced by the file location which is hidden from the JDT re-factoring
			 * process.
			 */
			return fileLocationAsPluginId;
		}

		@Override
		public int getSeverity() {
			return cleanUpStatus.getSeverity();
		}

		@Override
		public boolean isMultiStatus() {
			return cleanUpStatus.isMultiStatus();
		}

		@Override
		public boolean isOK() {
			return cleanUpStatus.isOK();
		}

		@Override
		public boolean matches(int severityMask) {
			return cleanUpStatus.matches(severityMask);
		}

	};
}
