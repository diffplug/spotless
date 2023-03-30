/*
 * Copyright 2016-2021 DiffPlug
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
package com.diffplug.spotless.extra.eclipse.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ObjectArrayAssert;
import org.assertj.core.api.StringAssert;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.ExtendedLogService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.slf4j.simple.SimpleLogger;

import com.diffplug.spotless.extra.eclipse.base.service.SingleSlf4JService;

/** Integration tests */
class SpotlessEclipseFrameworkTest {

	private final static String TEST_LOGGER_NAME = SpotlessEclipseFrameworkTest.class.getSimpleName();
	private final static String CUSTOM_PREFIX = "prefix\t";
	private final static String CUSTOM_POSTFIX = "\tpostfix";
	private final static String TEST_EXCEPTION_MESSAGE = "MY TEST-EXCEPTION";
	private static Slf4JMesssageListener SLF4J_RECEIVER = null;

	private static boolean TREAT_ERROR_AS_EXCEPTION = false;

	@BeforeAll
	static void frameworkTestSetup() throws BundleException {
		//Prepare interception of SLF4J messages to System.out
		SLF4J_RECEIVER = new Slf4JMesssageListener();

		//Configure SLF4J-Simple
		System.setProperty(SimpleLogger.LOG_FILE_KEY, "System.out");
		System.setProperty(SimpleLogger.SHOW_SHORT_LOG_NAME_KEY, "true");
		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "warn");

