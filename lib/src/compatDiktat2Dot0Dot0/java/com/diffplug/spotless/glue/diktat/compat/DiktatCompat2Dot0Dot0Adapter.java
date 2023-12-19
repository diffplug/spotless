/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.glue.diktat.compat;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.saveourtool.diktat.DiktatFactoriesKt;
import com.saveourtool.diktat.DiktatProcessor;
import com.saveourtool.diktat.api.DiktatCallback;
import com.saveourtool.diktat.api.DiktatError;
import com.saveourtool.diktat.api.DiktatRuleConfig;
import com.saveourtool.diktat.api.DiktatRuleSet;

import kotlin.Unit;

public class DiktatCompat2Dot0Dot0Adapter implements DiktatCompatAdapter {
	private final DiktatProcessor processor;
	private final DiktatCallback formatterCallback;
	private final ArrayList<DiktatError> errors = new ArrayList<>();

	public DiktatCompat2Dot0Dot0Adapter(@Nullable File configFile) {
		this.processor = getDiktatReporter(configFile);
		this.formatterCallback = new FormatterCallback(errors);
	}

	@Override
	public String format(File file, String content, boolean isScript) {
		errors.clear();
		String result = processor.fix(content, file.toPath(), formatterCallback);
		DiktatReporting.reportIfRequired(errors, DiktatError::getLine, DiktatError::getCol, DiktatError::getDetail);
		return result;
	}

	private static class FormatterCallback implements DiktatCallback {
		private final ArrayList<DiktatError> errors;

		FormatterCallback(ArrayList<DiktatError> errors) {
			this.errors = errors;
		}

		@Override
		public Unit invoke(DiktatError diktatError, Boolean corrected) {
			doInvoke(diktatError, corrected);
			return Unit.INSTANCE;
		}

		@Override
		public void invoke(@NotNull DiktatError diktatError, boolean corrected) {
			doInvoke(diktatError, corrected);
		}

		private void doInvoke(@NotNull DiktatError diktatError, boolean corrected) {
			if (!corrected) {
				errors.add(diktatError);
			}
		}
	}

	private static DiktatProcessor getDiktatReporter(File configFile) {
		final DiktatRuleSet ruleSet = DiktatFactoriesKt.getDiktatRuleSetFactory().invoke(readRuleConfigs(configFile));
		return DiktatFactoriesKt.getDiktatProcessorFactory().invoke(ruleSet);
	}

	private static List<DiktatRuleConfig> readRuleConfigs(File configFile) {
		if (configFile == null) {
			return Collections.emptyList();
		}
		try (final InputStream configInputStream = new FileInputStream(configFile)) {
			return DiktatFactoriesKt.getDiktatRuleConfigReader().invoke(configInputStream);
		} catch (IOException e) {
			throw new IllegalArgumentException("Fail to read configFile", e);
		}
    }
}
