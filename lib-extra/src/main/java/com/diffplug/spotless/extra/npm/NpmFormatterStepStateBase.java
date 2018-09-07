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
package com.diffplug.spotless.extra.npm;

import static com.diffplug.spotless.extra.npm.NpmExecutableResolver.tryFind;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.annotation.Nullable;

import com.diffplug.spotless.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public abstract class NpmFormatterStepStateBase implements Serializable {

	private static final long serialVersionUID = -5849375492831208496L;

	private final JarState jarState;

	@SuppressWarnings("unused")
	private final FileSignature nodeModulesSignature;

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	public final transient File nodeModulesDir;

	private final NpmConfig npmConfig;

	private final String stepName;

	protected NpmFormatterStepStateBase(String stepName, Provisioner provisioner, NpmConfig npmConfig, File buildDir, @Nullable File npm) throws IOException {
		this.stepName = requireNonNull(stepName);
		this.npmConfig = requireNonNull(npmConfig);
		this.jarState = JarState.from(j2v8MavenCoordinate(), requireNonNull(provisioner));

		this.nodeModulesDir = prepareNodeModules(buildDir, npm);
		this.nodeModulesSignature = FileSignature.signAsList(this.nodeModulesDir);
	}

	private File prepareNodeModules(File buildDir, @Nullable File npm) throws IOException {
		File targetDir = new File(buildDir, "spotless-node-modules-" + stepName);
		if (!targetDir.exists()) {
			if (!targetDir.mkdirs()) {
				throw new IOException("cannot create temp dir for node modules at " + targetDir);
			}
		}
		File packageJsonFile = new File(targetDir, "package.json");
		Files.write(packageJsonFile.toPath(), this.npmConfig.getPackageJsonContent().getBytes(StandardCharsets.UTF_8));
		runNpmInstall(npm, targetDir);
		return targetDir;
	}

	private void runNpmInstall(@Nullable File npm, File npmProjectDir) throws IOException {
		Process npmInstall = new ProcessBuilder()
				.inheritIO()
				.directory(npmProjectDir)
				.command(resolveNpm(npm).getAbsolutePath(), "install")
				.start();
		try {
			if (npmInstall.waitFor() != 0) {
				throw new IOException("Creating npm modules failed with exit code: " + npmInstall.exitValue());
			}
		} catch (InterruptedException e) {
			throw new IOException("Running npm install was interrupted.", e);
		}
	}

	private File resolveNpm(@Nullable File npm) {
		return Optional.ofNullable(npm)
				.orElseGet(() -> tryFind()
						.orElseThrow(() -> new IllegalStateException("cannot automatically determine npm executable and none was specifically supplied!")));
	}

	protected NodeJSWrapper nodeJSWrapper() {
		return new NodeJSWrapper(this.jarState.getClassLoader()); // TODO (simschla, 02.08.18): cache this instance
	}

	protected File nodeModulePath() {
		return new File(new File(this.nodeModulesDir, "node_modules"), this.npmConfig.getNpmModule());
	}

	private String j2v8MavenCoordinate() {
		return "com.eclipsesource.j2v8:j2v8_" + PlatformInfo.normalizedOSName() + "_" + PlatformInfo.normalizedArchName() + ":4.6.0";
	}

	protected static String readFileFromClasspath(Class<?> clazz, String name) {
		try {
			Path path = Paths.get(clazz.getResource(name).toURI());
			return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		} catch (URISyntaxException | IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	public abstract FormatterFunc createFormatterFunc();
}
