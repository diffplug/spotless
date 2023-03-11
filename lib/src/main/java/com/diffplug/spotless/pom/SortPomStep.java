/*
 * Copyright 2021-2023 DiffPlug
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

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public class SortPomStep {
	public static final String NAME = "sortPom";

	private SortPomStep() {}

	public static FormatterStep create(SortPomCfg cfg, Provisioner provisioner) {
		return FormatterStep.createLazy(NAME, () -> new State(cfg, provisioner), State::createFormat);
	}

	static class State implements Serializable {
		private static final long serialVersionUID = 1;

		final SortPomCfg cfg;
		JarState jarState;

		public State(SortPomCfg cfg, Provisioner provisioner) throws IOException {
			this.cfg = cfg;
			this.jarState = JarState.from("com.github.ekryd.sortpom:sortpom-sorter:3.0.0", provisioner);
		}

		FormatterFunc createFormat() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
			Class<?> formatterFunc = jarState.getClassLoader().loadClass("com.diffplug.spotless.glue.pom.SortPomFormatterFunc");
			Constructor<?> constructor = formatterFunc.getConstructor(SortPomCfg.class);
			return (FormatterFunc) constructor.newInstance(cfg);
		}
	}
}
