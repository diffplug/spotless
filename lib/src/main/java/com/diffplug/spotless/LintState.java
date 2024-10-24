/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.annotation.Nullable;

public class LintState {
	private final DirtyState dirtyState;
	private final @Nullable List<List<Lint>> lintsPerStep;

	LintState(DirtyState dirtyState, @Nullable List<List<Lint>> lintsPerStep) {
		this.dirtyState = dirtyState;
		this.lintsPerStep = lintsPerStep;
	}

	public DirtyState getDirtyState() {
		return dirtyState;
	}

	public boolean isHasLints() {
		return lintsPerStep != null;
	}

	public boolean isClean() {
		return dirtyState.isClean() && !isHasLints();
	}

	public LinkedHashMap<String, List<Lint>> getLintsByStep(Formatter formatter) {
		if (lintsPerStep == null) {
			throw new IllegalStateException("Check `isHasLints` first!");
		}
		if (lintsPerStep.size() != formatter.getSteps().size()) {
			throw new IllegalStateException("LintState was created with a different formatter!");
		}
		LinkedHashMap<String, List<Lint>> result = new LinkedHashMap<>();
		for (int i = 0; i < lintsPerStep.size(); i++) {
			List<Lint> lints = lintsPerStep.get(i);
			if (lints != null) {
				FormatterStep step = formatter.getSteps().get(i);
				result.put(step.getName(), lints);
			}
		}
		return result;
	}

	public LintState withRemovedSuppressions(Formatter formatter, String relativePath, List<LintSuppression> suppressions) {
		if (lintsPerStep == null) {
			return this;
		}
		if (formatter.getSteps().size() != lintsPerStep.size()) {
			throw new IllegalStateException("LintState was created with a different formatter!");
		}
		boolean changed = false;
		ValuePerStep<List<Lint>> perStepFiltered = new ValuePerStep<>(formatter);
		for (int i = 0; i < lintsPerStep.size(); i++) {
			FormatterStep step = formatter.getSteps().get(i);
			List<Lint> lintsOriginal = lintsPerStep.get(i);
			if (lintsOriginal != null) {
				List<Lint> lints = new ArrayList<>(lintsOriginal);
				Iterator<Lint> iter = lints.iterator();
				while (iter.hasNext()) {
					Lint lint = iter.next();
					for (LintSuppression suppression : suppressions) {
						if (suppression.suppresses(relativePath, step, lint)) {
							changed = true;
							iter.remove();
							break;
						}
					}
				}
				if (!lints.isEmpty()) {
					perStepFiltered.set(i, lints);
				}
			}
		}
		if (changed) {
			return new LintState(dirtyState, perStepFiltered.indexOfFirstValue() == -1 ? null : perStepFiltered);
		} else {
			return this;
		}
	}

	public String asStringDetailed(File file, Formatter formatter) {
		return asString(file, formatter, false);
	}

	public String asStringOneLine(File file, Formatter formatter) {
		return asString(file, formatter, true);
	}

	private String asString(File file, Formatter formatter, boolean oneLine) {
		if (!isHasLints()) {
			return "(none)";
		} else {
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < lintsPerStep.size(); i++) {
				List<Lint> lints = lintsPerStep.get(i);
				if (lints != null) {
					FormatterStep step = formatter.getSteps().get(i);
					for (Lint lint : lints) {
						result.append(file.getName()).append(":");
						lint.addWarningMessageTo(result, step.getName(), oneLine);
						result.append("\n");
					}
				}
			}
			result.setLength(result.length() - 1);
			return result.toString();
		}
	}

	public static LintState of(Formatter formatter, File file) throws IOException {
		return of(formatter, file, Files.readAllBytes(file.toPath()));
	}

	public static LintState of(Formatter formatter, File file, byte[] rawBytes) {
		var exceptions = new ValuePerStep<Throwable>(formatter);
		var raw = new String(rawBytes, formatter.getEncoding());
		var dirty = DirtyState.of(formatter, file, rawBytes, raw, exceptions);

		String toLint = LineEnding.toUnix(dirty.isClean() || dirty.didNotConverge() ? raw : new String(dirty.canonicalBytes(), formatter.getEncoding()));

		var lints = new ValuePerStep<List<Lint>>(formatter);
		// if a step did not throw an exception, then it gets to check for lints if it wants
		for (int i = 0; i < formatter.getSteps().size(); i++) {
			FormatterStep step = formatter.getSteps().get(i);
			Throwable exception = exceptions.get(i);
			if (exception == null || exception == formatStepCausedNoChange()) {
				try {
					var lintsForStep = step.lint(toLint, file);
					if (lintsForStep != null && !lintsForStep.isEmpty()) {
						lints.set(i, lintsForStep);
					}
				} catch (Exception e) {
					lints.set(i, List.of(Lint.createFromThrowable(step, e)));
				}
			}
		}
		// for steps that did throw an exception, we will turn those into lints
		// we try to reuse the exception if possible, but that is only possible if other steps
		// didn't change the formatted value. so we start at the end, and note when the string
		// gets changed by a step. if it does, we rerun the steps to get an exception with accurate line numbers.
		boolean nothingHasChangedSinceLast = true;
		for (int i = formatter.getSteps().size() - 1; i >= 0; i--) {
			FormatterStep step = formatter.getSteps().get(i);
			Throwable exception = exceptions.get(i);
			if (exception != null && exception != formatStepCausedNoChange()) {
				nothingHasChangedSinceLast = false;
			}
			Throwable exceptionForLint;
			if (nothingHasChangedSinceLast) {
				exceptionForLint = exceptions.get(i);
			} else {
				// steps changed the content, so we need to rerun to get an exception with accurate line numbers
				try {
					step.format(toLint, file);
					exceptionForLint = null; // the exception "went away" because it got fixed by a later step
				} catch (Throwable e) {
					exceptionForLint = e;
				}
			}
			List<Lint> lintsForStep;
			if (exceptionForLint instanceof Lint.Has) {
				lintsForStep = ((Lint.Has) exceptionForLint).getLints();
			} else if (exceptionForLint != null && exceptionForLint != formatStepCausedNoChange()) {
				lintsForStep = List.of(Lint.createFromThrowable(step, exceptionForLint));
			} else {
				lintsForStep = List.of();
			}
			if (!lintsForStep.isEmpty()) {
				lints.set(i, lintsForStep);
			}
		}
		return new LintState(dirty, lints.indexOfFirstValue() == -1 ? null : lints);
	}

	/** Returns the DirtyState which corresponds to {@code isClean()}. */
	public static LintState clean() {
		return isClean;
	}

	private static final LintState isClean = new LintState(DirtyState.clean(), null);

	static Throwable formatStepCausedNoChange() {
		return FormatterCausedNoChange.INSTANCE;
	}

	private static class FormatterCausedNoChange extends Exception {
		private static final long serialVersionUID = 1L;

		static final FormatterCausedNoChange INSTANCE = new FormatterCausedNoChange();
	}
}
