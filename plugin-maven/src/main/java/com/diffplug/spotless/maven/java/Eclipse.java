/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless.maven.java;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystemSession;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.extra.P2Mirror;
import com.diffplug.spotless.extra.java.EclipseJdtFormatterStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class Eclipse implements FormatterStepFactory {

	@Parameter
	private String file;

	@Parameter
	private String version;

	@Parameter
	private List<P2Mirror> p2Mirrors = new ArrayList<>();

	@Parameter
	private Boolean sortMembersDoNotSortFields = true;

	@Parameter
	private Boolean sortMembersEnabled = false;

	@Parameter
	private String sortMembersOrder;

	@Parameter
	private String sortMembersVisibilityOrder;

	@Parameter
	private Boolean sortMembersVisibilityOrderEnabled = false;

	private File cacheDirectory;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig stepConfig) {
		EclipseJdtFormatterStep.Builder eclipseConfig = EclipseJdtFormatterStep.createBuilder(stepConfig.getProvisioner());
		eclipseConfig.setVersion(version == null ? EclipseJdtFormatterStep.defaultVersion() : version);
		if (null != file) {
			File settingsFile = stepConfig.getFileLocator().locateFile(file);
			eclipseConfig.setPreferences(Arrays.asList(settingsFile));
		}
		eclipseConfig.setP2Mirrors(p2Mirrors);
		if (null != cacheDirectory) {
			eclipseConfig.setCacheDirectory(cacheDirectory);
		}
		if (null != sortMembersEnabled) {
			eclipseConfig.sortMembersEnabled(sortMembersEnabled);
		}
		if (null != sortMembersOrder) {
			eclipseConfig.sortMembersOrder(sortMembersOrder);
		}
		if (null != sortMembersDoNotSortFields) {
			eclipseConfig.sortMembersDoNotSortFields(sortMembersDoNotSortFields);
		}
		if (null != sortMembersVisibilityOrder) {
			eclipseConfig.sortMembersVisibilityOrder(sortMembersVisibilityOrder);
		}
		if (null != sortMembersVisibilityOrderEnabled) {
			eclipseConfig.sortMembersVisibilityOrderEnabled(sortMembersVisibilityOrderEnabled);
		}
		if (null != cacheDirectory) {
			eclipseConfig.setCacheDirectory(cacheDirectory);
		}
		return eclipseConfig.build();
	}

	@Override
	public void init(RepositorySystemSession repositorySystemSession) {
		this.cacheDirectory = repositorySystemSession.getLocalRepository().getBasedir();
	}
}
