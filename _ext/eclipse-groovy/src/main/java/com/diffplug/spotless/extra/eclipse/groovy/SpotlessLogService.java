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
package com.diffplug.spotless.extra.eclipse.groovy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.LogFilter;
import org.eclipse.equinox.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

/** Simple log service. */
public class SpotlessLogService implements ExtendedLogService, ExtendedLogReaderService {

	public final static Set<Integer> QUIET_SEVERITIES = Collections.unmodifiableSet(new HashSet<Integer>(Arrays.asList(IStatus.INFO, IStatus.OK)));
	public final static Set<Integer> FATAL_SEVERITIES = Collections.unmodifiableSet(new HashSet<Integer>(Arrays.asList(IStatus.ERROR, IStatus.CANCEL)));

	private final Set<LogListener> listener = new HashSet<LogListener>();

	@Override
	public void log(int level, String message) {}

	@Override
	public void log(int level, String message, Throwable exception) {
		log(level, message);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void log(ServiceReference sr, int level, String message) {
		log(level, message);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		log(level, message, exception);
	}

	@Override
	public void log(Object context, int level, String message) {
		log(level, message);
	}

	@Override
	public void log(Object context, int level, String message, Throwable exception) {
		log(new SimpleLogEntry(level, message, exception));
	}

	@Override
	public boolean isLoggable(int level) {
		return !QUIET_SEVERITIES.contains(level);
	}

	@Override
	public String getName() {
		return SpotlessLogService.class.getSimpleName();
	}

	@Override
	public Logger getLogger(String loggerName) {
		return this;
	}

	@Override
	public Logger getLogger(Bundle bundle, String loggerName) {
		return this;
	}

	@Override
	public void addLogListener(LogListener listener) {
		synchronized (this.listener) {
			this.listener.add(listener);
		}
	}

	@Override
	public void removeLogListener(LogListener listener) {
		synchronized (this.listener) {
			this.listener.remove(listener);
		}
	}

	public void log(LogEntry entry) {
		if (QUIET_SEVERITIES.contains(entry.getLevel())) {
			return;
		}
		if (FATAL_SEVERITIES.contains(entry.getLevel())
				&& 0 != listener.size()) {
			System.err.println(entry.toString());
		}
		synchronized (listener) {
			listener.stream().forEach(l -> l.logged(entry));
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getLog() {
		return Collections.emptyEnumeration(); //We do not provide historical information
	}

	@Override
	public void addLogListener(LogListener listener, LogFilter filter) {
		addLogListener(listener); //Listener must filter if required

	}

	public static class SimpleLogEntry implements LogEntry {

		private final int level;
		private final String message;
		private final Optional<Throwable> execption;

		public SimpleLogEntry(int level, String message) {
			this(level, message, Optional.empty());
		}

		public SimpleLogEntry(int level, String message, Throwable execption) {
			this(level, message, Optional.of(execption));
		}

		private SimpleLogEntry(int level, String message, Optional<Throwable> execption) {
			this.level = level;
			this.message = message;
			this.execption = execption;
		}

		@Override
		public Bundle getBundle() {
			//Return the spotless framework bundle
			return InternalPlatform.getDefault().getBundleContext().getBundle();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public ServiceReference getServiceReference() {
			return null;
		}

		@Override
		public int getLevel() {
			return level;
		}

		@Override
		public String getMessage() {
			return message;
		}

		@Override
		public Throwable getException() {
			return execption.orElse(null);
		}

		@Override
		public long getTime() {
			return 0;
		}

		@Override
		public String toString() {
			StringWriter result = new StringWriter();
			result.write(message);
			if (execption.isPresent()) {
				result.write('\n');
				result.write(execption.get().toString());
				result.write('\n');
				execption.get().printStackTrace(new PrintWriter(result));
			}
			return result.toString();
		}

	}

}
