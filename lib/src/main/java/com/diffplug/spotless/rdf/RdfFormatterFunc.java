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
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.LineEnding;

public class RdfFormatterFunc implements FormatterFunc {
	private static final Set<String> TURTLE_EXTENSIONS = Set.of("ttl", "turtle");
	private static final Set<String> TRIG_EXTENSIONS = Set.of("trig");
	private static final Set<String> NTRIPLES_EXTENSIONS = Set.of("n-triples", "ntriples", "nt");
	private static final Set<String> NQUADS_EXTENSIONS = Set.of("n-quads", "nquads", "nq");

	private final RdfFormatterStep.State state;
	private final ReflectionHelper reflectionHelper;

	public RdfFormatterFunc(RdfFormatterStep.State state)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		this.state = state;
		this.reflectionHelper = new ReflectionHelper(state);
	}

	@Override
	public String apply(String input) throws Exception {
		throw new UnsupportedOperationException("We need to know the filename so we can guess the RDF format. Use apply(String, File) instead!");
	}

	@Override
	public String apply(String rawUnix, File file) throws Exception {
		String filename = file.getName().toLowerCase(Locale.US);
		int lastDot = filename.lastIndexOf('.');
		if (lastDot < 0) {
			throw new IllegalArgumentException(
					"File %s has no file extension, cannot determine RDF format".formatted(file.getAbsolutePath()));
		}
		if (lastDot + 1 >= filename.length()) {
			throw new IllegalArgumentException(
					"File %s has no file extension, cannot determine RDF format".formatted(file.getAbsolutePath()));
		}
		String extension = filename.substring(lastDot + 1);

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
			throw new IllegalArgumentException("Cannot handle file with extension %s".formatted(extension));
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
		formatted = reflectionHelper.formatWithTurtleFormatter(rawUnix);
		if (state.getConfig().isVerify()) {
			veryfyResult(rawUnix, file, reflectionHelper, lang, formatted);
		}
		return LineEnding.toUnix(formatted);
	}

	private static void veryfyResult(String rawUnix, File file, ReflectionHelper reflectionHelper, Object lang,
			String formatted) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Object modelBefore = reflectionHelper.parseToModel(rawUnix, file, lang);
		Object modelAfter = reflectionHelper.parseToModel(formatted, file, lang);
		if (!reflectionHelper.areModelsIsomorphic(modelBefore, modelAfter)) {
			long beforeSize = reflectionHelper.modelSize(modelBefore);
			long afterSize = reflectionHelper.modelSize(modelAfter);
			String diffResult;
			if (beforeSize != afterSize) {
				diffResult = "< %,d triples".formatted(beforeSize);
				diffResult += "> %,d triples".formatted(afterSize);
			} else {
				diffResult = calculateDiff(reflectionHelper, modelBefore, modelAfter);
			}
			throw new IllegalStateException(
					"Formatted RDF is not isomorphic with original, which means that formatting changed the data.\n"
							+ "This could be a bug in the formatting system leading to data corruption and should be reported. \n"
							+ "If you are not scared to lose data, you can disable this check by setting the config option 'verify' to 'false'"
							+ "\n\nDiff:\n"
							+ diffResult);
		}
	}

	private static String calculateDiff(ReflectionHelper reflectionHelper, Object modelBefore, Object modelAfter)
			throws InvocationTargetException, IllegalAccessException {
		String diffResult;
		Object graphBefore = reflectionHelper.getGraph(modelBefore);
		Object graphAfter = reflectionHelper.getGraph(modelAfter);

		List<Object> onlyInBeforeContent = reflectionHelper.streamGraph(graphBefore)
				.filter(triple -> {
					try {
						return !reflectionHelper.graphContainsSameTerm(graphAfter, triple);
					} catch (InvocationTargetException | IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				})
				.collect(Collectors.toList());

		List<Object> onlyInAfterContent = reflectionHelper.streamGraph(graphAfter)
				.filter(triple -> {
					try {
						return !reflectionHelper.graphContainsSameTerm(graphBefore, triple);
					} catch (InvocationTargetException | IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				})
				.collect(Collectors.toList());
		if (!(onlyInBeforeContent.isEmpty() && onlyInAfterContent.isEmpty())) {
			diffResult = onlyInBeforeContent.stream().map("< %s"::formatted)
					.collect(Collectors.joining("\n"));
			diffResult += "\n" + onlyInAfterContent.stream().map("> %s"::formatted).collect(Collectors.joining("\n"));
		} else {
			diffResult = "'before' and 'after' content differs, but we don't know why. This is probably a bug.";
		}
		return diffResult;
	}

}
