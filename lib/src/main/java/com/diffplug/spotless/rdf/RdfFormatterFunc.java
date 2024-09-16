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

import com.diffplug.spotless.FormatExceptionPolicy;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.SerializedFunction;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RdfFormatterFunc implements FormatterFunc {

	private static final Set<String> TURTLE_EXTENSIONS = Set.of("ttl", "turtle");
	private static final Set<String> TRIG_EXTENSIONS = Set.of("trig");
	private static final Set<String> NTRIPLES_EXTENSIONS = Set.of("n-triples", "ntriples", "nt");
	private static final Set<String> NQUADS_EXTENSIONS = Set.of("n-quads", "nquads", "nq");

	private final RdfFormatterStep.State state;
	private final FormatExceptionPolicy exceptionPolicy = new FormatExceptionPolicyStrict();

	public RdfFormatterFunc(RdfFormatterStep.State state) {
		this.state = state;
	}

	@Override public String apply(String input) throws Exception {
		throw new UnsupportedOperationException("We need to know the filename so we can guess the RDF format. Use apply(String, File) instead!");
	}

	@Override public String apply(String rawUnix, File file) throws Exception {
		String filename = file.getName().toLowerCase();
		int lastDot = filename.lastIndexOf('.');
		if (lastDot < 0) {
			throw new IllegalArgumentException(
				String.format("File %s has no file extension, cannot determine RDF format", file.getAbsolutePath()));
		}
		if (lastDot + 1 >= filename.length()) {
			throw new IllegalArgumentException(
				String.format("File %s has no file extension, cannot determine RDF format", file.getAbsolutePath()));
		}
		String extension = filename.substring(lastDot + 1);
		ReflectionHelper reflectionHelper = new ReflectionHelper(state);
		try {
			if (TURTLE_EXTENSIONS.contains(extension)) {
				return formatTurtle(rawUnix, file, reflectionHelper);
			}
			if (TRIG_EXTENSIONS.contains(extension)) {
				return formatTrig(rawUnix, file);
			}
			if (NTRIPLES_EXTENSIONS.contains(extension)) {
				return formatNTriples(rawUnix, file);
			}
			if (NQUADS_EXTENSIONS.contains(extension)) {
				return formatNQuads(rawUnix, file);
			}
			throw new IllegalArgumentException(String.format("Cannot handle file with extension %s", extension));
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Error formatting file " + file.getPath(), e.getCause());
		} catch (Exception e) {
			throw new RuntimeException("Error formatting file " + file.getPath(), e);
		}
	}

	private String formatNQuads(String rawUnix, File file) {
		throw new UnsupportedOperationException("NQUADS formatting not supported yet");
	}

	private String formatNTriples(String rawUnix, File file) {
		throw new UnsupportedOperationException("NTRIPLES formatting not supported yet");
	}

	private String formatTrig(String rawUnix, File file) {
		throw new UnsupportedOperationException("TRIG formatting not supported yet");
	}

	private String formatTurtle(String rawUnix, File file, ReflectionHelper reflectionHelper)
		throws NoSuchMethodException, InvocationTargetException, IllegalAccessException,
		NoSuchFieldException, InstantiationException {
		String formatted;
		Object lang = reflectionHelper.getLang("TTL");
		if (state.getConfig().isUseTurtleFormatter()) {
			formatted = reflectionHelper.formatWithTurtleFormatter(rawUnix);
		} else {
			Object model = reflectionHelper.parseToModel(rawUnix, file, lang);
			formatted = reflectionHelper.formatWithJena(model, reflectionHelper.getRDFFormat("TURTLE_PRETTY"));
		}
		if (state.getConfig().isVerify()) {
			veryfyResult(rawUnix, file, reflectionHelper, lang, formatted);
		}
		return LineEnding.toUnix(formatted);
	}

	private static void veryfyResult(String rawUnix, File file, ReflectionHelper reflectionHelper, Object lang,
		String formatted) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Object modelBefore = reflectionHelper.parseToModel(rawUnix, file, lang);
		Object modelAfter = reflectionHelper.parseToModel(formatted, file, lang);
		if (!reflectionHelper.areModelsIsomorphic(modelBefore, modelAfter)){
			long beforeSize = reflectionHelper.modelSize(modelBefore);
			long afterSize = reflectionHelper.modelSize(modelAfter);
			String diffResult = "[no diff information available]";
			if (beforeSize != afterSize){
				diffResult=String.format("< %,d triples", beforeSize);
				diffResult += String.format("> %,d triples", afterSize);
			} else {
				List<Object> onlyInBeforeModel = new ArrayList<>();
				List<Object> onlyInAfterModel = new ArrayList<>();
				Object statementIterator = reflectionHelper.listModelStatements(modelBefore);
				while(reflectionHelper.hasNext(statementIterator)){
					Object statement = reflectionHelper.next(statementIterator);
					if (!reflectionHelper.containsBlankNode(statement)) {
						//don't compare statements with blank nodes. If the difference is there, that's just too bad
						if (!reflectionHelper.containsStatement(modelAfter, statement)){
							onlyInBeforeModel.add(statement);
						}
					}
				}
				statementIterator = reflectionHelper.listModelStatements(modelAfter);
				while(reflectionHelper.hasNext(statementIterator)){
					Object statement = reflectionHelper.next(statementIterator);
					if (!reflectionHelper.containsBlankNode(statement)) {
						//don't compare statements with blank nodes. If the difference is there, that's just too bad
						if (!reflectionHelper.containsStatement(modelBefore, statement)){
							onlyInAfterModel.add(statement);
						}
					}
				}
				if (! (onlyInBeforeModel.isEmpty() && onlyInAfterModel.isEmpty())) {
					diffResult = onlyInBeforeModel.stream().map(s -> String.format("< %s", s))
						.collect(Collectors.joining("\n"));
					diffResult += "\n" + onlyInAfterModel.stream().map(s -> String.format("> %s", s)).collect(Collectors.joining("\n"));
				} else {
					diffResult = "The only differences are in statements with blank nodes - not shown here";
				}
			}
			throw new IllegalStateException(
				"Formatted RDF is not isomorphic with original, which means that formatting changed the data.\n"
				+ "This could be a bug in the formatting system leading to data corruption and should be reported. \n"
				+ "If you are not scared to lose data, you can disable this check by setting the config option 'verify' to 'false'"
				+ "\n\nDiff:\n"
			+ diffResult);
		}
	}
}
