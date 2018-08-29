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
package com.diffplug.spotless.extra.eclipse.cdt;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.LogFilter;
import org.eclipse.equinox.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;
import org.osgi.service.log.LoggerConsumer;

/**
 * Simple log service for errors. 
 * The CDT formatter logs warnings for dedicated regional problems.
 * For example the CDT formatter logs a warning for standard C function provider
 * methods which do not use dedicated typedef for their return type.
 * The warnings do not contain any information about the type or source of problem.
 * Furthermore the other regions of the code(-line) are correctly formatted.
 * Hence the useless warnings are eaten. Just errors are logged (though it seems
 * that the formatter does not log any messages with an error level). 
 */
public class LogErrorService implements ExtendedLogService, ExtendedLogReaderService {

	@Override
	@Deprecated
	//Backward compatibility with Eclipse OSGI 3.12
	public void log(int level, String message) {}

	@Override
	@Deprecated
	//Backward compatibility with Eclipse OSGI 3.12
	public void log(int level, String message, Throwable exception) {
		log(level, message);
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	//Backward compatibility with Eclipse OSGI 3.12
	public void log(ServiceReference sr, int level, String message) {
		log(level, message);
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	//Backward compatibility with Eclipse OSGI 3.12
	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		log(level, message, exception);
	}

	@Override
	public void log(Object context, int level, String message) {
		log(level, message);
	}

	@SuppressWarnings("deprecation") ////Backward compatibility with Eclipse OSGI 3.12
	@Override
	public void log(Object context, int level, String message, Throwable exception) {
		LogLevel logLevel;
		switch (level) {
		case LogService.LOG_DEBUG:
			logLevel = LogLevel.DEBUG;
			break;
		case LogService.LOG_INFO:
			logLevel = LogLevel.INFO;
			break;
		case LogService.LOG_ERROR:
			logLevel = LogLevel.ERROR;
			break;
		case LogService.LOG_WARNING:
			logLevel = LogLevel.WARN;
			break;
		default:
			logLevel = LogLevel.AUDIT;
		}
		log(new SimpleLogEntry(logLevel, message, exception));
	}

	@Override
	public boolean isLoggable(int level) {
		return true;
	}

	@Override
	public String getName() {
		return LogErrorService.class.getSimpleName();
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
		//Nothing to do
	}

	@Override
	public void removeLogListener(LogListener listener) {
		//Nothing to do
	}

	public void log(LogEntry entry) {
		if (LogLevel.ERROR == entry.getLogLevel()) {
			System.err.println(entry.toString());
		}
	}

	@Override
	@Deprecated
	//Backward compatibility with Eclipse OSGI 3.12
	public Enumeration<LogEntry> getLog() {
		return Collections.emptyEnumeration(); //We do not provide historical information
	}

	@Override
	public void addLogListener(LogListener listener, LogFilter filter) {
		addLogListener(listener); //Listener must filter if required

	}

	@Override
	public org.osgi.service.log.Logger getLogger(Class<?> clazz) {
		return this;
	}

	@Override
	@Deprecated
	public <L extends org.osgi.service.log.Logger> L getLogger(String name, Class<L> loggerType) {
		throw new UnsupportedOperationException("Logger Factory currently not supported.");
	}

	@Override
	@Deprecated
	public <L extends org.osgi.service.log.Logger> L getLogger(Class<?> clazz, Class<L> loggerType) {
		return getLogger(getName(), loggerType);
	}

	@Override
	@Deprecated
	public <L extends org.osgi.service.log.Logger> L getLogger(Bundle bundle, String name, Class<L> loggerType) {
		return getLogger(getName(), loggerType);
	}

	@Override
	public boolean isTraceEnabled() {
		return false;
	}

	@Override
	public void trace(String message) {
		log(new SimpleLogEntry(LogLevel.TRACE, message));
	}

	@Override
	public void trace(String format, Object arg) {
		trace(String.format(format, arg));
	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		trace(String.format(format, arg1, arg2));
	}

	@Override
	public void trace(String format, Object... arguments) {
		trace(String.format(format, arguments));
	}

	@Override
	@Deprecated
	public <E extends Exception> void trace(LoggerConsumer<E> consumer) throws E {
		throw new UnsupportedOperationException("Logger Consumer currently not supported.");
	}

	@Override
	public boolean isDebugEnabled() {
		return false;
	}

	@Override
	public void debug(String message) {
		log(new SimpleLogEntry(LogLevel.DEBUG, message));
	}

	@Override
	public void debug(String format, Object arg) {
		debug(String.format(format, arg));
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		debug(String.format(format, arg1, arg2));
	}

	@Override
	public void debug(String format, Object... arguments) {
		trace(String.format(format, arguments));
	}

	@Override
	@Deprecated
	public <E extends Exception> void debug(LoggerConsumer<E> consumer) throws E {
		throw new UnsupportedOperationException("Logger Consumer currently not supported.");
	}

	@Override
	public boolean isInfoEnabled() {
		return false;
	}

	@Override
	public void info(String message) {
		log(new SimpleLogEntry(LogLevel.INFO, message));
	}

	@Override
	public void info(String format, Object arg) {
		info(String.format(format, arg));
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		info(String.format(format, arg1, arg2));
	}

	@Override
	public void info(String format, Object... arguments) {
		info(String.format(format, arguments));
	}

	@Override
	@Deprecated
	public <E extends Exception> void info(LoggerConsumer<E> consumer) throws E {
		throw new UnsupportedOperationException("Logger Consumer currently not supported.");
	}

	@Override
	public boolean isWarnEnabled() {
		return false;
	}

	@Override
	public void warn(String message) {
		log(new SimpleLogEntry(LogLevel.WARN, message));
	}

	@Override
	public void warn(String format, Object arg) {
		warn(String.format(format, arg));
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		warn(String.format(format, arg1, arg2));
	}

	@Override
	public void warn(String format, Object... arguments) {
		warn(String.format(format, arguments));
	}

	@Override
	@Deprecated
	public <E extends Exception> void warn(LoggerConsumer<E> consumer) throws E {
		throw new UnsupportedOperationException("Logger Consumer currently not supported.");
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public void error(String message) {
		log(new SimpleLogEntry(LogLevel.ERROR, message));
	}

	@Override
	public void error(String format, Object arg) {
		error(String.format(format, arg));
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		error(String.format(format, arg1, arg2));
	}

	@Override
	public void error(String format, Object... arguments) {
		error(String.format(format, arguments));
	}

	@Override
	@Deprecated
	public <E extends Exception> void error(LoggerConsumer<E> consumer) throws E {
		throw new UnsupportedOperationException("Logger Consumer currently not supported.");
	}

	@Override
	public void audit(String message) {
		log(new SimpleLogEntry(LogLevel.AUDIT, message));
	}

	@Override
	public void audit(String format, Object arg) {
		audit(String.format(format, arg));
	}

	@Override
	public void audit(String format, Object arg1, Object arg2) {
		audit(String.format(format, arg1, arg2));
	}

	@Override
	public void audit(String format, Object... arguments) {
		audit(String.format(format, arguments));
	}

	public static class SimpleLogEntry implements LogEntry {

		private final LogLevel level;
		private final String message;
		private final Optional<Throwable> execption;

		public SimpleLogEntry(LogLevel level, String message) {
			this(level, message, Optional.empty());
		}

		public SimpleLogEntry(LogLevel level, String message, Throwable execption) {
			this(level, message, Optional.ofNullable(execption));
		}

		private SimpleLogEntry(LogLevel level, String message, Optional<Throwable> execption) {
			this.level = level;
			this.message = message;
			this.execption = execption;
		}

		@Override
		public Bundle getBundle() {
			//Return the spotless framework bundle
			return InternalPlatform.getDefault().getBundleContext().getBundle();
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		@Override
		public ServiceReference getServiceReference() {
			return null;
		}

		@Override
		@Deprecated
		//Backward compatibility with Eclipse OSGI 3.12
		public int getLevel() {
			switch (level) {
			case DEBUG:
			case TRACE:
				return LogService.LOG_DEBUG;
			case AUDIT:
			case INFO:
				return LogService.LOG_INFO;
			case ERROR:
				return LogService.LOG_ERROR;
			case WARN:
				return LogService.LOG_WARNING;
			}
			return LogService.LOG_ERROR; //Don't fail here. Just log it as error. This is anyway just for debugging internal problems.
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

		@Override
		public LogLevel getLogLevel() {
			return level;
		}

		@Override
		public String getLoggerName() {
			return this.getClass().getSimpleName();
		}

		@Override
		public long getSequence() {
			return 0;
		}

		@Override
		public String getThreadInfo() {
			return null;
		}

		@Override
		public StackTraceElement getLocation() {
			return null;
		}

	}

}
