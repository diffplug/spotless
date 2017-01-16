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
package com.diffplug.spotless;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

class FormatExceptionPolicyLegacy implements FormatExceptionPolicy {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(Formatter.class.getName());

	@Override
	public void handleError(Throwable e, FormatterStep step, File file, Path rootDir) {
		if (e instanceof Error) {
			logger.severe("Step '" + step.getName() + "' found problem in '" + rootDir.relativize(file.toPath()) + "':\n" + e.getMessage());
			throw ((Error) e);
		} else {
			logger.log(Level.WARNING, "Unable to apply step '" + step.getName() + "' to '" + rootDir.relativize(file.toPath()), e);
		}
	}

	@Override
	public byte[] toBytes() {
		return LazyForwardingEquality.toBytes(this);
	}
}
