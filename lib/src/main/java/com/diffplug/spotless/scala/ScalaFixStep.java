/*
 * Copyright 2016-2020 DiffPlug
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
package com.diffplug.spotless.scala;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public class ScalaFixStep {
	// prevent direct instantiation
	private ScalaFixStep() {}

	public static final String NAME = "scalafix";
	private static final String DEFAULT_VERSION = "0.9.16";
	private static final String DEFAULT_SCALA_VERSION = "2.12.11";
	private static final String MAVEN_COORDINATE = "ch.epfl.scala:scalafix-cli_%s:%s";
	private static final String SEMANTICDB_MAVEN_COORDINATE = "org.scalameta:semanticdb-scalac_%s:latest.release";

	public static void init(final ScalaCompiler scalaCompiler, final File projectDir) {
		final List<String> compilerOptions = Arrays.asList(
				"-Ywarn-unused",
				"-Yrangepos",
				"-P:semanticdb:synthetics:on",
				"-P:semanticdb:sourceroot:" + projectDir);

		scalaCompiler.enable();
		scalaCompiler.addPlugin(String.format(SEMANTICDB_MAVEN_COORDINATE, scalaCompiler.getVersion()));
		scalaCompiler.addCompilerOptions(compilerOptions);
	}

	public static FormatterStep create(final String version, final String scalaVersion, final Provisioner provisioner, final ScalaCompiler scalaCompiler, @Nullable final File configFile) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(scalaVersion, "scalaVersion");
		Objects.requireNonNull(provisioner, "provisioner");
		Objects.requireNonNull(scalaCompiler, "scalaCompiler");
		return FormatterStep.createLazy(NAME, () -> new State(version, scalaVersion, provisioner, scalaCompiler, configFile), State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	public static String defaultScalaVersion() {
		return DEFAULT_SCALA_VERSION;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		final JarState jarState;
		final FileSignature configSignature;
		final ScalaCompiler scalaCompiler;

		State(final String version, final String scalaVersion, final Provisioner provisioner, final ScalaCompiler scalaCompiler, @Nullable final File configFile) throws IOException {
			this.jarState = JarState.from(String.format(MAVEN_COORDINATE, scalaVersion, version), provisioner);
			this.configSignature = FileSignature.signAsList(configFile == null ? Collections.emptySet() : Collections.singleton(configFile));
			this.scalaCompiler = scalaCompiler;
		}

		FormatterFunc createFormat() throws Exception {
			final ClassLoader classLoader = jarState.getClassLoader();

			final Class<?> javaConversionsCls = classLoader.loadClass("scala.collection.JavaConversions");
			final Class<?> absolutePathCls = classLoader.loadClass("scala.meta.io.AbsolutePath");
			final Class<?> classpathCls = classLoader.loadClass("scala.meta.io.Classpath");
			final Class<?> someCls = classLoader.loadClass("scala.Some");
			final Class<?> argsCls = classLoader.loadClass("scalafix.internal.v1.Args");
			final Class<?> mainOpsCls = classLoader.loadClass("scalafix.internal.v1.MainOps");

			final Method absolutePathApply = absolutePathCls.getMethod("apply", File.class, absolutePathCls);
			final Method asScalaIterator = javaConversionsCls.getMethod("asScalaIterator", Iterator.class);
			final Method argsDefault = argsCls.getMethod("default", absolutePathCls, PrintStream.class);
			final Object workingDirectory = absolutePathCls.getMethod("workingDirectory").invoke(null);
			final Object scalacOptionsIter = asScalaIterator.invoke(null, scalaCompiler.getCompilerOptions().iterator());
			final Object scalacOptions = scalacOptionsIter.getClass().getMethod("toList").invoke(scalacOptionsIter);
			final Object classpath = classpathCls.getMethod("apply", String.class).invoke(null, scalaCompiler.getClasspath());

			final Method someApply = someCls.getMethod("apply", Object.class);

			return new FormatterFunc() {
				@Override
				public String apply(String input) {
					throw new RuntimeException("This method shouldn't be called.");
				}

				@Override
				public String apply(String input, File source) throws Exception {
					final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					final PrintStream out = new PrintStream(byteArrayOutputStream);
					final Object args = argsDefault.invoke(null, workingDirectory, out);
					setField(args, "stdout", true);
					setField(args, "scalacOptions", scalacOptions);
					setField(args, "classpath", classpath);
					if (!configSignature.files().isEmpty()) {
						final File file = configSignature.getOnlyFile();
						final Object config = absolutePathApply.invoke(null, file, workingDirectory);
						setField(args, "config", someApply.invoke(null, config));
					}

					final Object maybeValidatedArgs = argsCls.getMethod("validate").invoke(args);
					final Object validatedArgs = maybeValidatedArgs.getClass().getMethod("get").invoke(maybeValidatedArgs);
					final Method handleFile = mainOpsCls.getMethod("handleFile", validatedArgs.getClass(), absolutePathCls);
					final Object sourcePath = absolutePathApply.invoke(null, source, workingDirectory);
					handleFile.invoke(null, validatedArgs, sourcePath);
					final String output = byteArrayOutputStream.toString();
					// Remove the last new line added by println
					return output.substring(0, output.length() - 1);
				}
			};
		}
	}

	private static void setField(final Object obj, final String name, final Object value) throws Exception {
		final Field field = obj.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(obj, value);
	}
}
