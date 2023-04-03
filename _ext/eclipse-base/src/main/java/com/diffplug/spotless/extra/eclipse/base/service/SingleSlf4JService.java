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
package com.diffplug.spotless.extra.eclipse.base.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

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
import org.osgi.service.log.LoggerConsumer;

/**
 * Eclipse log service facade that delegates to a Slf4J logger.
 * The service does not provide historical log entries (empty history reported).
 * No factory service is provided for OSGI logger extensions (method are marked
 * as deprecated and raise UnsupportedOperationException).
 * All Eclipse logger are delegated to a single Slf4J logger instance.
 * The log messages can be formatted by customizer.
 */
public class SingleSlf4JService implements ExtendedLogService, ExtendedLogReaderService {

	private final org.slf4j.Logger delegate;
	private final Map<LogLevel, LogMethods> logLevel2methods;
	private final Set<LogListener> listener;
	private final BiFunction<String, LogLevel, String> messageCustomizer;

	/** Create facade for named logger with customized messages. */
	public SingleSlf4JService(String name, BiFunction<String, LogLevel, String> messageCustomizer) {
		delegate = org.slf4j.LoggerFactory.getLogger(name);
		logLevel2methods = new HashMap<LogLevel, LogMethods>();
		/*
		 * Audit message are treated as normal info-messages and might not get logged.
		 * Logging of Eclipse messages in Spotless formatter is meant for debugging purposes and
		 * detection of erroneous usage/override of internal Eclipse methods.
		 * Hence the concept of Audit is not required.
		 */
		logLevel2methods.put(LogLevel.AUDIT,
				create(() -> true, m -> delegate.info(m), (m, e) -> delegate.info(m, e)));
		logLevel2methods.put(LogLevel.DEBUG,
				create(() -> delegate.isDebugEnabled(), m -> delegate.debug(m), (m, e) -> delegate.debug(m, e)));
		logLevel2methods.put(LogLevel.ERROR,
				create(() -> delegate.isErrorEnabled(), m -> delegate.error(m), (m, e) -> delegate.error(m, e)));
		logLevel2methods.put(LogLevel.INFO,
				create(() -> delegate.isInfoEnabled(), m -> delegate.info(m), (m, e) -> delegate.info(m, e)));
		logLevel2methods.put(LogLevel.TRACE,
				create(() -> delegate.isTraceEnabled(), m -> delegate.trace(m), (m, e) -> delegate.trace(m, e)));
		logLevel2methods.put(LogLevel.WARN,
				create(() -> delegate.isWarnEnabled(), m -> delegate.warn(m), (m, e) -> delegate.warn(m, e)));
		listener = new HashSet<LogListener>();
		this.messageCustomizer = messageCustomizer;
	}

	@Override
	@Deprecated
	//Backward compatibility with Eclipse OSGI 3.12
	public void log(int level, String message) {
		log(this, level, message);
	}

