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
package com.diffplug.spotless.cli.steps;

import static com.diffplug.spotless.cli.core.FilePathUtil.asFile;
import static com.diffplug.spotless.cli.core.FilePathUtil.asFiles;
import static com.diffplug.spotless.cli.core.FilePathUtil.assertDirectoryExists;
import static com.diffplug.spotless.cli.steps.OptionDefaultUse.use;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.cli.core.ExecutionLayout;
import com.diffplug.spotless.cli.core.SpotlessActionContext;
import com.diffplug.spotless.npm.NpmPathResolver;
import com.diffplug.spotless.npm.PrettierConfig;
import com.diffplug.spotless.npm.PrettierFormatterStep;

import picocli.CommandLine;

@CommandLine.Command(name = "prettier", description = "Runs prettier")
public class Prettier extends SpotlessFormatterStep {

	@CommandLine.Option(names = {"--dev-dependency", "-D"}, description = "The devDependencies to use for Prettier.")
	Map<String, String> devDependencies;

	@CommandLine.Option(names = {"--cache-dir", "-C"}, description = "The directory to use for caching Prettier.")
	Path cacheDir;

	@CommandLine.Option(names = {"--npm-exec", "-n"}, description = "The explicit path to the npm executable.")
	Path explicitNpmExecutable;

	@CommandLine.Option(names = {"--node-exec", "-N"}, description = "The explicit path to the node executable.")
	Path explicitNodeExecutable;

	@CommandLine.Option(names = {"--npmrc-file", "-R"}, description = "The explicit path to the .npmrc file.")
	Path explicitNpmrcFile;

	@CommandLine.Option(names = {"--additional-npmrc-location", "-A"}, description = "Additional locations to search for .npmrc files.")
	List<Path> additionalNpmrcLocations;

	@CommandLine.Option(names = {"--prettier-config-path", "-P"}, description = "The path to the Prettier configuration file.")
	Path prettierConfigPath;

	@CommandLine.Option(names = {"--prettier-config-option", "-c"}, description = "The Prettier configuration options.")
	Map<String, Object> prettierConfigOptions;

	@Nonnull
	@Override
	public List<FormatterStep> prepareFormatterSteps(SpotlessActionContext context) {
		FormatterStep prettierFormatterStep = builder(context)
				.withDevDependencies(devDependencies())
				.withCacheDir(cacheDir)
				.withExplicitNpmExecutable(explicitNpmExecutable)
				.withExplicitNodeExecutable(explicitNodeExecutable)
				.withExplicitNpmrcFile(explicitNpmrcFile)
				.withAdditionalNpmrcLocations(additionalNpmrcLocations())
				.withPrettierConfigOptions(prettierConfigOptions)
				.withPrettierConfigPath(prettierConfigPath)
				.build();

		//		return List.of(adapt(prettierFormatterStep));

		return List.of(prettierFormatterStep);
	}

	private static FormatterStep adapt(FormatterStep step) {
		return new FormatterStep() {
			@Override
			public String getName() {
				return step.getName();
			}

			@Nullable
			@Override
			public String format(String rawUnix, File file) throws Exception {
				return step.format(rawUnix, file);
			}

			@Override
			public void close() throws Exception {
				step.close();
			}
		};
	}

	private Map<String, String> devDependencies() {
		return use(devDependencies).orIfNullGet(PrettierFormatterStep::defaultDevDependencies);
	}

	private List<Path> additionalNpmrcLocations() {
		return use(additionalNpmrcLocations).orIfNullGet(Collections::emptyList);
	}

	private PrettierFormatterStepBuilder builder(@Nonnull SpotlessActionContext context) {
		return new PrettierFormatterStepBuilder(context);
	}

	private class PrettierFormatterStepBuilder {

		@Nonnull
		private final SpotlessActionContext context;

		private Map<String, String> devDependencies;

		private Path cacheDir = null;

		// npmPathResolver
		private Path explicitNpmExecutable;

		private Path explicitNodeExecutable;

		private Path explicitNpmrcFile;

		private List<Path> additionalNpmrcLocations;

		// prettierConfig

		private Map<String, Object> prettierConfigOptions;

		private Path prettierConfigPath;

		private PrettierFormatterStepBuilder(@Nonnull SpotlessActionContext context) {
			this.context = Objects.requireNonNull(context);
		}

		public PrettierFormatterStepBuilder withDevDependencies(Map<String, String> devDependencies) {
			this.devDependencies = devDependencies;
			return this;
		}

		public PrettierFormatterStepBuilder withCacheDir(Path cacheDir) {
			this.cacheDir = cacheDir;
			return this;
		}

		public PrettierFormatterStepBuilder withExplicitNpmExecutable(Path explicitNpmExecutable) {
			this.explicitNpmExecutable = explicitNpmExecutable;
			return this;
		}

		public PrettierFormatterStepBuilder withExplicitNodeExecutable(Path explicitNodeExecutable) {
			this.explicitNodeExecutable = explicitNodeExecutable;
			return this;
		}

		public PrettierFormatterStepBuilder withExplicitNpmrcFile(Path explicitNpmrcFile) {
			this.explicitNpmrcFile = explicitNpmrcFile;
			return this;
		}

		public PrettierFormatterStepBuilder withAdditionalNpmrcLocations(List<Path> additionalNpmrcLocations) {
			this.additionalNpmrcLocations = additionalNpmrcLocations;
			return this;
		}

		public PrettierFormatterStepBuilder withPrettierConfigOptions(Map<String, Object> prettierConfigOptions) {
			this.prettierConfigOptions = prettierConfigOptions;
			return this;
		}

		public PrettierFormatterStepBuilder withPrettierConfigPath(Path prettierConfigPath) {
			this.prettierConfigPath = prettierConfigPath;
			return this;
		}

		public FormatterStep build() {
			ExecutionLayout layout = context.executionLayout();
			File projectDirFile = asFile(layout.find(Path.of("package.json")) // project dir
					.map(Path::getParent)
					.orElseGet(layout::baseDir));
			File buildDirFile = asFile(layout.buildDirFor(Prettier.this));
			File cacheDirFile = asFile(cacheDir);
			assertDirectoryExists(projectDirFile, buildDirFile, cacheDirFile);
			FormatterStep step = PrettierFormatterStep.create(
					use(devDependencies).orIfNullGet(PrettierFormatterStep::defaultDevDependencies),
					context.provisioner(),
					projectDirFile,
					buildDirFile,
					cacheDirFile,
					new NpmPathResolver(
							asFile(explicitNpmExecutable),
							asFile(explicitNodeExecutable),
							asFile(explicitNpmrcFile),
							asFiles(additionalNpmrcLocations)),
					new PrettierConfig(
							asFile(prettierConfigPath),
							prettierConfigOptions));
			return step;
		}

	}

}
