/*
 * Copyright 2024 DiffPlug
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
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReflectionHelper {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final RdfFormatterStep.State state;
	private final ClassLoader classLoader;
	private final Class<?> JenaRdfDataMgrClass;
	private final Class<?> JenaRdfParserClass;
	private final Class<?> JenaRdfParserBuilderClass;
	private final Class<?> JenaErrorHandlerClass;
	private final Class<?> JenaModelClass;
	private final Class<?> JenaStmtIteratorClass;
	private final Class<?> JenaStatementClass;
	private final Class<?> JenaRDFNodeClass;
	private final Class<?> JenaResourceClass;
	private final Class<?> JenaModelFactoryClass;
	private final Class<?> JenaLangClass;
	private final Class<?> JenaRDFFormatClass;
	private final Class<?> JenaGraphClass;
	private final Class<?> JenaNode;
	private final Class<?> JenaTriple;
	private final Class<?> TurtleFormatFormattingStyleClass;
	private final Class<?> TurtleFormatFormattingStyleBuilderClass;
	private final Class<?> TurtleFormatFormatterClass;

	private final Method graphFindByNodes;
	private final Method graphStream;
	private final Method graphFindTriple;
	private final Method contains;
	private final Method getSubject;
	private final Method getPredicate;
	private final Method getObject;
	private final Method isAnon;
	private final Method getGraph;
	private final Method tripleGetObject;

	public ReflectionHelper(RdfFormatterStep.State state) throws ClassNotFoundException, NoSuchMethodException {
		this.state = state;
		this.classLoader = state.getJarState().getClassLoader();
		this.JenaRdfDataMgrClass = classLoader.loadClass("org.apache.jena.riot.RDFDataMgr");
		this.JenaRdfParserClass = classLoader.loadClass("org.apache.jena.riot.RDFParser");
		this.JenaRdfParserBuilderClass = classLoader.loadClass("org.apache.jena.riot.RDFParserBuilder");
		this.JenaErrorHandlerClass = classLoader.loadClass("org.apache.jena.riot.system.ErrorHandler");
		this.JenaModelClass = classLoader.loadClass("org.apache.jena.rdf.model.Model");
		this.JenaStmtIteratorClass = classLoader.loadClass("org.apache.jena.rdf.model.StmtIterator");
		this.JenaRDFNodeClass = classLoader.loadClass("org.apache.jena.rdf.model.RDFNode");
		this.JenaResourceClass = classLoader.loadClass("org.apache.jena.rdf.model.Resource");
		this.JenaStatementClass = classLoader.loadClass("org.apache.jena.rdf.model.Statement");
		this.JenaModelFactoryClass = classLoader.loadClass("org.apache.jena.rdf.model.ModelFactory");
		this.JenaLangClass = classLoader.loadClass("org.apache.jena.riot.Lang");
		this.JenaRDFFormatClass = classLoader.loadClass("org.apache.jena.riot.RDFFormat");
		this.TurtleFormatFormatterClass = classLoader.loadClass("de.atextor.turtle.formatter.TurtleFormatter");
		this.TurtleFormatFormattingStyleClass = classLoader.loadClass("de.atextor.turtle.formatter.FormattingStyle");
		Class<?>[] innerClasses = TurtleFormatFormattingStyleClass.getDeclaredClasses();
		this.TurtleFormatFormattingStyleBuilderClass = Arrays.stream(innerClasses)
				.filter(c -> c.getSimpleName().equals("FormattingStyleBuilder")).findFirst().get();
		this.getSubject = JenaStatementClass.getMethod("getSubject");
		;
		this.getPredicate = JenaStatementClass.getMethod("getPredicate");
		this.getObject = JenaStatementClass.getMethod("getObject");
		this.isAnon = JenaRDFNodeClass.getMethod("isAnon");
		this.getGraph = JenaModelClass.getMethod(("getGraph"));
		this.JenaGraphClass = classLoader.loadClass("org.apache.jena.graph.Graph");
		this.JenaNode = classLoader.loadClass("org.apache.jena.graph.Node");
		this.JenaTriple = classLoader.loadClass("org.apache.jena.graph.Triple");
		this.graphFindByNodes = JenaGraphClass.getMethod("find", JenaNode, JenaNode, JenaNode);
		this.graphFindTriple = JenaGraphClass.getMethod("find", JenaTriple);
		this.graphStream = JenaGraphClass.getMethod("stream");
		this.tripleGetObject = JenaTriple.getMethod("getObject");
		this.contains = JenaGraphClass.getMethod("contains", JenaTriple);
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

	public Object listModelStatements(Object modelBefore)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Method listStatements = JenaModelClass.getMethod("listStatements");
		return listStatements.invoke(modelBefore);
	}

	public boolean hasNext(Object statementIterator)
			throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		Method hasNext = JenaStmtIteratorClass.getMethod("hasNext");
		return (boolean) hasNext.invoke(statementIterator);
	}

	public Object next(Object statementIterator)
			throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		Method hasNext = JenaStmtIteratorClass.getMethod("next");
		return hasNext.invoke(statementIterator);
	}

	public boolean containsBlankNode(Object statement)
			throws InvocationTargetException, IllegalAccessException {
		Object subject = getSubject.invoke(statement);
		if ((boolean) isAnon.invoke(subject)) {
			return true;
		}
		Object predicate = getPredicate.invoke(statement);
		if ((boolean) isAnon.invoke(predicate)) {
			return true;
		}
		Object object = getObject.invoke(statement);
		return (boolean) isAnon.invoke(object);
	}

	public boolean containsStatement(Object model, Object statement)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Method contains = JenaModelClass.getMethod("contains", JenaStatementClass);
		return (boolean) contains.invoke(model, statement);
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
			if (severity.equals("warning") && !state.getConfig().isFailOnWarning()) {
				logger.warn("{}({},{}): {}", this.filePath, line, col, message);
			} else {
				if (severity.equals("warning")) {
					logger.error("Formatter fails because of a parser warning. To make the formatter succeed in"
							+ "the presence of warnings, set the configuration parameter 'failOnWarning' to 'false' (default: 'true')");
				}
				throw new RuntimeException(
						String.format("line %d, col %d: %s (severity: %s)", line, col, message, severity));
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

	public String formatWithJena(Object model, Object rdfFormat)
			throws NoSuchMethodException, NoSuchFieldException, InvocationTargetException, IllegalAccessException {
		StringWriter sw = new StringWriter();
		JenaRdfDataMgrClass
				.getMethod("write", StringWriter.class, JenaModelClass, JenaRDFFormatClass)
				.invoke(JenaRdfDataMgrClass, sw, model, rdfFormat);
		return sw.toString();
	}

	public String formatWithTurtleFormatter(Object model)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
		Object style = turtleFormatterStyle();
		Object formatter = turtleFormatter(style);
		return (String) TurtleFormatFormatterClass.getMethod("apply", JenaModelClass).invoke(formatter, model);
	}

	private Object turtleFormatterStyle() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Object builder = TurtleFormatFormattingStyleClass.getMethod("builder").invoke(TurtleFormatFormatterClass);
		for (String optionName : state.getTurtleFormatterStyle().keySet()) {
			Method method = getBuilderMethod(optionName);
			callBuilderMethod(builder, method, state.getTurtleFormatterStyle().get(optionName));
		}
		Object style = TurtleFormatFormattingStyleBuilderClass.getMethod("build").invoke(builder);
		return style;
	}

	public String formatWithTurtleFormatter(String ttlContent)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
		Object style = turtleFormatterStyle();
		Object formatter = turtleFormatter(style);
		return (String) TurtleFormatFormatterClass.getMethod("applyToContent", String.class).invoke(formatter, ttlContent);
	}

	private Object turtleFormatter(Object style)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Object formatter = TurtleFormatFormatterClass.getConstructor(TurtleFormatFormattingStyleClass)
				.newInstance(style);
		return formatter;
	}

	private void callBuilderMethod(Object builder, Method method, String parameterValueAsString)
			throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		Class<?> param = method.getParameterTypes()[0];
		if (param.isEnum()) {
			List<?> selectedEnumValueList = Arrays.stream(param.getEnumConstants()).filter(e -> {
				try {
					return e.getClass().getMethod("name").invoke(e).equals(parameterValueAsString);
				} catch (IllegalAccessException ex) {
					throw new RuntimeException(ex);
				} catch (InvocationTargetException ex) {
					throw new RuntimeException(ex);
				} catch (NoSuchMethodException ex) {
					throw new RuntimeException(ex);
				}
			}).collect(
					Collectors.toList());
			if (selectedEnumValueList.isEmpty()) {
				throw new IllegalArgumentException(
						String.format("Cannot set config option %s to value %s: value must be one of %s",
								method.getName(),
								parameterValueAsString,
								Arrays.stream(param.getEnumConstants()).map(e -> {
									try {
										return (String) e.getClass().getMethod("name").invoke(e);
									} catch (IllegalAccessException ex) {
										throw new RuntimeException(ex);
									} catch (InvocationTargetException ex) {
										throw new RuntimeException(ex);
									} catch (NoSuchMethodException ex) {
										throw new RuntimeException(ex);
									}
								}).collect(
										Collectors.joining(",", "[", "]"))));
			} else if (selectedEnumValueList.size() > 1) {
				throw new IllegalArgumentException(
						String.format("Found more than 1 enum value for name %s, that should never happen",
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
		} else {
			throw new IllegalArgumentException(String.format(
					"Cannot handle turtle-formatter config option %s: parameters of type %s are not implemented in the spotless plugin yet",
					method.getName(), param.getName()));
		}
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
					String.format("Unrecognized configuration parameter name: %s. Candidates are:\n%s", optionName, candidates.stream().map(Method::getName).collect(
							Collectors.joining("\n\t", "\t", ""))));
		}
		if (methods.size() > 1) {
			throw new RuntimeException(
					String.format("More than one builder method found for configuration parameter name: %s",
							optionName));
		}
		Method method = methods.get(0);
		if (method.getParameterCount() != 1) {
			throw new RuntimeException(
					String.format("Method with unexpected parameter count %s found for configuration parameter name: %s",
							method.getParameterCount(),
							optionName));
		}
		return method;
	}

	public Object getRDFFormat(String rdfFormat) throws NoSuchFieldException, IllegalAccessException {
		return JenaRDFFormatClass.getDeclaredField(rdfFormat).get(JenaRDFFormatClass);
	}

	public Object sortedModel(Object model) {
		return Proxy.newProxyInstance(classLoader, new Class[]{JenaModelClass},
				new SortedModelInvocationHandler(this, model));
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

	private class SortedModelInvocationHandler implements InvocationHandler {
		private ReflectionHelper reflectionHelper;
		private Object jenaModel;

		public SortedModelInvocationHandler(ReflectionHelper reflectionHelper, Object jenaModel) {
			this.reflectionHelper = reflectionHelper;
			this.jenaModel = jenaModel;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("listSubjects") && method.getParameterCount() == 0) {
				Object resIterator = method.invoke(jenaModel);
				List resources = new ArrayList<>();
				while (hasNext(resIterator)) {
					resources.add(next(resIterator));
				}
				resources.sort(Comparator.comparing(x -> {
					try {
						return (String) x.getClass().getMethod("getURI").invoke(x);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					} catch (InvocationTargetException e) {
						throw new RuntimeException(e);
					} catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
				}).thenComparing(x -> {
					Object anonId = null;
					try {
						anonId = x.getClass().getMethod("getAnonId").invoke(x);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					} catch (InvocationTargetException e) {
						throw new RuntimeException(e);
					} catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
					if (anonId != null) {
						return anonId.toString();
					}
					return null;
				}));
				return reflectionHelper.classLoader.loadClass("org.apache.jena.rdf.model.impl.ResIteratorImpl")
						.getConstructor(
								Iterator.class, Object.class)
						.newInstance(resources.iterator(), null);
			}
			return method.invoke(jenaModel);
		}

		boolean hasNext(Object it) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
			return (boolean) it.getClass().getMethod("hasNext").invoke(it);
		}

		Object next(Object it) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
			return it.getClass().getMethod("next").invoke(it);
		}

	}
}
