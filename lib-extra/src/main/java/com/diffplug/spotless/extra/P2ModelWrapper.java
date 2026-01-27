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
package com.diffplug.spotless.extra;

import java.util.Collection;
import java.util.Set;

import dev.equo.solstice.p2.P2Model;

/**
 * Wrapper for P2Model that exposes only the data needed for caching,
 * without leaking the P2Model type to consumers.
 */
public final class P2ModelWrapper {
	private final P2Model model;

	private P2ModelWrapper(P2Model model) {
		this.model = model;
	}

	public static P2ModelWrapper wrap(P2Model model) {
		return new P2ModelWrapper(model);
	}

	public P2Model unwrap() {
		return model;
	}

	public Collection<String> getP2Repos() {
		return model.getP2repo();
	}

	public Collection<String> getInstallList() {
		return model.getInstall();
	}

	public Set<String> getFilterNames() {
		return model.getFilters().keySet();
	}

	public Collection<String> getPureMaven() {
		return model.getPureMaven();
	}

	public boolean isUseMavenCentral() {
		return model.useMavenCentral;
	}
}
