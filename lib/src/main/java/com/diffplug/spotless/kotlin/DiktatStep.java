/*
 * Copyright 2021 DiffPlug
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
package com.diffplug.spotless.kotlin;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import javax.annotation.Nullable;

import com.diffplug.spotless.*;

/** Wraps up [diktat](https://github.com/cqfn/diKTat) as a FormatterStep. */
public class DiktatStep {

	// prevent direct instantiation
	private DiktatStep() {}

	private static final String DEFAULT_VERSION = "0.4.0";
	static final String NAME = "diktat";
	static final String PACKAGE_DIKTAT = "org.cqfn.diktat";
	static final String PACKAGE_KTLINT = "com.pinterest.ktlint";
	static final String MAVEN_COORDINATE = PACKAGE_DIKTAT + ":diktat-rules:";

	public static String defaultVersionDiktat() {
		return DEFAULT_VERSION;
	}

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersionDiktat(), provisioner);
	}

	public static FormatterStep create(String versionDiktat, Provisioner provisioner) {
		return create(versionDiktat, provisioner, Collections.emptyMap(), null);
	}

	public static FormatterStep create(String versionDiktat, Provisioner provisioner, @Nullable FileSignature config) {
		return create(versionDiktat, provisioner, Collections.emptyMap(), config);
	}

	public static FormatterStep create(String versionDiktat, Provisioner provisioner, Map<String, String> userData, @Nullable FileSignature config) {
		return create(versionDiktat, provisioner, false, userData, config);
	}

	public static FormatterStep createForScript(String versionDiktat, Provisioner provisioner, @Nullable FileSignature config) {
		return createForScript(versionDiktat, provisioner, Collections.emptyMap(), config);
	}

	public static FormatterStep createForScript(String versionDiktat, Provisioner provisioner, Map<String, String> userData, @Nullable FileSignature config) {
		return create(versionDiktat, provisioner, true, userData, config);
	}

	public static FormatterStep create(String versionDiktat, Provisioner provisioner, boolean isScript, Map<String, String> userData, @Nullable FileSignature config) {
		Objects.requireNonNull(versionDiktat, "versionDiktat");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new DiktatStep.State(versionDiktat, provisioner, isScript, userData, config),
				DiktatStep.State::createFormat);
	}

	static final class State implements Serializable {

		private static final long serialVersionUID = 1L;

		/** Are the files being linted Kotlin script files. */
		private final boolean isScript;
		private final @Nullable FileSignature config;
		private final String pkg;
		private final String pkgKtlint;
		final JarState jar;
		private final TreeMap<String, String> userData;

		State(String versionDiktat, Provisioner provisioner, boolean isScript, Map<String, String> userData, @Nullable FileSignature config) throws IOException {

			HashSet<String> pkgSet = new HashSet<>();
			pkgSet.add(MAVEN_COORDINATE + versionDiktat);

			this.userData = new TreeMap<>(userData);
			this.pkg = PACKAGE_DIKTAT;
			this.pkgKtlint = PACKAGE_KTLINT;
			this.jar = JarState.from(pkgSet, provisioner);
			this.isScript = isScript;
			this.config = config;
		}

		FormatterFunc createFormat() throws Exception {

			ClassLoader classLoader = jar.getClassLoader();

			// first, we get the diktat rules
			if (config != null) {
				System.setProperty("diktat.config.path", config.getOnlyFile().getAbsolutePath());
			}

			Class<?> ruleSetProviderClass = classLoader.loadClass(pkg + ".ruleset.rules.DiktatRuleSetProvider");
			Object diktatRuleSet = ruleSetProviderClass.getMethod("get").invoke(ruleSetProviderClass.newInstance());
			Iterable<?> ruleSets = Collections.singletonList(diktatRuleSet);

			// next, we create an error callback which throws an assertion error when the format is bad
			Class<?> function2Interface = classLoader.loadClass("kotlin.jvm.functions.Function2");
			Class<?> lintErrorClass = classLoader.loadClass(pkgKtlint + ".core.LintError");
			Method detailGetter = lintErrorClass.getMethod("getDetail");
			Method lineGetter = lintErrorClass.getMethod("getLine");
			Method colGetter = lintErrorClass.getMethod("getCol");

			// grab the KtLint singleton
			Class<?> ktlintClass = classLoader.loadClass(pkgKtlint + ".core.KtLint");
			Object ktlint = ktlintClass.getDeclaredField("INSTANCE").get(null);

			Class<?> paramsClass = classLoader.loadClass(pkgKtlint + ".core.KtLint$Params");
			// and its constructor
			Constructor<?> constructor = paramsClass.getConstructor(
					/* fileName, nullable */ String.class,
					/* text */ String.class,
					/* ruleSets */ Iterable.class,
					/* userData */ Map.class,
					/* callback */ function2Interface,
					/* script */ boolean.class,
					/* editorConfigPath, nullable */ String.class,
					/* debug */ boolean.class);
			Method formatterMethod = ktlintClass.getMethod("format", paramsClass);
			FormatterFunc.NeedsFile formatterFunc = (input, file) -> {
				ArrayList<Object> errors = new ArrayList<>();

				Object formatterCallback = Proxy.newProxyInstance(classLoader, new Class[]{function2Interface},
						(proxy, method, args) -> {
							Object lintError = args[0]; //ktlint.core.LintError
							boolean corrected = (Boolean) args[1];
							if (!corrected) {
								errors.add(lintError);
							}
							return null;
						});

				userData.put("file_path", file.getAbsolutePath());
				try {
					Object params = constructor.newInstance(
							/* fileName, nullable */ file.getName(),
							/* text */ input,
							/* ruleSets */ ruleSets,
							/* userData */ userData,
							/* callback */ formatterCallback,
							/* script */ isScript,
							/* editorConfigPath, nullable */ null,
							/* debug */ false);
					String result = (String) formatterMethod.invoke(ktlint, params);
					if (!errors.isEmpty()) {
						StringBuilder error = new StringBuilder("");
						error.append("There are ").append(errors.size()).append(" unfixed errors:");
						for (Object er : errors) {
							String detail = (String) detailGetter.invoke(er);
							int line = (Integer) lineGetter.invoke(er);
							int col = (Integer) colGetter.invoke(er);

							error.append(System.lineSeparator()).append("Error on line: ").append(line).append(", column: ").append(col).append(" cannot be fixed automatically")
									.append(System.lineSeparator()).append(detail);
						}
						throw new AssertionError(error);
					}
					return result;
				} catch (InvocationTargetException e) {
					throw ThrowingEx.unwrapCause(e);
				}
			};

			return formatterFunc;
		}

	}

}
