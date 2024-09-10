package com.diffplug.spotless.rdf;

import java.io.File;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.diffplug.spotless.FormatExceptionPolicy;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.OnMatch;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.SerializableFileFilter;
import com.diffplug.spotless.npm.TsFmtFormatterStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdfFormatterStep implements Serializable{
	public static final String LATEST_TURTLE_FORMATTER_VERSION = "1.2.12";
	public static long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String TURTLE_FORMATTER_COORDINATES = "de.atextor:turtle-formatter" ;

	private final JarState.Promised jarState;
	private final Map<String, String> turtleFormatterStyle;
	private final RdfFormatterConfig config;

	public static FormatterStep create(RdfFormatterConfig config, Map<String, String> turtleOptions, Provisioner provisioner)
		throws ClassNotFoundException {
		JarState.Promised jarState;
		jarState = JarState.promise(() -> JarState.from(TURTLE_FORMATTER_COORDINATES + ":" + config.getTurtleFormatterVersion(), provisioner));
		RdfFormatterStep step = new RdfFormatterStep(jarState, config, turtleOptions);
		return FormatterStep.create("RdfFormatter", step, RdfFormatterStep::state, RdfFormatterStep::formatterFunc);
	}

	public static State state(RdfFormatterStep step){
		return new State(step.config, step.turtleFormatterStyle, step.jarState.get());
	}

	public static RdfFormatterFunc formatterFunc(State state) {
		return new RdfFormatterFunc(state);
	}


	public RdfFormatterStep(JarState.Promised jarState, RdfFormatterConfig config,
		Map<String, String> turtleFormatterStyle) {
		this.jarState = jarState;
		this.turtleFormatterStyle = turtleFormatterStyle;
		this.config = config;
	}


	static class State implements Serializable {
		public static final long serialVersionUID = 1L;

		private final RdfFormatterConfig config;

		private final Map<String, String> turtleFormatterStyle;

		private final JarState jarState;

		public State(RdfFormatterConfig config, Map<String, String> turtleFormatterStyle,
			JarState jarState) {
			this.config = config;
			this.turtleFormatterStyle = new TreeMap<>(turtleFormatterStyle == null ? Map.of() : turtleFormatterStyle);
			this.jarState = jarState;
		}

		public RdfFormatterConfig getConfig() {
			return config;
		}

		public Map<String, String> getTurtleFormatterStyle() {
			return turtleFormatterStyle;
		}

		public JarState getJarState() {
			return jarState;
		}

		@Override public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof State))
				return false;
			State state = (State) o;
			return Objects.equals(getConfig(), state.getConfig()) && Objects.equals(
				getTurtleFormatterStyle(), state.getTurtleFormatterStyle()) && Objects.equals(
				getJarState(), state.getJarState());
		}

		@Override public int hashCode() {
			return Objects.hash(getConfig(), getTurtleFormatterStyle(), getJarState());
		}
	}

}

