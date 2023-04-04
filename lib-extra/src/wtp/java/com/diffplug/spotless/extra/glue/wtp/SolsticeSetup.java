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
package com.diffplug.spotless.extra.glue.wtp;

import static org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames.CMDOCUMENT_GLOBAL_CACHE_ENABLED;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.osgi.framework.Constants;

import dev.equo.solstice.NestedJars;
import dev.equo.solstice.ShimIdeBootstrapServices;
import dev.equo.solstice.Solstice;
import dev.equo.solstice.p2.CacheLocations;

public class SolsticeSetup {
	static {
		NestedJars.setToWarnOnly();
		NestedJars.onClassPath().confirmAllNestedJarsArePresentOnClasspath(CacheLocations.nestedJars());
		try {
			var solstice = Solstice.findBundlesOnClasspath();
			solstice.warnAndModifyManifestsToFix();
			Map<String, String> props = Map.of("osgi.nl", "en_US",
					Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT,
					EquinoxLocations.PROP_INSTANCE_AREA, Files.createTempDirectory("spotless-wtp").toAbsolutePath().toString(),
					CMDOCUMENT_GLOBAL_CACHE_ENABLED, Boolean.toString(false));
			solstice.openShim(props);
			ShimIdeBootstrapServices.apply(props, solstice.getContext());
			solstice.start("org.apache.felix.scr");
			solstice.startAllWithLazy(false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static void init() {

	}
}
