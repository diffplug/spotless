/*
 * Copyright 2021-2024 DiffPlug
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
package com.diffplug.spotless.pom;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.RoundedStep;

public class SortPomStep implements RoundedStep {
	private static final long serialVersionUID = 1L;
	private static final String MAVEN_COORDINATE = "com.github.ekryd.sortpom:sortpom-sorter:";
	public static final String NAME = "sortPom";

	private final JarState.Promised jarState;
	private final SortPomCfg cfg;

	private SortPomStep(JarState.Promised jarState, SortPomCfg cfg) {
		this.jarState = jarState;
		this.cfg = cfg;
	}

	public static FormatterStep create(SortPomCfg cfg, Provisioner provisioner) {
		return FormatterStep.create(NAME,
				new SortPomStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATE + cfg.version, provisioner)), cfg),
				SortPomStep::equalityState,
				State::createFormat);
	}

	private State equalityState() {
		return new State(jarState.get(), cfg);
	}

	private static class State implements Serializable {
		private static final long serialVersionUID = 1;

		private final SortPomCfg cfg;
		private final JarState jarState;

		State(JarState jarState, SortPomCfg cfg) {
			this.jarState = jarState;
			this.cfg = cfg;
		}

		FormatterFunc createFormat() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
			Class<?> formatterFunc = jarState.getClassLoader().loadClass("com.diffplug.spotless.glue.pom.SortPomFormatterFunc");
			Constructor<?> constructor = formatterFunc.getConstructor(SortPomCfg.class);
			return (FormatterFunc) constructor.newInstance(cfg);
		}
	}
}
