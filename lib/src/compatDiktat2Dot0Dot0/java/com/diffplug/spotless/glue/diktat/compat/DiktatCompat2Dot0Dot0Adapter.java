package com.diffplug.spotless.glue.diktat.compat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.saveourtool.diktat.DiktatFactoriesKt;
import com.saveourtool.diktat.DiktatProcessor;
import com.saveourtool.diktat.api.DiktatCallback;
import com.saveourtool.diktat.api.DiktatError;
import com.saveourtool.diktat.api.DiktatRuleConfig;
import com.saveourtool.diktat.api.DiktatRuleSet;

import kotlin.Unit;

import org.jetbrains.annotations.NotNull;

public class DiktatCompat2Dot0Dot0Adapter implements DiktatCompatAdapter {
	private final DiktatProcessor processor;
	private final DiktatCallback formatterCallback;

	public DiktatCompat2Dot0Dot0Adapter(File configFile) {
		this.processor = getDiktatReporter(configFile);
		this.formatterCallback = new FormatterCallback(new ArrayList<>());
	}

	@Override
	public String format(File file, String content, boolean isScript) {
		return processor.fix(
			file.toPath(),
			formatterCallback
		);
	}

	private static class FormatterCallback implements DiktatCallback {
		private final ArrayList<DiktatError> errors;

		FormatterCallback(ArrayList<DiktatError> errors) {
			this.errors = errors;
		}

		@Override
		public Unit invoke(DiktatError diktatError, Boolean corrected) {
			if (!corrected) {
				errors.add(diktatError);
			}
			return Unit.INSTANCE;
		}

		@Override
		public void invoke(@NotNull DiktatError diktatError, boolean corrected) {
			if (!corrected) {
				errors.add(diktatError);
			}
		}
	}

	private static DiktatProcessor getDiktatReporter(File configFile) {
		try {
			final InputStream configInputStream = new FileInputStream(configFile);
			final List<DiktatRuleConfig> ruleConfigs = DiktatFactoriesKt.getDiktatRuleConfigReader().invoke(configInputStream);
			final DiktatRuleSet ruleSet = DiktatFactoriesKt.getDiktatRuleSetFactory().invoke(ruleConfigs);
			return DiktatFactoriesKt.getDiktatProcessorFactory().invoke(ruleSet);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Config file doesn't exist", e);
		}
	}
}
