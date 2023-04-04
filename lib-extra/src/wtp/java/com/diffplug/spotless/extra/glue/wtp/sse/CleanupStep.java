/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.extra.glue.wtp.sse;

import org.eclipse.wst.sse.core.internal.cleanup.AbstractStructuredCleanupProcessor;

/**
 * Common base class for step implementations based on an SSE cleanup processor.
 * <p>
 * Some WTP formatters do not apply all formatting within a formatter process,
 * but provide a cleanup and a formatter process, whereas the cleanup process
 * calls the formatter process.
 * </p>
 * <p>
 * Not all cleanup processes provided by WTP are suitable for Spotless formatting.
 * For example the XML cleanup processor focus on syntax and line delimiter
 * corrections. The syntax correction is based on the corresponding XSD/DTD,
 * but it must be assumed by the Spotless formatter that the provided files
 * are valid. It cannot be the purpose of a formatter to correct syntax.
 * A Java formatter should also not attempt to correct compilation errors.
 * A line delimiter correction would furthermore violate the Spotless rule
 * that the strings processed by a Spotless formatter chain must use
 * UNIX style delimiters.
 * </p>
 * @see org.eclipse.wst.sse.core.internal.cleanup.AbstractStructuredCleanupProcessor
 */
public class CleanupStep {

	/** Make some of the protected SEE AbstractStructuredCleanupProcessor methods public. */
	public interface ProcessorAccessor {

		/** Configure from Eclipse framework preferences */
		void refreshPreferences();

		/** Get underlying processor */
		AbstractStructuredCleanupProcessor get();

		/** Get processor content type */
		String getTypeId();
	}

	protected final ProcessorAccessor processorAccessor;

	protected CleanupStep(ProcessorAccessor processorAccessor) throws Exception {
		this.processorAccessor = processorAccessor;
		this.processorAccessor.refreshPreferences();
		/*
		 *  Don't refresh the preferences every time a clone of the processor is created.
		 *  All processors shall use the preferences of its parent.
		 */
		processorAccessor.get().refreshCleanupPreferences = false;
	}

	/**
	 * Calls the cleanup and formatting task of the processor and returns the formatted string.
	 * @param raw Dirty string
	 * @return Formatted string
	 * @throws Exception All exceptions are considered fatal to the build process (Gradle, Maven, ...) and should not be caught.
	 */
	public String format(String raw) throws Exception {
		return processorAccessor.get().cleanupContent(raw);
	}
}
