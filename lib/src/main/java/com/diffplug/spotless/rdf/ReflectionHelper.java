/*
 * Copyright 2024-2025 DiffPlug
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
package com.diffplug.spotless.rdf;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReflectionHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final RdfFormatterStep.State state;
	private final ClassLoader classLoader;
	private final Class<?> JenaRdfParserClass;
	private final Class<?> JenaRdfParserBuilderClass;
	private final Class<?> JenaErrorHandlerClass;
	private final Class<?> JenaModelClass;
	private final Class<?> JenaStmtIteratorClass;
	private final Class<?> JenaRDFNodeClass;
	private final Class<?> JenaResourceClass;
	private final Class<?> JenaPropertyClass;
	private final Class<?> JenaModelFactoryClass;
	private final Class<?> JenaLangClass;
	private final Class<?> JenaGraphClass;
	private final Class<?> JenaTriple;
	private final Class<?> TurtleFormatFormattingStyleClass;
	private final Class<?> TurtleFormatFormattingStyleBuilderClass;
	private final Class<?> TurtleFormatFormatterClass;
	private final Class<?> TurtleFormatKnownPrefix;

	private final Method graphStream;
	private final Method graphFindTriple;
	private final Method contains;
	private final Method getGraph;
	private final Method tripleGetObject;

	private final Object jenaModelInstance;

	public ReflectionHelper(RdfFormatterStep.State state)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		this.state = state;
		this.classLoader = state.getJarState().getClassLoader();
		this.JenaRdfParserClass = classLoader.loadClass("org.apache.jena.riot.RDFParser");
		this.JenaRdfParserBuilderClass = classLoader.loadClass("org.apache.jena.riot.RDFParserBuilder");
		this.JenaErrorHandlerClass = classLoader.loadClass("org.apache.jena.riot.system.ErrorHandler");
		this.JenaModelClass = classLoader.loadClass("org.apache.jena.rdf.model.Model");
		this.JenaStmtIteratorClass = classLoader.loadClass("org.apache.jena.rdf.model.StmtIterator");
		this.JenaRDFNodeClass = classLoader.loadClass("org.apache.jena.rdf.model.RDFNode");
		this.JenaResourceClass = classLoader.loadClass("org.apache.jena.rdf.model.Resource");
		this.JenaPropertyClass = classLoader.loadClass("org.apache.jena.rdf.model.Property");
		this.JenaModelFactoryClass = classLoader.loadClass("org.apache.jena.rdf.model.ModelFactory");
		this.JenaLangClass = classLoader.loadClass("org.apache.jena.riot.Lang");
		this.TurtleFormatFormatterClass = classLoader.loadClass("de.atextor.turtle.formatter.TurtleFormatter");
		this.TurtleFormatFormattingStyleClass = classLoader.loadClass("de.atextor.turtle.formatter.FormattingStyle");
		Class<?>[] innerClasses = TurtleFormatFormattingStyleClass.getDeclaredClasses();
		this.TurtleFormatFormattingStyleBuilderClass = Arrays.stream(innerClasses)
				.filter(c -> "FormattingStyleBuilder".equals(c.getSimpleName())).findFirst().orElseThrow();
		this.TurtleFormatKnownPrefix = Arrays.stream(innerClasses).filter(c -> "KnownPrefix".equals(c.getSimpleName())).findFirst().orElseThrow();
		this.getGraph = JenaModelClass.getMethod("getGraph");
		this.JenaGraphClass = classLoader.loadClass("org.apache.jena.graph.Graph");
		this.JenaTriple = classLoader.loadClass("org.apache.jena.graph.Triple");
		this.graphFindTriple = JenaGraphClass.getMethod("find", JenaTriple);
		this.graphStream = JenaGraphClass.getMethod("stream");
		this.tripleGetObject = JenaTriple.getMethod("getObject");
		this.contains = JenaGraphClass.getMethod("contains", JenaTriple);
		this.jenaModelInstance = JenaModelFactoryClass.getMethod("createDefaultModel").invoke(JenaModelFactoryClass);
	}

	public Object getLang(String lang) throws NoSuchFieldException, IllegalAccessException {
		return this.JenaLangClass.getDeclaredField(lang).get(this.JenaLangClass);
	}

	public Object getModel() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		return JenaModelFactoryClass.getMethod("createDefaultModel").invoke(JenaModelFactoryClass);
	}

	public Object getErrorHandler(File file) {
		return Proxy.newProxyInstance(this.classLoader,
				new Class<?>[]{JenaErrorHandlerClass}, new DynamicErrorInvocationHandler(file));
	}

	public Object next(Object statementIterator)
			throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		Method hasNext = JenaStmtIteratorClass.getMethod("next");
		return hasNext.invoke(statementIterator);
	}

	public boolean graphContainsSameTerm(Object graph, Object triple) throws InvocationTargetException, IllegalAccessException {
		boolean found = (boolean) contains.invoke(graph, triple);
		if (!found) {
			return false;
		}
		Iterator<Object> it = (Iterator<Object>) graphFindTriple.invoke(graph, triple);
		while (it.hasNext()) {
			Object foundTriple = it.next();
			Object foundObject = tripleGetObject.invoke(foundTriple);
			Object searchedObject = tripleGetObject.invoke(triple);
			if (!foundObject.equals(searchedObject)) {
				return false;
			}
		}
		return true;
	}

	private class DynamicErrorInvocationHandler implements InvocationHandler {
		private final String filePath;

		public DynamicErrorInvocationHandler(File file) {
			if (state.getConfig().isFailOnWarning()) {
				this.filePath = null;
			} else {
				this.filePath = file.getPath();
			}
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String message = (String) args[0];
			long line = (long) args[1];
			long col = (long) args[2];
			String severity = method.getName();
			if ("warning".equals(severity) && !state.getConfig().isFailOnWarning()) {
				LOGGER.warn("{}({},{}): {}", this.filePath, line, col, message);
			} else {
				if ("warning".equals(severity)) {
					LOGGER.error("Formatter fails because of a parser warning. To make the formatter succeed in"
							+ "the presence of warnings, set the configuration parameter 'failOnWarning' to 'false' (default: 'true')");
				}
				throw new RuntimeException(
						"line %d, col %d: %s (severity: %s)".formatted(line, col, message, severity));
			}
			return null;
		}
	}

	public Object getParser(Object lang, Object errorHandler, String rawUnix)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Object parserBuilder = JenaRdfParserClass.getMethod("create").invoke(JenaRdfParserClass);
		parserBuilder = JenaRdfParserBuilderClass.getMethod("errorHandler", JenaErrorHandlerClass)
				.invoke(parserBuilder, errorHandler);
		parserBuilder = JenaRdfParserBuilderClass.getMethod("forceLang", JenaLangClass).invoke(parserBuilder, lang);
		parserBuilder = JenaRdfParserBuilderClass.getMethod("strict", Boolean.TYPE).invoke(parserBuilder, true);
		parserBuilder = JenaRdfParserBuilderClass.getMethod("checking", Boolean.TYPE).invoke(parserBuilder, true);
		parserBuilder = JenaRdfParserBuilderClass.getMethod("fromString", String.class).invoke(parserBuilder, rawUnix);
		return JenaRdfParserBuilderClass.getMethod("build").invoke(parserBuilder);
	}

	public void parseModel(Object parser, Object model)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		JenaRdfParserClass.getMethod("parse", JenaModelClass).invoke(parser, model);
	}

	public Object getGraph(Object model) throws InvocationTargetException, IllegalAccessException {
		return getGraph.invoke(model);
	}

	public Stream<Object> streamGraph(Object graph) throws InvocationTargetException, IllegalAccessException {
		return (Stream<Object>) graphStream.invoke(graph);

	}

	private Object newTurtleFormatterStyle() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Object builder = TurtleFormatFormattingStyleClass.getMethod("builder").invoke(TurtleFormatFormatterClass);
		for (String optionName : state.getTurtleFormatterStyle().keySet()) {
			Method method = getBuilderMethod(optionName);
			callBuilderMethod(builder, method, state.getTurtleFormatterStyle().get(optionName));
		}
		return TurtleFormatFormattingStyleBuilderClass.getMethod("build").invoke(builder);
	}

	public String formatWithTurtleFormatter(String ttlContent)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
		Object style = newTurtleFormatterStyle();
		Object formatter = newTurtleFormatter(style);
		return (String) TurtleFormatFormatterClass.getMethod("applyToContent", String.class).invoke(formatter, ttlContent);
	}

	private Object newTurtleFormatter(Object style)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return TurtleFormatFormatterClass.getConstructor(TurtleFormatFormattingStyleClass)
				.newInstance(style);
	}

	private void callBuilderMethod(Object builder, Method method, String parameterValueAsString)
			throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		Class<?> param = method.getParameterTypes()[0];
		if (param.isEnum()) {
			List<?> selectedEnumValueList = Arrays.stream(param.getEnumConstants()).filter(e -> {
				try {
					return e.getClass().getMethod("name").invoke(e).equals(parameterValueAsString);
				} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
					throw new RuntimeException(ex);
				}
			}).collect(
					Collectors.toList());
			if (selectedEnumValueList.isEmpty()) {
				throw new IllegalArgumentException(
						"Cannot set config option %s to value %s: value must be one of %s".formatted(
								method.getName(),
								parameterValueAsString,
								Arrays.stream(param.getEnumConstants()).map(e -> {
									try {
										return (String) e.getClass().getMethod("name").invoke(e);
									} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
										throw new RuntimeException(ex);
									}
								}).collect(
										Collectors.joining(",", "[", "]"))));
			} else if (selectedEnumValueList.size() > 1) {
				throw new IllegalArgumentException(
						"Found more than 1 enum value for name %s, that should never happen".formatted(
								parameterValueAsString));
			}
			method.invoke(builder, selectedEnumValueList.get(0));
		} else if (param.equals(NumberFormat.class)) {
			method.invoke(builder, new DecimalFormat(parameterValueAsString, DecimalFormatSymbols.getInstance(Locale.US)));
		} else if (param.equals(Boolean.class) || param.equals(Boolean.TYPE)) {
			method.invoke(builder, Boolean.parseBoolean(parameterValueAsString));
		} else if (param.equals(String.class)) {
			method.invoke(builder, parameterValueAsString);
		} else if (param.equals(Integer.class)) {
			method.invoke(builder, Integer.parseInt(parameterValueAsString));
		} else if (param.equals(Double.class)) {
			method.invoke(builder, Double.parseDouble(parameterValueAsString));
		} else if (param.equals(Long.class)) {
			method.invoke(builder, Long.parseLong(parameterValueAsString));
		} else if (param.equals(Float.class)) {
			method.invoke(builder, Float.parseFloat(parameterValueAsString));
		} else if (Set.class.isAssignableFrom(param)) {
			method.invoke(builder, makeSetOf(((ParameterizedType) method.getGenericParameterTypes()[0]).getActualTypeArguments()[0], parameterValueAsString));
		} else if (List.class.isAssignableFrom(param)) {
			method.invoke(builder, makeListOf(((ParameterizedType) method.getGenericParameterTypes()[0]).getActualTypeArguments()[0], parameterValueAsString));
		} else {
			throw new IllegalArgumentException("Cannot handle turtle-formatter config option %s: parameters of type %s are not implemented in the spotless plugin yet".formatted(
					method.getName(), param.getName()));
		}
	}

	private Object makeListOf(Type type, String parameterValueAsString) {
		String[] entries = split(parameterValueAsString);
		return Arrays.stream(entries).map(e -> {
			try {
				return instantiate(type, e);
			} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
				throw new RuntimeException(ex);
			}
		}).collect(Collectors.toList());
	}

	private Object makeSetOf(Type type, String parameterValueAsString) {
		String[] entries = split(parameterValueAsString);
		return Arrays.stream(entries).map(e -> {
			try {
				return instantiate(type, e);
			} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
				throw new RuntimeException(ex);
			}
		}).collect(Collectors.toSet());
	}

	private Object instantiate(Type type, String stringRepresentation)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		if (type.equals(String.class)) {
			return stringRepresentation;
		}
		if (type.equals(JenaRDFNodeClass)) {
			try {
				String uri = tryToMakeUri(stringRepresentation);
				return this.JenaModelClass.getMethod("createResource", String.class)
						.invoke(this.jenaModelInstance, uri);
			} catch (IllegalArgumentException e) {
				return this.JenaModelClass.getMethod("createLiteral", String.class, String.class)
						.invoke(this.jenaModelInstance, stringRepresentation, "");
			}
		}
		if (type.equals(JenaResourceClass)) {
			return this.JenaModelClass.getMethod("createResource", String.class).invoke(this.jenaModelInstance, tryToMakeUri(stringRepresentation));
		}
		if (type.equals(JenaPropertyClass)) {
			String uri = tryToMakeUri(stringRepresentation);
			if (uri != null) {
				String localname = uri.replaceAll("^.+[#/]", "");
				String namespace = uri.substring(0, uri.length() - localname.length());
				return this.JenaModelClass.getMethod("createProperty", String.class, String.class)
						.invoke(this.jenaModelInstance, namespace, localname);
			}
		}
		if (type.equals(TurtleFormatKnownPrefix)) {
			return getKnownPrefix(stringRepresentation);
		}
		throw new IllegalArgumentException("Cannot instantiate class %s from string representation %s".formatted(type, stringRepresentation));
	}

	private String tryToMakeUri(String stringRepresentation)
			throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		if (stringRepresentation.matches("[^:/]+:[^:/]+")) {
			int colonIndex = stringRepresentation.indexOf(':');
			//could be a known prefix
			String prefix = stringRepresentation.substring(0, colonIndex);
			Object knownPrefix = getKnownPrefix(prefix);
			String base = this.TurtleFormatKnownPrefix.getMethod("iri").invoke(knownPrefix).toString();
			return base + stringRepresentation.substring(colonIndex + 1);
		}
		// try to parse a URI - throws an IllegalArgumentException if it is not a URI
		URI uri = URI.create(stringRepresentation);
		return uri.toString();
	}

	private Object getKnownPrefix(String stringRepresentation)
			throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		Field[] fields = TurtleFormatFormattingStyleClass.getDeclaredFields();
		List<String> options = new ArrayList<>();
		for (Field field : fields) {
			if (field.getType().equals(TurtleFormatKnownPrefix)) {
				Object knownPrefix = field.get(TurtleFormatFormattingStyleClass);
				String prefix = (String) TurtleFormatKnownPrefix.getMethod("prefix").invoke(knownPrefix);
				options.add(prefix);
				if (stringRepresentation.equals(prefix)) {
					return knownPrefix;
				}
			}
		}
		throw new IllegalArgumentException("Unable to find FormattingStyle.KnownPrefix for prefix '%s'. Options are: %s".formatted(stringRepresentation, options.stream().collect(
				Collectors.joining(",\n\t", "\n\t", "\n"))));
	}

	private static String[] split(String parameterValueAsString) {
		if (parameterValueAsString == null || parameterValueAsString.isBlank()) {
			return new String[0];
		}
		return parameterValueAsString.split("\\s*(,|,\\s*\n|\n)\\s*");
	}

	private Method getBuilderMethod(String optionName) {
		Method[] allMethods = TurtleFormatFormattingStyleBuilderClass.getDeclaredMethods();
		List<Method> methods = Arrays.stream(allMethods).filter(m -> m.getName().equals(optionName))
				.collect(
						Collectors.toList());
		if (methods.isEmpty()) {
			List<Method> candidates = Arrays.stream(allMethods).filter(m -> m.getParameterCount() == 1)
					.sorted(Comparator.comparing(Method::getName)).collect(
							Collectors.toList());
			throw new RuntimeException(
					"Unrecognized configuration parameter name: %s. Candidates are:%n%s".formatted(optionName, candidates.stream().map(Method::getName).collect(
							Collectors.joining("\n\t", "\t", ""))));
		}
		if (methods.size() > 1) {
			throw new RuntimeException(
					"More than one builder method found for configuration parameter name: %s".formatted(
							optionName));
		}
		Method method = methods.get(0);
		if (method.getParameterCount() != 1) {
			throw new RuntimeException(
					"Method with unexpected parameter count %s found for configuration parameter name: %s".formatted(
							method.getParameterCount(),
							optionName));
		}
		return method;
	}

	public Object parseToModel(String rawUnix, File file, Object lang)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Object model = getModel();
		Object errorHandler = getErrorHandler(file);
		Object parser = getParser(lang, errorHandler, rawUnix);
		parseModel(parser, model);
		return model;
	}

	public boolean areModelsIsomorphic(Object leftModel, Object rightModel)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Method isIsomorphicWith = JenaModelClass.getMethod("isIsomorphicWith", JenaModelClass);
		return (boolean) isIsomorphicWith.invoke(leftModel, rightModel);
	}

	public long modelSize(Object model) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		Method size = JenaModelClass.getMethod("size");
		return (long) size.invoke(model);
	}

}
