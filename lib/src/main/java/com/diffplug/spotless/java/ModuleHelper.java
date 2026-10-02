/*
 * Copyright 2016-2026 DiffPlug
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
package com.diffplug.spotless.java;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.Jvm;

import sun.misc.Unsafe;

/**
 * Some Java formatters (google-java-format, palantir-java-format, cleanthat) need access to
 * the internal packages of the JDK compiler module. Instead of hardcoding those package names,
 * which are JDK internals that may be renamed or removed, we enumerate and open every package
 * of that module.
 */
final class ModuleHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModuleHelper.class);

	/** The module which owns the packages that the Java formatters above require. */
	private static final String REQUIRED_MODULE = "jdk.compiler";

	// prevent direct instantiation
	private ModuleHelper() {}

	private static boolean checkDone;

	public static synchronized void doOpenInternalPackagesIfRequired() {
		if (Jvm.version() < 16 || checkDone) {
			return;
		}
		try {
			checkDone = true;
			final List<String> unavailableRequiredPackages = unavailableRequiredPackages();
			if (!unavailableRequiredPackages.isEmpty()) {
				openPackages(unavailableRequiredPackages);
				final List<String> failedToOpen = unavailableRequiredPackages();
				if (!failedToOpen.isEmpty()) {
					final StringBuilder message = new StringBuilder();
					message.append("WARNING: Some required internal classes are unavailable. Please consider adding the following JVM arguments\n");
					for (String name : failedToOpen) {
						message.append("WARNING: --add-opens %s/%s=ALL-UNNAMED%n".formatted(REQUIRED_MODULE, name));
					}
					LOGGER.warn("{}", message);
				}
			}
		} catch (Throwable e) {
			LOGGER.error("WARNING: Failed to check for available JDK packages.", e);
		}
	}

	/** @return the packages of {@link #REQUIRED_MODULE} which are not open to the module calling this code. */
	private static List<String> unavailableRequiredPackages() {
		final Module callerModule = ModuleHelper.class.getModule();
		final List<String> packages = new ArrayList<>();
		for (Module module : requiredModules()) {
			for (String name : module.getPackages()) {
				if (!module.isOpen(name, callerModule)) {
					packages.add(name);
				}
			}
		}
		return packages;
	}

	private static List<Module> requiredModules() {
		final List<Module> modules = new ArrayList<>();
		for (Module module : ModuleLayer.boot().modules()) {
			if (REQUIRED_MODULE.equals(module.getName())) {
				modules.add(module);
			}
		}
		return modules;
	}

	private static void openPackages(Collection<String> packagesToOpen) throws Throwable {
		final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		final Unsafe unsafe = (Unsafe) unsafeField.get(null);
		final Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
		final MethodHandles.Lookup lookup = (MethodHandles.Lookup) unsafe.getObject(
				unsafe.staticFieldBase(implLookupField),
				unsafe.staticFieldOffset(implLookupField));
		final MethodHandle modifiers = lookup.findSetter(Method.class, "modifiers", Integer.TYPE);
		final Method addOpensMethod = Module.class.getDeclaredMethod("implAddOpens", String.class);
		modifiers.invokeExact(addOpensMethod, Modifier.PUBLIC);
		for (Module module : requiredModules()) {
			for (String name : module.getPackages()) {
				if (packagesToOpen.contains(name)) {
					addOpensMethod.invoke(module, name);
				}
			}
		}
	}
}