	@Override
	@Deprecated
	//Backward compatibility with Eclipse OSGI 3.12
	public void log(int level, String message, @Nullable Throwable exception) {
		log(this, level, message, exception);
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	//Backward compatibility with Eclipse OSGI 3.12
	public void log(ServiceReference sr, int level, String message) {
		log(this, level, message);
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	//Backward compatibility with Eclipse OSGI 3.12
	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		log(this, level, message, exception);
	}

	@Override
	public void log(Object context, int level, String message) {
		log(context, level, message, null);
	}

	@Override
	public void log(Object context, int level, String message, @Nullable Throwable exception) {
		LogLevel logLevel = convertDeprectatedOsgiLevel(level);
		log(new SimpleLogEntry(logLevel, message, exception));
	}

	@Override
	public boolean isLoggable(int level) {
		LogLevel logLevel = convertDeprectatedOsgiLevel(level);
		return logLevel2methods.get(logLevel).isEnabled();
	}

	@SuppressWarnings("deprecation") ////Backward compatibility with Eclipse OSGI 3.12
	private static LogLevel convertDeprectatedOsgiLevel(int level) {
		switch (level) {
		case SingleSlf4JService.LOG_DEBUG:
			return LogLevel.DEBUG;
		case SingleSlf4JService.LOG_INFO:
			return LogLevel.INFO;
		case SingleSlf4JService.LOG_ERROR:
			return LogLevel.ERROR;
		case SingleSlf4JService.LOG_WARNING:
			return LogLevel.WARN;
		default:
			return LogLevel.AUDIT;
		}
	}

	@Override
	public String getName() {
		return delegate.getName();
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

	private void log(LogEntry entry) {
		synchronized (listener) {
			listener.stream().forEach(l -> l.logged(entry));
		}
		String customMessage = messageCustomizer.apply(entry.getMessage(), entry.getLogLevel());
		logLevel2methods.get(entry.getLogLevel()).log(customMessage, entry.getException());
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
		throw new UnsupportedOperationException("Logger factory for indifivaul types currently not supported.");
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
		return delegate.isTraceEnabled();
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
	public <E extends Exception> void trace(LoggerConsumer<E> consumer) throws E {
		consumer.accept(this);
	}

	@Override
	public boolean isDebugEnabled() {
		return delegate.isDebugEnabled();
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
		debug(String.format(format, arguments));
	}

	@Override
	public <E extends Exception> void debug(LoggerConsumer<E> consumer) throws E {
		consumer.accept(this);
	}

	@Override
	public boolean isInfoEnabled() {
		return delegate.isInfoEnabled();
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
	public <E extends Exception> void info(LoggerConsumer<E> consumer) throws E {
		consumer.accept(this);
	}

	@Override
	public boolean isWarnEnabled() {
		return delegate.isWarnEnabled();
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
	public <E extends Exception> void warn(LoggerConsumer<E> consumer) throws E {
		consumer.accept(this);
	}

	@Override
	public boolean isErrorEnabled() {
		return delegate.isErrorEnabled();
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
	public <E extends Exception> void error(LoggerConsumer<E> consumer) throws E {
		consumer.accept(this);
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

	/** Internal wrapper for Eclipse OSGI 3.12 based logs and new log services. */
	private static class SimpleLogEntry implements LogEntry {

		private final LogLevel level;
		private final String message;
		private final Optional<Throwable> execption;

		public SimpleLogEntry(LogLevel level, String message) {
			this(level, message, Optional.empty());
		}

		public SimpleLogEntry(LogLevel level, String message, @Nullable Throwable execption) {
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
				return SingleSlf4JService.LOG_DEBUG;
			case AUDIT:
			case INFO:
				return SingleSlf4JService.LOG_INFO;
			case ERROR:
				return SingleSlf4JService.LOG_ERROR;
			case WARN:
				return SingleSlf4JService.LOG_WARNING;
			}
			return SingleSlf4JService.LOG_ERROR; //Don't fail here. Just log it as error. This is anyway just for debugging internal problems.
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
			var result = new StringWriter();
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
			return null; // Not used by SingleSlf4JService
		}

	}

	private static LogMethods create(Supplier<Boolean> enabled, Consumer<String> log, BiConsumer<String, Throwable> logException) {
		return new LogMethods(enabled, log, logException);
	}

	private static class LogMethods {
		private final Supplier<Boolean> enabled;
		private final Consumer<String> log;
		private final BiConsumer<String, Throwable> logException;

		private LogMethods(Supplier<Boolean> enabled, Consumer<String> log, BiConsumer<String, Throwable> logException) {
			this.enabled = enabled;
			this.log = log;
			this.logException = logException;
		}

		public boolean isEnabled() {
			return enabled.get();
		}

		public void log(String message) {
			log.accept(message);
		}

		public void log(String message, @Nullable Throwable exception) {
			if (null == exception) {
				log(message);
			} else {
				logException.accept(message, exception);
			}
		}

	};

}
