/*
 * Copyright 2016-2022 DiffPlug
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

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.java.ImportOrderStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class ImportOrder implements FormatterStepFactory {
	@Parameter
	private String file;

	@Parameter
	private String order;

	@Parameter
	private boolean wildcardsLast = false;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		if (file != null ^ order != null) {
			if (file != null) {
				File importsFile = config.getFileLocator().locateFile(file);
				return ImportOrderStep.forJava().createFrom(wildcardsLast, importsFile);
			} else {
				return ImportOrderStep.forJava().createFrom(wildcardsLast, order.split(",", -1));
			}
		} else if (file == null && order == null) {
			return ImportOrderStep.forJava().createFrom(wildcardsLast);
		} else {
			throw new IllegalArgumentException("Must specify exactly one of 'file' or 'order'.");
		}
	}
}
