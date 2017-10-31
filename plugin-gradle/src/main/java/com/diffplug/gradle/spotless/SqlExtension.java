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
package com.diffplug.gradle.spotless;

import com.diffplug.spotless.sql.HibernateStep;

public class SqlExtension extends FormatExtension {
	static final String NAME = "sql";

	public SqlExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
	}

	public void hibernateSql() {
		hibernateSql(HibernateStep.defaultVersion());
	}

	public void hibernateSql(String version) {
		hibernate(version, HibernateStep.Kind.BASIC);
	}

	public void hibernateDdl() {
		hibernateDdl(HibernateStep.defaultVersion());
	}

	public void hibernateDdl(String version) {
		hibernate(version, HibernateStep.Kind.DDL);
	}

	private void hibernate(String version, HibernateStep.Kind kind) {
		this.addStep(HibernateStep.create(
				version,
				GradleProvisioner.fromProject(getProject()),
				kind));
	}
}