		//Instantiate default framework + SLF4J logger
		SpotlessEclipseFramework.setup(new SpotlessEclipseConfig() {
			@Override
			public void registerServices(SpotlessEclipseServiceConfig config) {
				config.applyDefault();
				config.useSlf4J(TEST_LOGGER_NAME, (s, l) -> {
					if (TREAT_ERROR_AS_EXCEPTION && (LogLevel.ERROR == l)) {
						throw new IllegalArgumentException(TEST_EXCEPTION_MESSAGE);
					}
					return CUSTOM_PREFIX + s + CUSTOM_POSTFIX;
				});
			}
		});
	}

	@AfterAll
	static void deregisterSlf4JReceiver() {
		SLF4J_RECEIVER.deregister();
	}

	private EclipseMessageListener eclipseMessageListener;

	@BeforeEach
	void eclipseReceiverSetup() {
		ExtendedLogReaderService service = getService(ExtendedLogReaderService.class);
		eclipseMessageListener = new EclipseMessageListener();
		service.addLogListener(eclipseMessageListener);
		SLF4J_RECEIVER.clear();
	}

	@AfterEach
	void logReceiverTearDown() {
		ExtendedLogReaderService service = getService(ExtendedLogReaderService.class);
		service.removeLogListener(eclipseMessageListener);
	}

	@Test
	void testCustomizedLogMessage() {
		ResourcesPlugin.getPlugin().getLog().log(new Status(IStatus.ERROR, "Some plugin", "Hello World!"));
		assertSlf4J()
				.as("Error message logged.").received("Hello World!").contains(TEST_LOGGER_NAME)
				.as("Customization method has been applied.").contains(CUSTOM_PREFIX, CUSTOM_POSTFIX)
				.as("Status level has been converted to simple SLF4J level").contains("ERROR");
		assertEclipse()
				.as("Warning message received.").received("Hello World!")
				.as("Customization method is only for SLF4J").doesNotContain(CUSTOM_PREFIX);
	}

	@Test
	void testCustomizedLogException() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> {
					try {
						TREAT_ERROR_AS_EXCEPTION = true;
						ResourcesPlugin.getPlugin().getLog().log(new Status(IStatus.ERROR, "Some plugin", "Hello World!"));
					} finally {
						TREAT_ERROR_AS_EXCEPTION = false;
					}
				})
				.withMessage(TEST_EXCEPTION_MESSAGE);
	}

	@Test
	void testPluginLog() {
		List<Integer> logLevels = Arrays.asList(IStatus.CANCEL, IStatus.ERROR, IStatus.INFO, IStatus.OK, IStatus.WARNING);
		List<Integer> enabledLogLevels = Arrays.asList(IStatus.ERROR, IStatus.WARNING);
		List<Integer> disabledLogLevels = logLevels.stream().filter(level -> !enabledLogLevels.contains(level)).collect(Collectors.toList());
		ILog logger = ResourcesPlugin.getPlugin().getLog();
		logLevels.forEach(logLevel -> logger.log(new Status(logLevel, "Some plugin", logLevel.toString())));
		assertSlf4J()
				.as("Messages for all enabled levels are logged.").received(
						enabledLogLevels.stream().map(i -> i.toString()).collect(Collectors.toList()));
		assertSlf4J()
				.as("Messages all disabled levels are not logged.").notReceived(
						disabledLogLevels.stream().map(i -> i.toString()).collect(Collectors.toList()));
		assertEclipse()
				.as("All messages received.").received(
						logLevels.stream().map(i -> i.toString()).collect(Collectors.toList()));
	}

	@Test
	void testLogServiceLevel() {
		ExtendedLogService service = getService(ExtendedLogService.class);
		assertThat(service.isErrorEnabled()).as("Error log level is enabled").isTrue();
		assertThat(service.isWarnEnabled()).as("Warning log level is enabled").isTrue();
		assertThat(service.isInfoEnabled()).as("Info log level is disabled").isFalse();
		assertThat(service.isDebugEnabled()).as("Debug log level is disabled").isFalse();
		assertThat(service.isTraceEnabled()).as("Trace log level is disabled").isFalse();
	}

	@Test
	void testLogServiceLog() {
		ExtendedLogService service = getService(ExtendedLogService.class);
		service.info("Log Info");
		service.warn("Log Warn");
		assertSlf4J().received("Log Warn");
		assertSlf4J().notReceived("Log Info");
		assertEclipse().received(Arrays.asList("Log Warn", "Log Info"));
	}

	@Test
	@Deprecated
	void testLogServiceLog_3_12() {
		ExtendedLogService service = getService(ExtendedLogService.class);
		service.log(SingleSlf4JService.LOG_INFO, "Log Info");
		try {
			throw new IllegalArgumentException(TEST_EXCEPTION_MESSAGE);
		} catch (Exception e) {
			service.log(SingleSlf4JService.LOG_WARNING, "Log Warn", e);
		}
		assertSlf4J().received(Arrays.asList("Log Warn", TEST_EXCEPTION_MESSAGE));
		assertSlf4J().notReceived("Log Info");
		assertEclipse().received(Arrays.asList("Log Warn", TEST_EXCEPTION_MESSAGE, "Log Info"));
	}

	private static <T> T getService(Class<T> serviceClass) {
		ResourcesPlugin plugin = ResourcesPlugin.getPlugin();
		assertThat(plugin).as("ResourcesPlugin instantiated as part of framework defaults").isNotNull();
		Bundle bundle = plugin.getBundle();
		assertThat(bundle).as("ResourcesPlugin has been started.").isNotNull();
		BundleContext context = bundle.getBundleContext();
		assertThat(context).as("ResourcesPlugin has been started.").isNotNull();
		ServiceReference<T> reference = context.getServiceReference(serviceClass);
		assertThat(reference).as(serviceClass.getSimpleName() + " has been registered.").isNotNull();
		T service = context.getService(reference);
		assertThat(service).as(serviceClass.getSimpleName() + " can be resolved.").isNotNull();
		return service;
	}

	private static interface IMessageListener {
		Collection<String> getMessages();
	}

	private static class EclipseMessageListener implements LogListener, IMessageListener {

		private final List<String> messages;

		public EclipseMessageListener() {
			messages = new ArrayList<String>();
		}

		@Override
		public Collection<String> getMessages() {
			return Collections.unmodifiableList(messages);
		}

		@Override
		public void logged(LogEntry entry) {
			messages.add(entry.getMessage());
			if (null != entry.getException()) {
				messages.add(entry.getException().getMessage());
			}
		}
	}

	private final static class Slf4JMesssageListener extends PrintStream implements IMessageListener {

		private final List<String> messages;
		private final PrintStream originalStream;

		public Slf4JMesssageListener() {
			super(System.out);
			messages = new ArrayList<String>();
			originalStream = System.out;
			System.setOut(this);
		}

		@Override
		public void println(String x) {
			if (x.contains(TEST_LOGGER_NAME)) {
				messages.add(x);
			} else {
				super.println(x);
			}
		}

		@Override
		public void println(Object x) {
			if (x instanceof Exception) {
				var e = (Exception) x;
				if (TEST_EXCEPTION_MESSAGE == e.getMessage()) {
					messages.add(TEST_EXCEPTION_MESSAGE);
				}
			}
			super.println(x);
		}

		public void deregister() {
			System.setOut(originalStream);
		}

		public void clear() {
			messages.clear();
		}

		@Override
		public Collection<String> getMessages() {
			return Collections.unmodifiableList(messages);
		}
	}

	private static class MessageListenerAssert<T extends IMessageListener> extends AbstractAssert<MessageListenerAssert<T>, T> {

		public MessageListenerAssert(T actual) {
			super(actual, MessageListenerAssert.class);
		}

		public ObjectArrayAssert<String> received(Collection<String> unformattedMessages) {
			List<String> formattedMessages = unformattedMessages.stream()
					.map(unformattedMessage -> getReceivedMessage(unformattedMessage, false))
					.collect(Collectors.toList());
			return new ObjectArrayAssert<String>(formattedMessages.toArray(new String[0]));
		}

		public StringAssert received(String unformattedMessage) {
			return new StringAssert(getReceivedMessage(unformattedMessage, false));
		}

		public MessageListenerAssert<T> notReceived(String unformattedMessage) {
			getReceivedMessage(unformattedMessage, true);
			return this;
		}

		public MessageListenerAssert<T> notReceived(Collection<String> unformattedMessages) {
			unformattedMessages.forEach(unformattedMessage -> getReceivedMessage(unformattedMessage, true));
			return this;
		}

		private String getReceivedMessage(String unformattedMessage, boolean negate) {
			isNotNull();
			String receivedMessage = null;
			for (String formattedMessage : actual.getMessages()) {
				if (formattedMessage.contains(unformattedMessage)) {
					receivedMessage = formattedMessage;
					break;
				}
			}
			if ((!negate) && (null == receivedMessage)) {
				failWithMessage("Message <%s> not received.", unformattedMessage);
			}
			if (negate && (null != receivedMessage)) {
				failWithMessage("Message <%s> has been received. Formatted message is: %s", unformattedMessage, receivedMessage);
			}
			return receivedMessage;
		}

	}

	private static MessageListenerAssert<Slf4JMesssageListener> assertSlf4J() {
		return new MessageListenerAssert<Slf4JMesssageListener>(SLF4J_RECEIVER).as("SLF4J");
	}

	private MessageListenerAssert<EclipseMessageListener> assertEclipse() {
		return new MessageListenerAssert<EclipseMessageListener>(eclipseMessageListener).as("Eclipse");
	}
}
