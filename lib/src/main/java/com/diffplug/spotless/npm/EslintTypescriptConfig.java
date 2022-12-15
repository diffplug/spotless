package com.diffplug.spotless.npm;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.ThrowingEx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;

public class EslintTypescriptConfig extends EslintConfig {

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	@Nullable
	private final transient File typescriptConfigPath;

	@SuppressWarnings("unused")
	private final FileSignature typescriptConfigPathSignature;

	public EslintTypescriptConfig(@Nullable File eslintConfigPath, @Nullable String eslintConfigJs, @Nullable File typescriptConfigPath) {
		super(eslintConfigPath, eslintConfigJs);
		try {
			this.typescriptConfigPath = typescriptConfigPath;
			this.typescriptConfigPathSignature = typescriptConfigPath != null ? FileSignature.signAsList(this.typescriptConfigPath) : FileSignature.signAsList();
		} catch (IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	@Override
	public EslintConfig withEslintConfigPath(@Nullable File eslintConfigPath) {
		return new EslintTypescriptConfig(eslintConfigPath, this.getEslintConfigJs(), this.typescriptConfigPath);
	}

	@Nullable
	public File getTypescriptConfigPath() {
		return typescriptConfigPath;
	}
}
